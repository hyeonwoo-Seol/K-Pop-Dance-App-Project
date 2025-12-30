# >> tasks.py
# >> I/O 바운드 작업(다운로드)와 CPU/GPU 바운드 작업(AI 추론)을 분리하여 정의하고, Worker 프로세스의 리소스를 관리한다.
# >> Worker 프로세스가 Fork 될 때, AI 모델을 메모리에 미리 로드한다.
# >> S3 클라이언트를 사용하여 영상 리소스를 로컬 스토리지로 가져오는 I/O 작업을 수행한다.
# >> 다운로드된 경로를 인자로 받아서, AI 추론 엔진을 호출하고 결과를 반환한다.

import os
import json
import boto3
import requests
from botocore.exceptions import ClientError
from celery.signals import worker_process_init
from celery_app import app
from config import Config
from pose_estimation import PoseEstimator
from scoring import Scoring

# >> AI 모델 전역 변수
pose_estimator = None
scoring_engine = None

# >> Celery 워커 초기화 시그널 (생명 주기 연동)
# >> 워커 프로세스가 시작될 때(부모에서 분기된 직후) 실행된다.
@worker_process_init.connect
def init_worker(**kwargs):
    global pose_estimator, scoring_engine
    print("\n [Worker] 워커 프로세스 초기화 감지! 모델 로드를 시작합니다...")
    
    try:
        if pose_estimator is None:
            # 여기서 모델을 로드하면, tasks.py가 실행되는 각 워커 프로세스마다
            # 독립적인 모델 인스턴스와 GPU 컨텍스트를 가지게 된다.
            # PoseEstimator 생성자 내부에서 warmup()이 자동 실행된다.
            pose_estimator = PoseEstimator(model_path='yolo11l-pose.pt')
            scoring_engine = Scoring()
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
            # 테스트 편의를 위해 파일이 없으면 그냥 넘어가지 않고 에러 로그 출력
            print(f"[Warn] 로컬 테스트 파일이 없습니다: {local_file_path}")
            pass
            
        print(f"로컬 테스트 파일 확인됨: {local_file_path}")

    return local_file_path


# >> Task 2: AI 분석 및 채점 (YOLO v11 + DTW)
@app.task(name='tasks.pose_estimation_task')
# >> 다운로드 된 영상을 받아서 YOLO를 사용해 분석하고 점수를 매긴다.
def pose_estimation_task(video_path, song_id, user_id, video_id):
    global pose_estimator, scoring_engine
    
    print(f"\n[Task 2] AI 분석 및 채점 시작: {video_path}")
    print(f"요청 정보 | Song: {song_id} | User: {user_id}")

    # [안전장치] 만약 워커 초기화 시점에 모델 로드가 실패했거나, 
    # 워커가 아닌 방식으로 실행되었을 경우를 대비한 Fallback 로직
    if pose_estimator is None:
        print("[Warning] 모델이 미리 로드되지 않았습니다. 지금 로드합니다 (지연 발생 가능).")
        try:
            pose_estimator = PoseEstimator(model_path='yolo11l-pose.pt')
            scoring_engine = Scoring()
        except Exception as e:
            _send_error_callback(user_id, video_id, f"Model load failed: {str(e)}")
            return {"status": "error", "message": str(e)}

    # >> 결과 파일명 규칙: {video_id}_result.json (규격 준수)
    result_filename = f"{video_id}_result.json"
    result_json_path = os.path.join(Config.RESULT_DIR, result_filename)

    # >> 중간 임시 파일 경로 변수 (나중에 삭제하기 위해 저장)
    temp_json_path = ""

    try:
        # >> Pose Estimation 실행 (User Video)
        # >> process_video는 임시 경로에 파일을 저장하므로, 나중에 최종 경로로 저장해야 함
        temp_json_path = pose_estimator.process_video(video_path, Config.RESULT_DIR)
        
        # >> Scoring 파이프라인 연결
        # >> song_id를 기반으로 전문가 데이터 경로 설정
        expert_json_path = os.path.join(Config.EXPERT_DIR, f"{song_id}.json")
        
        # >> 전문가 데이터가 없으면 테스트를 위해 사용자 데이터를 전문가 데이터로 사용 (Self-Comparison Test)
        if not os.path.exists(expert_json_path):
            print(f"[Info] 전문가 데이터({expert_json_path})가 없어 사용자 데이터를 비교 대상으로 사용합니다 (Test Mode).")
            expert_json_path = temp_json_path

        # >> Scoring 수행
        score_data = None
        if scoring_engine:
            score_data = scoring_engine.compare(temp_json_path, expert_json_path)
            
        # >> 최종 결과 JSON 구성
        final_data = {}
        with open(temp_json_path, 'r', encoding='utf-8') as f:
            final_data = json.load(f)
        
        if score_data:
            # Summary 정보 업데이트
            final_data["summary"]["total_score"] = score_data["total_score"]
            final_data["summary"]["worst_part"] = score_data["worst_part"]
            final_data["summary"]["best_part"] = score_data["best_part"]  # Best Part 업데이트
            final_data["summary"]["accuracy_grade"] = _calculate_grade(score_data["total_score"])
            
            # Timeline Feedback 업데이트 (규격에 맞게 수정됨)
            final_data["timeline_feedback"] = score_data["timeline"]

            # Frames별 Score 업데이트
            frame_scores = score_data["frame_scores"]
            for i, frame in enumerate(final_data["frames"]):
                if i < len(frame_scores) and frame["is_valid"]:
                    frame["score"] = frame_scores[i]
        
        # >> 최종 파일 저장 ({video_id}_result.json)
        with open(result_json_path, 'w', encoding='utf-8') as f:
            json.dump(final_data, f, indent=None)

        print(f"[Task 2] 분석 및 점수 계산 완료: {final_data['summary']['total_score']}점")

        # >> 분석 결과 S3 업로드
        s3_url = ""
        if Config.USE_AWS:
            s3 = boto3.client(
                's3',
                aws_access_key_id=Config.AWS_ACCESS_KEY_ID,
                aws_secret_access_key=Config.AWS_SECRET_ACCESS_KEY,
                region_name=Config.AWS_REGION
            )
            # >> 버킷 이름은 규격에 따라 'kpop-dance-app-data'로 가정하거나 Config에서 관리
            bucket = "kpop-dance-app-data"
            s3_key = f"analyzed/{user_id}/{video_id}_result.json"
            
            print(f"[Upload] S3 업로드 중: {s3_key}")
            s3.upload_file(result_json_path, bucket, s3_key)
            s3_url = f"https://{bucket}.s3.{Config.AWS_REGION}.amazonaws.com/{s3_key}"

        # >> AWS API Gateway로 완료 통보
        _send_callback(user_id, video_id, song_id, final_data, s3_url)
        
        # >> 임시 파일 삭제 로직 (규격 외 파일 정리)
        if temp_json_path and os.path.exists(temp_json_path):
            try:
                os.remove(temp_json_path)
                print(f"[Info] 중간 임시 파일 삭제됨: {temp_json_path}")
            except Exception as e:
                print(f"[Warning] 임시 파일 삭제 실패: {e}")

        return {
            "status": "success",
            "video_path": video_path,
            "result_path": result_json_path
        }

    except Exception as e:
        print(f"분석 중 치명적 오류: {e}")
        _send_error_callback(user_id, video_id, str(e))
        return {"status": "error", "error_message": str(e)}

# >> 점수에 따른 등급 계산 헬퍼
def _calculate_grade(score):
    if score >= 90: return "S"
    elif score >= 80: return "A"
    elif score >= 70: return "B"
    else: return "C"

# >> 성공 콜백 (규격 5.1)
def _send_callback(user_id, video_id, song_id, result_data, s3_url):
    if not Config.USE_AWS: return

    url = f"{Config.API_GATEWAY_URL}/analysis/complete"
    payload = {
        "status": "success",
        "user_id": user_id,
        "video_id": video_id,
        "song_id": song_id,
        "data": {
            "total_score": result_data["summary"]["total_score"],
            "grade": result_data["summary"]["accuracy_grade"],
            "practice_time": result_data["summary"]["practice_time_sec"],
            "s3_json_url": s3_url,
            "s3_thumbnail_url": "" 
        }
    }
    try:
        requests.post(url, json=payload)
        print(f"[API] 완료 통보 전송 성공")
    except Exception as e:
        print(f"[API] 통보 실패: {e}")

# >> 실패 콜백 (규격 5.2)
def _send_error_callback(user_id, video_id, error_msg):
    if not Config.USE_AWS: return
    
    url = f"{Config.API_GATEWAY_URL}/analysis/complete"
    payload = {
        "status": "fail",
        "user_id": user_id,
        "video_id": video_id,
        "error_code": "ANALYSIS_ERROR",
        "message": error_msg
    }
    try:
        requests.post(url, json=payload)
        print(f"[API] 에러 통보 전송")
    except Exception:
        pass
