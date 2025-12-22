# >> SQS Bridge가 호출할 download_video_task 함수가 정의된 파일이다.

import os
import time
from celery_app import app
from pose_estimation import PoseEstimator

# >> 전역 변수로 모델을 한 번만 로드한다.
# >> 이렇게 해야 매 작업마다 모델을 다시 로드하는 시간을 아낄 수 있다.
pose_estimator = None


# >> [MOCK] AWS S3 다운로드를 흉내내는 함수이다.
# >> 실제로는 로컬 폴더에 있는 파일을 그대로 사용한다.
@app.task(name='tasks.download_video_task')
def download_video_task(bucket_name, video_key):
    print(f"[Mock] S3 다운로드 요청 받음: s3://{bucket_name}/{video_key}")
    
    # >> 가상의 다운로드 경로
    # >> 예: video_key가 "user1/dance.mp4"라면 -> "data/raw_videos/dance.mp4"로 매핑
    file_name = os.path.basename(video_key)
    base_dir = os.path.dirname(os.path.abspath(__file__))
    local_path = os.path.join(base_dir, 'data', 'raw_videos', file_name)

    # >> 폴더가 없으면 생성한다.
    os.makedirs(os.path.dirname(local_path), exist_ok=True)

    if os.path.exists(local_path):
        print(f"✅ [Mock] 로컬 파일 확인됨: {local_path}")
        return local_path
    else:
        # 파일이 없으면 에러 발생 (테스트를 위해 더미 파일을 만들어주세요)
        error_msg = f"❌ [Error] 테스트용 파일이 없습니다. 여기에 파일을 넣어주세요: {local_path}"
        print(error_msg)
        raise FileNotFoundError(error_msg)


# >> YOLO11을 사용하여 영상을 분석한다.
@app.task(name='tasks.pose_estimation_task')
def pose_estimation_task(video_path):
    global pose_estimator
    
    print(f"🧠 [Celery] 분석 작업 시작: {video_path}")

    # >> 모델이 로드되지 않았다면 로드한다. (Lazy Loading)
    if pose_estimator is None:
        print("🔧 [Celery] 워커 프로세스에 모델 초기화 중...")
        pose_estimator = PoseEstimator()

    try:
        # 결과 저장 폴더
        base_dir = os.path.dirname(os.path.abspath(__file__))
        output_dir = os.path.join(base_dir, 'data', 'analyzed_json')
        os.makedirs(output_dir, exist_ok=True)

        # >> 분석을 수행한다.
        result_json_path = pose_estimator.process_video(video_path, output_dir)
        
        return {
            "status": "success",
            "video_path": video_path,
            "result_json": result_json_path
        }

    except Exception as e:
        print(f"❌ [Celery] 분석 중 오류 발생: {e}")
        return {"status": "error", "message": str(e)}
