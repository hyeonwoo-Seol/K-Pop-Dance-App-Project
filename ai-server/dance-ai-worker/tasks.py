import os
import time
import boto3
from botocore.exceptions import ClientError
from celery_app import app
from config import Config
from pose_estimation import PoseEstimator

# AI 모델 전역 변수 (워커가 실행될 때 한 번만 로딩하기 위함)
pose_estimator = None

# ------------------------------------------------------------------------
# Task 1: 영상 다운로드 (S3 <-> Local)
# ------------------------------------------------------------------------
@app.task(
    name='tasks.download_video_task',
    bind=True,             # 재시도(Retry) 기능을 위해 bind=True 필수
    max_retries=3,         # 최대 3번 재시도
    default_retry_delay=5  # 실패 시 5초 뒤 재시도
)
def download_video_task(self, bucket_name, video_key):
    """
    S3에서 영상을 다운로드하거나, 로컬 테스트 파일을 반환합니다.
    """
    print(f"\n📥 [Task 1] 다운로드 요청 시작: {video_key}")
    
    # 저장될 로컬 파일 경로 생성
    file_name = os.path.basename(video_key)
    local_file_path = os.path.join(Config.DOWNLOAD_DIR, file_name)

    # [분기점] 실제 AWS 모드인가? 테스트 모드인가?
    if Config.USE_AWS:
        # === [REAL] 실제 AWS S3 다운로드 로직 ===
        s3_client = boto3.client(
            's3',
            aws_access_key_id=Config.AWS_ACCESS_KEY_ID,
            aws_secret_access_key=Config.AWS_SECRET_ACCESS_KEY,
            region_name=Config.AWS_REGION
        )
        try:
            print(f"   cloud: S3({bucket_name})에서 다운로드 중...")
            s3_client.download_file(bucket_name, video_key, local_file_path)
            print(f"   ✅ 다운로드 완료: {local_file_path}")
            
        except ClientError as e:
            print(f"   ❌ S3 다운로드 실패: {e}")
            # 네트워크 에러 등은 Celery가 알아서 재시도하게 함
            raise self.retry(exc=e)
            
    else:
        # === [MOCK] 로컬 테스트 모드 ===
        print(f"   🚧 [TEST MODE] AWS 연결 없이 로컬 파일을 사용합니다.")
        
        # 테스트를 위해 'data/raw_videos'에 해당 파일이 이미 있다고 가정
        if not os.path.exists(local_file_path):
            # 파일이 없으면 에러 (테스트를 위해 파일을 미리 넣어둬야 함)
            error_msg = f"❌ 테스트용 파일이 없습니다! 여기에 넣어주세요: {local_file_path}"
            print(error_msg)
            # 테스트 모드에서는 재시도하지 않고 바로 에러 발생
            raise FileNotFoundError(error_msg)
            
        print(f"   ✅ 로컬 테스트 파일 확인됨: {local_file_path}")

    # 다음 Task(분석)로 파일 경로를 넘겨줍니다.
    return local_file_path


# ------------------------------------------------------------------------
# Task 2: AI 분석 (YOLO v11)
# ------------------------------------------------------------------------
@app.task(name='tasks.pose_estimation_task')
def pose_estimation_task(video_path):
    """
    다운로드된 영상을 받아 YOLO 분석을 수행합니다.
    """
    global pose_estimator
    
    print(f"\n🧠 [Task 2] AI 분석 시작: {video_path}")

    # 모델이 메모리에 없으면 로드 (Cold Start 방지)
    if pose_estimator is None:
        print("   🔧 모델 초기화 중... (최초 1회 실행)")
        pose_estimator = PoseEstimator()

    try:
        # 분석 실행 (PoseEstimator 클래스 활용)
        # 결과는 config.py에 정의된 RESULT_DIR에 저장됨
        result_json_path = pose_estimator.process_video(video_path, Config.RESULT_DIR)
        
        print(f"   🎉 모든 작업 완료! 결과 파일: {result_json_path}")
        
        return {
            "status": "success",
            "video_path": video_path,
            "result_path": result_json_path
        }

    except Exception as e:
        print(f"   ❌ 분석 중 치명적 오류: {e}")
        return {"status": "error", "error_message": str(e)}