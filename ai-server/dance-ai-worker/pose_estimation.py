# >> pose_estimation.py
# >> 영상 프레임을 추출하고, 키포인트를 탐지하고, 데이터 정규화를 수행한다.
# >> 초기 추론 시 발생하는 지연을 방지하기 위해 더미 데이터를 통해 GPU Warm Up 작업을 수행한다.
# >> 영상 해상도에 의존적인 픽셀 좌표를 0 ~ 1 사이의 상대 좌표로 변환하여 해상도 불변성을 확보한다.
# >> YOLO의 기본 17개 키포인트 이외에 좌우 어깨 좌표의 평균값을 계산하여 Neck 좌표를 보간하는 커스텀 로직이다.
# >> [최종 수정] 배치 처리를 제거하고, ID가 변경되더라도 위치 기반으로 추적을 복구(Re-ID)하는 로직을 통합한다.
# >> [수정] 색상 히스토그램 로직 제거. 초기 타겟(Max Area) 선정 후 ID 기반 추적 및 거리 기반 Re-ID 적용.
# >> [긴급 수정] Predict 모드 시 임의 ID 할당 제거.
# >> [긴급 수정] YOLO ID 의존성 제거. 오직 "거리(Distance)" 5% 이내 법칙만 따름.
# >> [추가 수정] ID 우선 추적(Hybrid) 방식 적용 및 거리 임계값 20%로 상향 조정.
# >> [안정화] None ID 업데이트 방지 및 Fallback 방어 로직 적용.

import cv2
import json
import os
import torch
import numpy as np
from ultralytics import YOLO

# >> YOLO 모델 초기화 및 WarmUP
# >> model_path는 사용할 YOLO 모델 파일 경로다.
class PoseEstimator:
    def __init__(self, model_path='yolo11l-pose.pt'):
        print(f"\n[AI] YOLO 모델 로딩 중... ({model_path})")
        
        # GPU 사용 가능 여부 확인
        self.device = 'cuda' if torch.cuda.is_available() else 'cpu'
        print(f"[AI] 실행 디바이스: {self.device}")
        
        # 모델 로드
        try:
            self.model = YOLO(model_path)
            print("[AI] 모델 가중치 로드 완료!")
        except Exception as e:
            print(f"[Error] 모델 로드 실패: {e}")
            raise e

        # 워밍업 실행 (첫 실행 렉 방지)
        self.warmup()


    # >> 더미 데이터를 사용하여 모델을 WarmUp하고 VRAM 상태를 점검한다.
    def warmup(self):
        print("[AI] 모델 워밍업 시작 (Dummy Inference)...")
        try:
            # 1. 더미 이미지 생성 (YOLO 입력 크기인 640x640, 검은 화면)
            dummy_frame = np.zeros((640, 640, 3), dtype=np.uint8)
            
            # 2. 추론 실행 (결과는 버림)
            # track() 대신 predict()를 사용하여 트래커 상태를 초기화하지 않음 (KeyError 방지)
            self.model.predict(source=dummy_frame, device=self.device, verbose=False)
            
            print("[AI] 모델 워밍업 완료!")

            # 3. VRAM 상태 로그
            if self.device == 'cuda':
                reserved_bytes = torch.cuda.memory_reserved()
                print(f"[GPU Status] 예약된 VRAM: {reserved_bytes / 1024 / 1024:.2f} MB")
                
        except Exception as e:
            print(f"[Warning] 워밍업 중 오류 발생: {e}")

    # >> 영상을 프레임 단위로 분석하여 정규화된 JSON 데이터를 생성하고 저장한다.
    def process_video(self, video_path, output_dir):
        if not os.path.exists(video_path):
            raise FileNotFoundError(f"영상을 찾을 수 없습니다: {video_path}")

        video_name = os.path.splitext(os.path.basename(video_path))[0]
        # >> [변경] 생성된 사용자 JSON 파일 이름은 userID_songID_Artist_PartNumber_result.json으로 할거야.
        output_json_path = os.path.join(output_dir, f"{video_name}_result.json")
        
        # 영상 정보 읽기
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
            # "timeline_feedback": [],  <-- 제거됨
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

if __name__ == "__main__":
    estimator = PoseEstimator()
