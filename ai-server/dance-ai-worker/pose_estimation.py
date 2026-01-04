# >> pose_estimation.py
# >> 영상 프레임을 추출하고, 키포인트를 탐지하고, 데이터 정규화를 수행한다.
# >> 초기 추론 시 발생하는 지연을 방지하기 위해 더미 데이터를 통해 GPU Warm Up 작업을 수행한다.
# >> 영상 해상도에 의존적인 픽셀 좌표를 0 ~ 1 사이의 상대 좌표로 변환하여 해상도 불변성을 확보한다.
# >> YOLO의 기본 17개 키포인트 이외에 좌우 어깨 좌표의 평균값을 계산하여 Neck 좌표를 보간하는 커스텀 로직이다.
# >> [최종 수정] 배치 처리를 제거하고, ID가 변경되더라도 위치 기반으로 추적을 복구(Re-ID)하는 로직을 통합한다.

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
        # >> [수정] S3 저장 규격에 맞춰 summary 필드 초기화 수정 및 errors 추가
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
        
        # 현재 처리 중인 프레임 인덱스
        processed_frame_count = 0
        
        while True:
            ret, frame = cap.read()
            if not ret:
                break
            
            # YOLO 추론 (Tracking Mode)
            results = self.model.track(
                source=frame, 
                persist=True, 
                tracker="bytetrack.yaml", 
                device=self.device, 
                verbose=False
            )
            
            result = results[0]
            timestamp = processed_frame_count / fps if fps > 0 else 0
            
            frame_data = {
                "frame_index": processed_frame_count,
                "timestamp": float(f"{timestamp:.4f}"),
                "is_valid": False,
                "score": 0.0,
                "keypoints": [],
                "errors": [] # [수정] 프레임별 관절 에러 마킹 배열 초기화
            }

            boxes = result.boxes
            keypoints = result.keypoints
            
            if boxes is not None and boxes.id is not None and keypoints is not None:
                track_ids = boxes.id.int().cpu().tolist()
                boxes_xywh = boxes.xywh.cpu().numpy() # 중심x, 중심y, 너비, 높이
                
                # >> [Target Selection] 타겟 ID 결정 (아직 없으면 가장 큰 사람 선택)
                if target_track_id is None:
                    max_area = 0
                    best_id = -1
                    
                    for idx, t_id in enumerate(track_ids):
                        w, h = boxes_xywh[idx][2], boxes_xywh[idx][3]
                        area = w * h
                        
                        if area > max_area:
                            max_area = area
                            best_id = t_id
                    
                    if best_id != -1:
                        target_track_id = best_id
                        print(f"[AI] 메인 유저 감지됨 (Frame {processed_frame_count}): ID {target_track_id}")

                # >> [Target Maintenance] 타겟 ID가 바뀌었는지 확인하고 복구 (Re-ID)
                best_match_idx = -1
                
                # 1. ID로 먼저 찾기 (가장 정확함)
                if target_track_id in track_ids:
                    best_match_idx = track_ids.index(target_track_id)
                
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
                                target_track_id = t_id
                    
                    if best_match_idx != -1:
                        print(f"[ID Switch] ID 변경됨: ... -> {target_track_id} (Frame {processed_frame_count})")

                # >> [Filtering] 타겟 ID 데이터 추출
                if best_match_idx != -1:
                    # 해당 인덱스의 키포인트 추출
                    keypoints_raw = keypoints.data[best_match_idx].cpu().numpy()
                    
                    # 현재 위치 갱신 (다음 프레임을 위해)
                    last_center_x = boxes_xywh[best_match_idx][0]
                    last_center_y = boxes_xywh[best_match_idx][1]
                    
                    normalized_keypoints = []
                    max_dim = max(width, height)
                    
                    # 1. Keypoints 정규화
                    for kp in keypoints_raw:
                        x, y, conf = kp
                        norm_x = x / max_dim if max_dim > 0 else 0
                        norm_y = y / max_dim if max_dim > 0 else 0
                        
                        normalized_keypoints.append([
                            float(f"{norm_x:.5f}"),
                            float(f"{norm_y:.5f}"),
                            float(f"{conf:.4f}")
                        ])
                    
                    # 2. Neck 추가
                    l_sh, r_sh = normalized_keypoints[5], normalized_keypoints[6]
                    if l_sh[2] > 0 and r_sh[2] > 0:
                        nx, ny = (l_sh[0] + r_sh[0]) / 2, (l_sh[1] + r_sh[1]) / 2
                        nc = (l_sh[2] + r_sh[2]) / 2
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
        
        if target_track_id is None:
             print("[Warning] 영상 전체에서 사람을 감지하지 못했습니다.")

        with open(output_json_path, 'w', encoding='utf-8') as f:
            json.dump(results_data, f, indent=None)
        
        print(f"[AI] 분석 완료 및 JSON 저장: {output_json_path}")
        return output_json_path

if __name__ == "__main__":
    estimator = PoseEstimator()
