# >> task.py
# >> I/O 바운드 작업(다운로드)와 CPU/GPU 바운드 작업(AI 추론)을 분리하여 정의하고, Worker 프로세스의 리소스를 관리한다.
# >> Worker 프로세스가 Fork 될 때, AI 모델을 메모리에 미리 로드한다.
# >> S3 클라이언트를 사용하여 영상 리소스를 로컬 스토리지로 가져오는 I/O 작업을 수행한다.
# >> 다운로드된 경로를 인자로 받아서, AI 추론 엔진을 호출하고 결과를 반환한다.
import os
import time
import boto3
from botocore.exceptions import ClientError
from celery.signals import worker_process_init
from celery_app import app
from config import Config
from pose_estimation import PoseEstimator

# AI 모델 전역 변수
pose_estimator = None


# >> Celery 워커 초기화 시그널 (생명 주기 연동)
# >> 워커 프로세스가 시작될 때(부모에서 분기된 직후) 실행된다.
@worker_process_init.connect
def init_worker(**kwargs):
    global pose_estimator
    print("\n [Worker] 워커 프로세스 초기화 감지! 모델 로드를 시작합니다...")
    
    try:
        if pose_estimator is None:
            # 여기서 모델을 로드하면, tasks.py가 실행되는 각 워커 프로세스마다
            # 독립적인 모델 인스턴스와 GPU 컨텍스트를 가지게 된다.
            # PoseEstimator 생성자 내부에서 warmup()이 자동 실행된다.
            pose_estimator = PoseEstimator(model_path='yolo11l-pose.pt')
            print("[Worker] 모델 로드 및 워밍업 완료, 작업을 기다립니다.\n")
    except Exception as e:
        print(f"[Worker] 모델 초기화 실패: {e}")


# >> Task 1: 영상 다운로드 (S3 <-> Local)
@app.task(
    name='tasks.download_video_task',
    bind=True,             
    max_retries=3,         
    default_retry_delay=5  
)

# >> S3에서 영상을 다운로드 하거나, 로컬 테스트 파일을 반환한다.
def download_video_task(self, bucket_name, video_key):
    print(f"\n[Task 1] 다운로드 요청 시작: {video_key}")
    
    file_name = os.path.basename(video_key)
    local_file_path = os.path.join(Config.DOWNLOAD_DIR, file_name)

    if Config.USE_AWS:
        # === [REAL] 실제 AWS S3 다운로드 ===
        s3_client = boto3.client(
            's3',
            aws_access_key_id=Config.AWS_ACCESS_KEY_ID,
            aws_secret_access_key=Config.AWS_SECRET_ACCESS_KEY,
            region_name=Config.AWS_REGION
        )
        try:
            print(f"cloud: S3({bucket_name})에서 다운로드 중...")
            s3_client.download_file(bucket_name, video_key, local_file_path)
            print(f"다운로드 완료: {local_file_path}")
            
        except ClientError as e:
            print(f"S3 다운로드 실패: {e}")
            raise self.retry(exc=e)
            
    else:
        # === [MOCK] 로컬 테스트 모드 ===
        print(f"[TEST MODE] AWS 연결 없이 로컬 파일을 사용합니다.")
        
        if not os.path.exists(local_file_path):
            error_msg = f"테스트용 파일이 없습니다! 여기에 넣어주세요: {local_file_path}"
            print(error_msg)
            raise FileNotFoundError(error_msg)
            
        print(f"로컬 테스트 파일 확인됨: {local_file_path}")

    return local_file_path



# >> Task 2: AI 분석 (YOLO v11)
@app.task(name='tasks.pose_estimation_task')
# >> 다운로드 된 영상을 받아서 YOLO를 사용해 분석을 수행한다.
def pose_estimation_task(video_path):
    global pose_estimator
    
    print(f"\n[Task 2] AI 분석 시작: {video_path}")

    # [안전장치] 만약 워커 초기화 시점에 모델 로드가 실패했거나, 
    # 워커가 아닌 방식으로 실행되었을 경우를 대비한 Fallback 로직
    if pose_estimator is None:
        print("[Warning] 모델이 미리 로드되지 않았습니다. 지금 로드합니다 (지연 발생 가능).")
        try:
            pose_estimator = PoseEstimator(model_path='yolo11l-pose.pt')
        except Exception as e:
            return {"status": "error", "error_message": f"Model load failed: {str(e)}"}

    try:
        # 분석 실행
        result_json_path = pose_estimator.process_video(video_path, Config.RESULT_DIR)
        
        print(f"모든 작업 완료, 결과 파일: {result_json_path}")
        
        return {
            "status": "success",
            "video_path": video_path,
            "result_path": result_json_path
        }

    except Exception as e:
        print(f"분석 중 치명적 오류: {e}")
        return {"status": "error", "error_message": str(e)}
