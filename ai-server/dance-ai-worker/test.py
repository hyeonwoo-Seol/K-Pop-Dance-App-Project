import cv2
import json
import os
import numpy as np
from ultralytics import YOLO
from pose_estimation import PoseEstimator
from scoring import Scoring

class TestPoseEstimator(PoseEstimator):
    def process_video(self, video_path, output_dir, is_user_video=False):
        """
        PoseEstimator의 process_video를 오버라이딩하여
        사용자 영상일 경우 수동으로 타겟을 선택하는 기능을 추가함.
        """
        if not os.path.exists(video_path):
            raise FileNotFoundError(f"영상을 찾을 수 없습니다: {video_path}")

        video_name = os.path.splitext(os.path.basename(video_path))[0]
        output_json_path = os.path.join(output_dir, f"{video_name}_result.json")
        
        cap = cv2.VideoCapture(video_path)
        fps = cap.get(cv2.CAP_PROP_FPS)
        total_frames = int(cap.get(cv2.CAP_PROP_FRAME_COUNT))
        width = int(cap.get(cv2.CAP_PROP_FRAME_WIDTH))
        height = int(cap.get(cv2.CAP_PROP_FRAME_HEIGHT))
        duration_sec = total_frames / fps if fps > 0 else 0
        
        print(f"[AI] 분석 시작: {video_name} (FPS: {fps}, Frames: {total_frames})")

        # 새 비디오 시작 시 트래커 상태 리셋 (안전장치)
        if hasattr(self.model, 'predictor') and self.model.predictor is not None:
            self.model.predictor.trackers = {} 

        # JSON 구조: 규격서(Contract) v1.1
        results_data = {
            "metadata": {
                "version": "1.1",
                "model": "yolo11l-pose",
                "video_width": width,
                "video_height": height,
                "total_frames": total_frames,
                "fps": float(f"{fps:.2f}"),
                "duration_sec": float(f"{duration_sec:.2f}")
            },
            "summary": {
                "total_score": 0,          
                "accuracy_grade": "Pending", 
                "part_accuracies": {},     # 부위별 정확도 딕셔너리
                "worst_points": []         # 가장 많이 틀린 관절 이름 리스트
            },
            "timeline_feedback": [],
            "frames": []
        }

        # >> [Tracking] 메인 유저 ID를 저장할 변수
        target_track_id = None
        
        # >> [Re-ID] 추적 복구를 위한 마지막 위치 저장 변수
        last_center_x = None
        last_center_y = None
        
        # 추적 시작 여부 플래그
        tracking_started = False 
        
        # 현재 처리 중인 프레임 인덱스
        processed_frame_count = 0
        
        # [핵심] 거리 임계값: 화면 너비의 20% (빠른 움직임 대응을 위해 상향)
        REID_DISTANCE_THRESHOLD = width * 0.20

        while True:
            ret, frame = cap.read()
            if not ret:
                break
            
            # [수정] YOLO 추론 (Tracking Mode) - 예외 처리 및 conf 상향
            try:
                results = self.model.track(
                    source=frame, 
                    persist=True, 
                    tracker="bytetrack.yaml", 
                    device=self.device, 
                    verbose=False,
                    conf=0.3  # 노이즈 감소를 위해 0.1 -> 0.3 상향
                )
            except KeyError:
                # 트래커 초기화 에러 발생 시 predict로 폴백
                if processed_frame_count % 30 == 0:
                     print(f"[Warning] Frame {processed_frame_count}: Tracker KeyError occurred. Fallback to predict.")
                results = self.model.predict(source=frame, verbose=False, conf=0.3)
            
            result = results[0]
            timestamp = processed_frame_count / fps if fps > 0 else 0
            
            frame_data = {
                "frame_index": processed_frame_count,
                "timestamp": float(f"{timestamp:.4f}"),
                "is_valid": False,
                "score": 0.0,
                "keypoints": [],
                "errors": [] # 프레임별 관절 에러 마킹용
            }

            boxes = result.boxes
            keypoints = result.keypoints
            
            if boxes is not None and keypoints is not None:
                track_ids = []
                if boxes.id is not None:
                    track_ids = boxes.id.int().cpu().tolist()
                else:
                    track_ids = [None] * len(boxes)
                
                boxes_xywh = boxes.xywh.cpu().numpy()
                
                # -----------------------------------------------------------------
                # [로직 1] 초기 타겟 선정 (추적이 아직 시작 안 됐을 때만!)
                # -----------------------------------------------------------------
                if not tracking_started and target_track_id is None:
                    
                    # === [Test.py 추가] 사용자 영상일 경우 ROI 수동 선택 ===
                    if is_user_video:
                        print("\n========================================================")
                        print("[Test] 사용자 영상입니다. 추적할 대상을 선택해야 합니다.")
                        print("1. 마우스로 추적할 사람을 드래그하여 박스를 그리세요.")
                        print("2. 선택 후 SPACE 또는 ENTER 키를 누르세요.")
                        print("========================================================")
                        
                        # ROI 선택창 띄우기
                        roi = cv2.selectROI("Select Target", frame, fromCenter=False, showCrosshair=True)
                        cv2.destroyWindow("Select Target")
                        
                        # roi: (x, y, w, h) -> Top-Left 기준
                        rx, ry, rw, rh = roi
                        # ROI의 중심 좌표 계산
                        rcx, rcy = rx + rw / 2, ry + rh / 2
                        
                        # ROI 중심과 가장 가까운 YOLO 감지 객체 찾기
                        min_roi_dist = float('inf')
                        best_match_id = -1
                        best_match_idx = -1
                        
                        for idx, t_id in enumerate(track_ids):
                            bx, by, bw, bh = boxes_xywh[idx] # YOLO는 xywh가 중심좌표임
                            # 거리 계산
                            dist = ((bx - rcx)**2 + (by - rcy)**2)**0.5
                            if dist < min_roi_dist:
                                min_roi_dist = dist
                                best_match_id = t_id
                                best_match_idx = idx
                        
                        if best_match_idx != -1:
                            target_track_id = best_match_id
                            last_center_x = boxes_xywh[best_match_idx][0]
                            last_center_y = boxes_xywh[best_match_idx][1]
                            tracking_started = True
                            print(f"[Test] 사용자가 선택한 타겟 매칭 성공: ID {target_track_id} (거리차: {min_roi_dist:.2f})")
                        else:
                            print("[Warning] 선택한 영역 근처에서 사람을 감지하지 못했습니다. 자동 모드(가장 큰 사람)로 전환합니다.")
                            is_user_video = False # 실패 시 자동 로직으로 Fallback

                    # === [기존 로직] 가장 큰 사람 자동 선택 (전문가 영상 or 선택 실패 시) ===
                    if not tracking_started:
                        max_area = 0
                        best_id = -1
                        best_idx = -1
                        
                        for idx, t_id in enumerate(track_ids):
                            w, h = boxes_xywh[idx][2], boxes_xywh[idx][3]
                            area = w * h
                            
                            # 가장 큰 사람을 찾는다 (첫 프레임 기준)
                            if area > max_area:
                                max_area = area
                                best_id = t_id
                                best_idx = idx
                        
                        if best_idx != -1:
                            target_track_id = best_id
                            last_center_x = boxes_xywh[best_idx][0]
                            last_center_y = boxes_xywh[best_idx][1]
                            tracking_started = True 
                            print(f"[AI] 초기 타겟 설정됨 (Frame {processed_frame_count}): ID {target_track_id}")

                # -----------------------------------------------------------------
                # [로직 2] 하이브리드 추적 (ID 우선 -> 거리 기반 Handover)
                # -----------------------------------------------------------------
                # 1순위: ID가 일치하는 사람이 있으면 무조건 그 사람을 믿음 (Tracker 신뢰)
                # 2순위: ID가 사라졌으면, 마지막 위치와 가장 가까운 사람을 찾아 ID를 갱신함
                
                best_match_idx = -1
                
                if tracking_started and last_center_x is not None:
                    # Case A: 기존 ID가 현재 프레임에 존재하는지 확인
                    if target_track_id in track_ids and target_track_id is not None:
                        # [추가] target_track_id가 None이 아닐 때만 인덱스 찾기 (안전장치)
                        best_match_idx = track_ids.index(target_track_id)
                    
                    # Case B: ID 소실 -> 거리 기반으로 새로운 타겟 탐색 (Re-ID)
                    else:
                        min_dist = float('inf')
                        nearest_idx = -1
                        nearest_id = None
                        
                        # 현재 프레임의 모든 객체와 거리 계산
                        for i, t_id in enumerate(track_ids):
                            cx, cy = boxes_xywh[i][0], boxes_xywh[i][1]
                            dist = ((cx - last_center_x)**2 + (cy - last_center_y)**2)**0.5
                            
                            if dist < min_dist:
                                min_dist = dist
                                nearest_idx = i
                                nearest_id = t_id
                        
                        # [핵심] 가장 가까운 객체가 허용 범위(20%) 이내인가?
                        if nearest_idx != -1 and min_dist < REID_DISTANCE_THRESHOLD:
                            best_match_idx = nearest_idx
                            
                            # [수정] None ID 업데이트 방지 (Fallback 방어)
                            # 찾아낸 객체의 ID가 None이 아닐 때만 내 타겟 ID를 갱신한다.
                            # ID가 None이라면(Predict 모드 등), target_track_id를 건드리지 않고 위치만 갱신한다.
                            if nearest_id is not None:
                                target_track_id = nearest_id # 타겟 ID 갱신 (Handover)
                                print(f"[AI] 타겟 재설정 (ID 변경): {target_track_id} (거리: {min_dist:.2f})")
                            else:
                                # 여기서는 ID 갱신 없이, best_match_idx를 통해 좌표만 따라간다.
                                pass
                        else:
                            # 20% 범위 내에 아무도 없음 -> 놓침(Lost)
                            pass

                # -----------------------------------------------------------------
                # [로직 3] 데이터 추출 및 저장
                # -----------------------------------------------------------------
                if best_match_idx != -1:
                    keypoints_raw = keypoints.data[best_match_idx].cpu().numpy()
                    
                    # [중요] 다음 프레임 추적을 위해 현재 위치 갱신
                    last_center_x = boxes_xywh[best_match_idx][0]
                    last_center_y = boxes_xywh[best_match_idx][1]
                    
                    normalized_keypoints = []
                    max_dim = max(width, height)
                    
                    for kp in keypoints_raw:
                        x, y, conf = kp
                        norm_x = x / max_dim if max_dim > 0 else 0
                        norm_y = y / max_dim if max_dim > 0 else 0
                        
                        normalized_keypoints.append([
                            float(f"{norm_x:.5f}"),
                            float(f"{norm_y:.5f}"),
                            float(f"{conf:.4f}")
                        ])
                    
                    l_sh, r_sh = normalized_keypoints[5], normalized_keypoints[6]
                    if l_sh[2] > 0 and r_sh[2] > 0:
                        nx, ny, nc = (l_sh[0] + r_sh[0]) / 2, (l_sh[1] + r_sh[1]) / 2, (l_sh[2] + r_sh[2]) / 2
                    else:
                        nx, ny, nc = 0.0, 0.0, 0.0
                        
                    normalized_keypoints.append([
                        float(f"{nx:.5f}"),
                        float(f"{ny:.5f}"),
                        float(f"{nc:.4f}")
                    ])
                    
                    frame_data["keypoints"] = normalized_keypoints
                    frame_data["is_valid"] = True

            results_data["frames"].append(frame_data)
            processed_frame_count += 1
            
            if processed_frame_count % 50 == 0:
                print(f"처리 중... {processed_frame_count}/{total_frames} frames")

        cap.release()
        
        if last_center_x is None:
             print("[Warning] 영상 전체에서 사람을 감지하지 못했습니다.")

        with open(output_json_path, 'w', encoding='utf-8') as f:
            json.dump(results_data, f, indent=None)
        
        print(f"[AI] 분석 완료 및 JSON 저장: {output_json_path}")
        return output_json_path

def create_overlay(video_path, json_path, output_path):
    """
    영상 위에 JSON의 Keypoint를 오버레이하여 저장하는 함수.
    정규화된 좌표를 역변환(Normalization Reversal)하여 그린다.
    """
    if not os.path.exists(video_path) or not os.path.exists(json_path):
        print(f"[Overlay] 파일 없음: {video_path} 또는 {json_path}")
        return

    print(f"[Overlay] 생성 시작: {output_path}")
    
    with open(json_path, 'r', encoding='utf-8') as f:
        data = json.load(f)
    
    cap = cv2.VideoCapture(video_path)
    fps = cap.get(cv2.CAP_PROP_FPS)
    width = int(cap.get(cv2.CAP_PROP_FRAME_WIDTH))
    height = int(cap.get(cv2.CAP_PROP_FRAME_HEIGHT))
    
    # 코덱 설정 (mp4v)
    fourcc = cv2.VideoWriter_fourcc(*'mp4v')
    out = cv2.VideoWriter(output_path, fourcc, fps, (width, height))
    
    frames_data = data["frames"]
    max_dim = max(width, height)
    
    # 스켈레톤 연결 정보 (COCO 형식 + Neck)
    # (p1, p2) 인덱스 쌍
    skeleton = [
        (0, 1), (0, 2), (1, 3), (2, 4),           # 얼굴
        (5, 7), (7, 9), (6, 8), (8, 10),          # 팔
        (5, 6), (5, 11), (6, 12), (11, 12),       # 몸통
        (11, 13), (13, 15), (12, 14), (14, 16),   # 다리
        (5, 17), (6, 17), (11, 17), (12, 17)      # 목 연결 (시각적 보완)
    ]
    
    frame_idx = 0
    while True:
        ret, frame = cap.read()
        if not ret:
            break
            
        # 해당 프레임의 데이터 찾기
        if frame_idx < len(frames_data):
            f_data = frames_data[frame_idx]
            
            if f_data["is_valid"]:
                kps = f_data["keypoints"]
                
                # 좌표 역변환 및 저장
                pixel_points = []
                for kp in kps:
                    nx, ny, conf = kp
                    px = int(nx * max_dim)
                    py = int(ny * max_dim)
                    pixel_points.append((px, py, conf))
                
                # 스켈레톤 그리기 (Line)
                for p1_i, p2_i in skeleton:
                    if p1_i < len(pixel_points) and p2_i < len(pixel_points):
                        pt1 = pixel_points[p1_i]
                        pt2 = pixel_points[p2_i]
                        
                        # 신뢰도가 0.3 이상일 때만 그림
                        if pt1[2] > 0.3 and pt2[2] > 0.3:
                            cv2.line(frame, (pt1[0], pt1[1]), (pt2[0], pt2[1]), (255, 255, 0), 2)
                
                # 관절 포인트 그리기 (Circle)
                for i, (px, py, conf) in enumerate(pixel_points):
                    if conf > 0.3:
                        color = (0, 0, 255) if i == 17 else (0, 255, 0) # 목은 빨간색
                        cv2.circle(frame, (px, py), 4, color, -1)

        out.write(frame)
        frame_idx += 1
        
        if frame_idx % 100 == 0:
            print(f"Overlay Frame {frame_idx} processed")
            
    cap.release()
    out.release()
    print("[Overlay] 생성 완료!")

if __name__ == "__main__":
    # 1. 디렉토리 설정
    base_dir = os.path.dirname(os.path.abspath(__file__))
    sample_mp4_dir = os.path.join(base_dir, "sampleMP4")
    sample_json_dir = os.path.join(base_dir, "sampleJSON")
    overlay_dir = os.path.join(base_dir, "overLay")
    
    # 폴더가 없으면 생성
    os.makedirs(sample_json_dir, exist_ok=True)
    os.makedirs(overlay_dir, exist_ok=True)
    
    # 2. 파일 경로 설정
    source_video_path = os.path.join(sample_mp4_dir, "Cut_AfterLike_source.mp4")
    user_video_path = os.path.join(sample_mp4_dir, "Cut_AfterLike_user.mp4")
    
    print(f"Source Video Path: {source_video_path}")
    print(f"User Video Path: {user_video_path}")
    
    # 3. Estimator 초기화
    estimator = TestPoseEstimator()
    
    # 4. 전문가 영상 처리 (자동 모드)
    print("\n[Step 1] 전문가 영상 처리 시작...")
    try:
        source_json_path = estimator.process_video(source_video_path, sample_json_dir, is_user_video=False)
    except Exception as e:
        print(f"전문가 영상 처리 중 오류: {e}")
        source_json_path = None

    # 5. 사용자 영상 처리 (수동 선택 모드)
    print("\n[Step 2] 사용자 영상 처리 시작...")
    try:
        user_json_path = estimator.process_video(user_video_path, sample_json_dir, is_user_video=True)
    except Exception as e:
        print(f"사용자 영상 처리 중 오류: {e}")
        user_json_path = None
    
    # 6. 점수 계산
    if source_json_path and user_json_path:
        print("\n[Step 3] 점수 계산 시작...")
        scorer = Scoring()
        # 주의: scoring.py의 compare 함수는 (user_path, expert_path) 순서임
        result = scorer.compare(user_json_path, source_json_path)
        
        if result:
            print("\n---------------------------------------------------")
            print(f"최종 점수: {result['total_score']}점")
            print("---------------------------------------------------")
    
    # 7. 오버레이 생성
    print("\n[Step 4] 오버레이 영상 생성 시작...")
    if source_json_path:
        create_overlay(source_video_path, source_json_path, os.path.join(overlay_dir, "source_overlay.mp4"))
    
    if user_json_path:
        create_overlay(user_video_path, user_json_path, os.path.join(overlay_dir, "user_overlay.mp4"))

    print("\n[Test] 모든 과정이 완료되었습니다.")
