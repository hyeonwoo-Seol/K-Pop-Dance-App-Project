import cv2
import json
import os
import torch
from ultralytics import YOLO

class PoseEstimator:
    def __init__(self, model_path='yolo11n-pose.pt'):
        """
        YOLO 모델 초기화
        :param model_path: 사용할 YOLO 모델 파일 경로 (없으면 자동 다운로드됨)
        """
        print(f"🔄 [AI] YOLO 모델 로딩 중... ({model_path})")
        # GPU 사용 가능 여부 확인
        self.device = 'cuda' if torch.cuda.is_available() else 'cpu'
        print(f"⚡ [AI] 실행 디바이스: {self.device}")
        
        self.model = YOLO(model_path)
        print("✅ [AI] 모델 로딩 완료!")

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
                "resolution": [width, height]
            },
            "frames": []
        }

        # YOLO 추론 (stream=True로 메모리 효율화)
        # RTX 5060 Ti의 경우 device=0 명시
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
    estimator = PoseEstimator()
    # 테스트 영상 경로를 직접 지정해서 테스트 가능
    # estimator.process_video("data/raw_videos/test.mp4", "data/results")
