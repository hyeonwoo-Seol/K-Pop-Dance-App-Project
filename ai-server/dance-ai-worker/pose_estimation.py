import cv2
import json
import os
import torch
import numpy as np
from ultralytics import YOLO

class PoseEstimator:
    def __init__(self, model_path='yolo11l-pose.pt'):
        """
        YOLO 모델 초기화 및 워밍업
        :param model_path: 사용할 YOLO 모델 파일 경로 (기본값: Large 모델)
        """
        print(f"\n🔄 [AI] YOLO 모델 로딩 중... ({model_path})")
        
        # GPU 사용 가능 여부 확인
        self.device = 'cuda' if torch.cuda.is_available() else 'cpu'
        print(f"⚡ [AI] 실행 디바이스: {self.device}")
        
        # 모델 로드
        try:
            self.model = YOLO(model_path)
            print("✅ [AI] 모델 가중치 로드 완료!")
        except Exception as e:
            print(f"❌ [Error] 모델 로드 실패: {e}")
            raise e

        # 워밍업 실행 (첫 실행 렉 방지)
        self.warmup()

    def warmup(self):
        """
        더미 데이터를 사용하여 모델을 예열(Warm-up)하고 VRAM 상태를 점검합니다.
        """
        print("🔥 [AI] 모델 워밍업 시작 (Dummy Inference)...")
        try:
            # 1. 더미 이미지 생성 (YOLO 입력 크기인 640x640, 검은 화면)
            dummy_frame = np.zeros((640, 640, 3), dtype=np.uint8)
            
            # 2. 추론 실행 (결과는 버림)
            # verbose=False로 불필요한 로그 출력 방지
            self.model.predict(source=dummy_frame, device=self.device, verbose=False)
            
            print("🔥 [AI] 모델 워밍업 완료! (Ready to serve)")

            # 3. VRAM 점유율 확인 (운영 용량 산정용)
            if self.device == 'cuda':
                # 현재 할당된 메모리 (Byte -> MB 변환)
                allocated_bytes = torch.cuda.memory_allocated()
                allocated_mb = allocated_bytes / 1024 / 1024
                
                # 최대 예약된 메모리 (캐시 포함)
                reserved_bytes = torch.cuda.memory_reserved()
                reserved_mb = reserved_bytes / 1024 / 1024

                print(f"📊 [GPU Status] 현재 모델 VRAM 점유: {allocated_mb:.2f} MB")
                print(f"📊 [GPU Status] 전체 예약된 VRAM(캐시포함): {reserved_mb:.2f} MB")
                
                # 16GB(약 16384MB) 기준 사용률 계산
                usage_percent = (reserved_mb / 16384) * 100
                print(f"   (참고: RTX 5060 Ti 16GB 기준 약 {usage_percent:.2f}% 사용 중)")
                
        except Exception as e:
            print(f"⚠️ [Warning] 워밍업 중 오류 발생 (무시 가능): {e}")

    def process_video(self, video_path, output_dir):
        """
        영상을 프레임 단위로 분석하여 스켈레톤 데이터를 JSON으로 저장
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
        
        print(f"🎬 [AI] 분석 시작: {video_name} (FPS: {fps}, Frames: {total_frames})")

        results_data = {
            "meta": {
                "video_name": video_name,
                "fps": fps,
                "total_frames": total_frames,
                "resolution": [width, height],
                "model": "yolo11l-pose"
            },
            "frames": []
        }

        # YOLO 추론 (stream=True로 메모리 효율화)
        results = self.model.predict(source=video_path, stream=True, device=self.device, verbose=False)

        for i, result in enumerate(results):
            frame_data = {
                "frame_id": i,
                "keypoints": []
            }

            # 사람이 감지된 경우
            if result.keypoints is not None:
                # 첫 번째 사람(가장 신뢰도 높은 사람)만 추출
                # data[0]은 (N, 3) 형태: [x, y, confidence]
                if len(result.keypoints.data) > 0:
                    keypoints = result.keypoints.data[0].cpu().numpy().tolist()
                    frame_data["keypoints"] = keypoints
            
            results_data["frames"].append(frame_data)
            
            # 진행 상황 로깅 (100 프레임마다)
            if i % 100 == 0:
                print(f"   ⏳ 처리 중... {i}/{total_frames} frames")

        cap.release()

        # JSON 저장
        with open(output_json_path, 'w') as f:
            json.dump(results_data, f)
        
        print(f"✅ [AI] 분석 완료! 결과 저장됨: {output_json_path}")
        return output_json_path

# 테스트용 (이 파일만 직접 실행했을 때)
if __name__ == "__main__":
    # 클래스 생성 시 자동으로 워밍업이 수행됩니다.
    estimator = PoseEstimator()
    
    # VRAM 로그를 확인한 후, 실제 영상 테스트를 원하시면 아래 주석을 해제하세요.
    # estimator.process_video("data/raw_videos/test.mp4", "data/results")
