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
            self.model.predict(source=dummy_frame, device=self.device, verbose=False)
            
            print("[AI] 모델 워밍업 완료!")

            # 3. VRAM 상태 로그
            if self.device == 'cuda':
                reserved_bytes = torch.cuda.memory_reserved()
                print(f"[GPU Status] 예약된 VRAM: {reserved_bytes / 1024 / 1024:.2f} MB")
                
        except Exception as e:
            print(f"[Warning] 워밍업 중 오류 발생: {e}")

    def process_video(self, video_path, output_dir):
        """
        영상을 프레임 단위로 분석하여 정규화된 JSON 데이터를 생성 및 저장
        """
        if not os.path.exists(video_path):
            raise FileNotFoundError(f"영상을 찾을 수 없습니다: {video_path}")

        video_name = os.path.splitext(os.path.basename(video_path))[0]
        output_json_path = os.path.join(output_dir, f"{video_name}_analysis.json")
        
        # 영상 정보 읽기
        cap = cv2.VideoCapture(video_path)
        fps = cap.get(cv2.CAP_PROP_FPS)
        total_frames = int(cap.get(cv2.CAP_PROP_FRAME_COUNT))
        width = int(cap.get(cv2.CAP_PROP_FRAME_WIDTH))
        height = int(cap.get(cv2.CAP_PROP_FRAME_HEIGHT))
        duration_sec = total_frames / fps if fps > 0 else 0
        
        print(f"[AI] 분석 시작: {video_name} (FPS: {fps}, Frames: {total_frames})")


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
                "worst_part": "None",
                "best_part": "None",
                "practice_time_sec": float(f"{duration_sec:.2f}")
            },
            "timeline_feedback": [],
            "frames": []
        }

        # YOLO 추론 (stream=True로 메모리 효율화)
        results = self.model.predict(source=video_path, stream=True, device=self.device, verbose=False)

        for i, result in enumerate(results):
            # 현재 프레임의 시간 (초)
            timestamp = i / fps if fps > 0 else 0
            
            frame_data = {
                "frame_index": i,
                "timestamp": float(f"{timestamp:.4f}"),
                "is_valid": False,
                "score": 0.0,
                "keypoints": []
            }

            # 사람이 감지된 경우
            if result.keypoints is not None and len(result.keypoints.data) > 0:
                # data[0]은 (17, 3) 형태: [[x, y, conf], ...]
                keypoints_raw = result.keypoints.data[0].cpu().numpy()
                
                normalized_keypoints = []
                
                # 1. 기존 17개 키포인트 정규화
                for kp in keypoints_raw:
                    x, y, conf = kp
                    
                    # >> 좌표 정규화 (0.0 ~ 1.0)
                    norm_x = x / width if width > 0 else 0
                    norm_y = y / height if height > 0 else 0
                    
                    # 소수점 5자리까지만 저장 (용량 최적화)
                    normalized_keypoints.append([
                        float(f"{norm_x:.5f}"),
                        float(f"{norm_y:.5f}"),
                        float(f"{conf:.4f}")
                    ])
                
                # 2. Neck (Index 17) 추가
                # >> 5번(Left Shoulder)과 6번(Right Shoulder)의 중간값을 계산한다.
                left_shoulder = normalized_keypoints[5]
                right_shoulder = normalized_keypoints[6]
                
                # 두 어깨가 모두 감지되었을 때(conf > 0)만 계산
                if left_shoulder[2] > 0 and right_shoulder[2] > 0:
                    neck_x = (left_shoulder[0] + right_shoulder[0]) / 2
                    neck_y = (left_shoulder[1] + right_shoulder[1]) / 2
                    neck_conf = (left_shoulder[2] + right_shoulder[2]) / 2
                else:
                    neck_x, neck_y, neck_conf = 0.0, 0.0, 0.0

                normalized_keypoints.append([
                    float(f"{neck_x:.5f}"),
                    float(f"{neck_y:.5f}"),
                    float(f"{neck_conf:.4f}")
                ])
                
                frame_data["keypoints"] = normalized_keypoints
                frame_data["is_valid"] = True
            
            results_data["frames"].append(frame_data)
            
            # 진행 상황 로깅
            if i % 100 == 0:
                print(f"처리 중... {i}/{total_frames} frames")

        cap.release()

        # JSON 저장
        with open(output_json_path, 'w', encoding='utf-8') as f:
            json.dump(results_data, f, indent=None)
        
        print(f"[AI] 분석 완료 및 JSON 저장: {output_json_path}")
        return output_json_path

if __name__ == "__main__":
    estimator = PoseEstimator()
    # estimator.process_video("data/raw_videos/test.mp4", "data/analyzed_json")
