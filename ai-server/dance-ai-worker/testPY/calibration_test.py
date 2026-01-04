# >> calibration_test.py
# >> 전문가 영상 2개를 비교하여 알고리즘의 '허용 오차(Baseline Error)'를 측정하는 스크립트입니다.
# >> Source 영상은 자동으로 분석하고, User 영상은 [수동 선택 모드]를 통해 특정 인물을 추적합니다.
# >> [수정] ID Switching 방지: ID가 바뀌더라도 직전 위치(Last Position)와 가장 가까운 사람을 찾아 추적을 이어가는 로직 추가.

import os
import json
import cv2
import numpy as np
import shutil
from pose_estimation import PoseEstimator
from scoring import Scoring

# >> [확장] 수동 선택 기능을 갖춘 추적기
class ManualPoseEstimator(PoseEstimator):
    
    # >> 1단계: 사람이 나올 때까지 프레임을 넘기며 추적할 ID를 선택받는 함수
    def select_target_id(self, video_path):
        if not os.path.exists(video_path):
            print(f"[Error] 영상을 찾을 수 없습니다: {video_path}")
            return None

        cap = cv2.VideoCapture(video_path)
        
        print("\n[ID 선택] 사람을 찾기 위해 초반 영상을 검색 중입니다...")
        
        found_valid_frame = False
        display_frame = None
        detected_ids = []
        
        # 최대 150프레임(약 5초)까지 탐색
        max_search_frames = 150
        frame_idx = 0

        while frame_idx < max_search_frames:
            ret, frame = cap.read()
            if not ret: break

            # YOLO 추론 (단일 프레임)
            results = self.model.track(source=frame, persist=True, verbose=False, device=self.device)
            result = results[0]

            if result.boxes and result.boxes.id is not None:
                track_ids = result.boxes.id.int().cpu().tolist()
                boxes = result.boxes.xyxy.cpu().numpy()
                detected_ids = track_ids
                
                display_frame = frame.copy()
                
                # 박스와 ID 그리기
                for idx, track_id in enumerate(track_ids):
                    x1, y1, x2, y2 = map(int, boxes[idx])
                    cv2.rectangle(display_frame, (x1, y1), (x2, y2), (0, 255, 0), 2)
                    
                    label = f"ID: {track_id}"
                    (w, h), _ = cv2.getTextSize(label, cv2.FONT_HERSHEY_SIMPLEX, 0.8, 2)
                    cv2.rectangle(display_frame, (x1, y1 - 30), (x1 + w, y1), (0, 255, 0), -1)
                    cv2.putText(display_frame, label, (x1, y1 - 5), cv2.FONT_HERSHEY_SIMPLEX, 0.8, (0, 0, 0), 2)
                
                found_valid_frame = True
                print(f" >> {frame_idx}번째 프레임에서 사람 감지 성공!")
                break
            
            frame_idx += 1
        
        cap.release()

        if not found_valid_frame:
            print(f"[Warning] 초반 {max_search_frames}프레임 동안 사람을 감지하지 못했습니다.")
            return None

        preview_path = "id_selection_preview.jpg"
        cv2.imwrite(preview_path, display_frame)
        print(f"[안내] 선택 화면이 팝업됩니다. (안 보이면 '{preview_path}' 파일을 확인하세요)")

        try:
            cv2.imshow("Select Target Person", display_frame)
            cv2.waitKey(100) 
        except Exception:
            pass 

        print(f"\n감지된 ID 목록: {detected_ids}")
        while True:
            try:
                selection = input(">> 추적할 사람의 ID를 입력하세요: ")
                selected_id = int(selection)
                if selected_id in detected_ids:
                    cv2.destroyAllWindows()
                    return selected_id
                else:
                    print(f"오류: ID {selected_id}는 감지된 목록에 없습니다. 다시 입력해주세요.")
            except ValueError:
                print("오류: 숫자를 입력해주세요.")

    # >> 2단계: [핵심 수정] ID 기반 추적 + 위치 기반 보정(Re-Identification)
    def process_video_specific_id(self, video_path, output_dir, initial_target_id):
        video_name = os.path.splitext(os.path.basename(video_path))[0]
        output_json_path = os.path.join(output_dir, f"{video_name}_ID_{initial_target_id}.json")
        
        cap = cv2.VideoCapture(video_path)
        fps = cap.get(cv2.CAP_PROP_FPS)
        total_frames = int(cap.get(cv2.CAP_PROP_FRAME_COUNT))
        width = int(cap.get(cv2.CAP_PROP_FRAME_WIDTH))
        height = int(cap.get(cv2.CAP_PROP_FRAME_HEIGHT))
        duration_sec = total_frames / fps if fps > 0 else 0
        
        print(f"[Test AI] 추적 분석 시작: {video_name} (Initial ID: {initial_target_id})")

        json_data = {
            "metadata": {
                "version": "1.2_manual_tracking",
                "model": "yolo11l-pose-manual",
                "video_width": width, "video_height": height,
                "total_frames": total_frames, "fps": fps, "duration_sec": duration_sec
            },
            "summary": {"total_score": 0}, 
            "frames": []
        }
        
        # >> 추적 상태 변수
        current_target_id = initial_target_id
        last_center_x = None
        last_center_y = None
        
        frame_idx = 0
        valid_frames_count = 0
        
        while True:
            ret, frame = cap.read()
            if not ret: break
            
            # YOLO 추론
            results = self.model.track(source=frame, persist=True, verbose=False, device=self.device)
            result = results[0]
            
            frame_info = {
                "frame_index": frame_idx,
                "timestamp": float(f"{frame_idx/fps:.4f}") if fps > 0 else 0,
                "is_valid": False,
                "keypoints": []
            }
            
            best_match_idx = -1
            found_by_id = False
            
            if result.boxes and result.boxes.id is not None:
                track_ids = result.boxes.id.int().cpu().tolist()
                boxes_xywh = result.boxes.xywh.cpu().numpy() # x, y, w, h
                
                # 1. ID로 먼저 찾기 (가장 정확함)
                if current_target_id in track_ids:
                    best_match_idx = track_ids.index(current_target_id)
                    found_by_id = True
                
                # 2. ID로 못 찾았지만, 직전 위치가 있다면 위치 기반 검색 (ID Switching 대응)
                elif last_center_x is not None:
                    min_dist = float('inf')
                    
                    for i, t_id in enumerate(track_ids):
                        cx, cy = boxes_xywh[i][0], boxes_xywh[i][1]
                        dist = ((cx - last_center_x)**2 + (cy - last_center_y)**2)**0.5
                        
                        # 화면 너비의 15% 이내 거리여야 같은 사람으로 간주 (급격한 이동 제외)
                        if dist < (width * 0.15):
                            if dist < min_dist:
                                min_dist = dist
                                best_match_idx = i
                                # 여기서 ID가 바뀌었다면 업데이트 (중요!)
                                current_target_id = t_id 
                    
                    if best_match_idx != -1:
                        print(f"   [ID Switch] ID 변경됨: {initial_target_id} -> {current_target_id} (Frame {frame_idx})")

                # >> 데이터 추출 및 저장
                if best_match_idx != -1:
                    keypoints_raw = result.keypoints.data[best_match_idx].cpu().numpy()
                    
                    # 현재 위치 갱신 (다음 프레임을 위해)
                    last_center_x = boxes_xywh[best_match_idx][0]
                    last_center_y = boxes_xywh[best_match_idx][1]
                    
                    # 정규화 로직 (max_dim 기준)
                    normalized_kp = []
                    max_dim = max(width, height)
                    
                    for kp in keypoints_raw:
                        x, y, conf = kp
                        norm_x = x / max_dim if max_dim > 0 else 0
                        norm_y = y / max_dim if max_dim > 0 else 0
                        normalized_kp.append([float(f"{norm_x:.5f}"), float(f"{norm_y:.5f}"), float(f"{conf:.4f}")])
                    
                    # Neck 추가
                    l_sh, r_sh = normalized_kp[5], normalized_kp[6]
                    if l_sh[2] > 0 and r_sh[2] > 0:
                        nx, ny = (l_sh[0]+r_sh[0])/2, (l_sh[1]+r_sh[1])/2
                        nc = (l_sh[2]+r_sh[2])/2
                    else:
                        nx, ny, nc = 0, 0, 0
                    normalized_kp.append([float(f"{nx:.5f}"), float(f"{ny:.5f}"), float(f"{nc:.4f}")])
                    
                    frame_info["is_valid"] = True
                    frame_info["keypoints"] = normalized_kp
                    valid_frames_count += 1
            
            # 만약 사람을 아예 놓쳤다면? last_center 유지 (잠시 가려졌을 수도 있으므로)
            # 단, 너무 오래 놓치면(예: 30프레임) last_center 초기화 고려 가능 (여기선 유지)

            json_data["frames"].append(frame_info)
            
            if frame_idx % 100 == 0:
                status = f"ID {current_target_id}" if frame_info["is_valid"] else "Lost"
                print(f"   >> Processing frame {frame_idx}/{total_frames}... ({status})")
            frame_idx += 1
            
        cap.release()
        
        with open(output_json_path, 'w', encoding='utf-8') as f:
            json.dump(json_data, f, indent=None)
            
        print(f"[Test AI] 분석 완료: {output_json_path} (Valid Frames: {valid_frames_count}/{total_frames})")
        return output_json_path

# >> 등급 계산 (기존 유지)
def calculate_grade(score, visibility_ratio=1.0):
    if score >= 90: grade = "S"
    elif score >= 80: grade = "A"
    elif score >= 70: grade = "B"
    else: grade = "C"
    if visibility_ratio < 0.7:
        grades_order = ["S", "A", "B", "C"]
        try:
            current_idx = grades_order.index(grade)
            new_idx = min(current_idx + 1, len(grades_order) - 1)
            grade = grades_order[new_idx]
        except ValueError: pass
    return grade

def run_calibration_test():
    base_dir = os.path.dirname(os.path.abspath(__file__))
    sample_dir = os.path.join(base_dir, 'sampleMP4')
    output_dir = os.path.join(base_dir, 'calibration_result')
    
    source_video_path = os.path.join(sample_dir, 'Cut_AfterLike_source.mp4')
    user_video_path = os.path.join(sample_dir, 'Cut_AfterLike_user.mp4')
    
    if os.path.exists(output_dir): shutil.rmtree(output_dir)
    os.makedirs(output_dir)

    print("==================================================")
    print("   [ 졸업작품 ] 전문가 vs 전문가 오차 측정 (ID Re-Id 기능 포함)")
    print("==================================================")

    try:
        manual_estimator = ManualPoseEstimator(model_path='yolo11l-pose.pt')
        scorer = Scoring()
    except Exception as e:
        print(f"[Error] 모델 초기화 실패: {e}")
        return

    # 1. Source 분석 (자동)
    print("\n[1단계] Source(기준) 영상 분석")
    source_json_path = manual_estimator.process_video(source_video_path, output_dir)
    final_source_path = os.path.join(output_dir, "source_expert.json")
    if os.path.exists(source_json_path):
        os.rename(source_json_path, final_source_path)

    # 2. User 분석 (수동 선택 + 자동 추적 보정)
    print("\n[2단계] User(비교) 영상 분석")
    selected_id = manual_estimator.select_target_id(user_video_path)
    
    if selected_id is not None:
        user_json_path = manual_estimator.process_video_specific_id(user_video_path, output_dir, selected_id)
        
        # 3. 비교
        print("\n[3단계] 최종 점수 산출")
        try:
            result = scorer.compare(user_json_path, final_source_path)
            if result:
                grade = calculate_grade(result['total_score'], result.get('visibility_ratio', 1.0))
                print("\n==================================================")
                print("   [ 🏆 테스트 결과 ]")
                print("==================================================")
                print(f"1. 종합 점수 (Total Score): {result['total_score']}점")
                print(f"2. 등급 (Grade): {grade}") 
                print(f"3. 취약 부위: {result['worst_part']}")
                print(f"4. 유효 프레임 비율: {result.get('visibility_ratio', 0.0):.2f}")
                
                print(f"\n[결론]")
                if result['total_score'] >= 85:
                    print(f"✅ 성공! 추적과 싱크, 점수 산출이 모두 정상입니다.")
                else:
                    print(f"⚠️ 점수가 {result['total_score']}점입니다. 오버레이 영상을 다시 확인해보세요.")
        except Exception as e:
            print(f"비교 실패: {e}")
    else:
        print("[취소] ID 선택 실패")

if __name__ == "__main__":
    run_calibration_test()
