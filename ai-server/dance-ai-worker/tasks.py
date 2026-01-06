# >> tasks.py
# >> I/O 바운드 작업(다운로드)와 CPU/GPU 바운드 작업(AI 추론)을 분리하여 정의하고, Worker 프로세스의 리소스를 관리한다.
# >> Worker 프로세스가 Fork 될 때, AI 모델을 메모리에 미리 로드한다.
# >> S3 클라이언트를 사용하여 영상 리소스를 로컬 스토리지로 가져오는 I/O 작업을 수행한다.
# >> 다운로드된 경로를 인자로 받아서, AI 추론 엔진을 호출하고 결과를 반환한다.

import os
import json
import boto3
import requests
import gzip
import shutil
import logging
from botocore.exceptions import ClientError, NoCredentialsError
from celery.signals import worker_process_init
from celery_app import app
from config import Config
from pose_estimation import PoseEstimator
from scoring import Scoring

# >> 로깅 설정
# >> 기존 print 대신 logging을 사용하여 레벨별 로그 관리 및 파일 저장이 용이하도록 변경
logging.basicConfig(
    level=logging.INFO,
    format='[%(asctime)s] [%(levelname)s] %(message)s',
    datefmt='%Y-%m-%d %H:%M:%S'
)
logger = logging.getLogger(__name__)

# >> AI 모델 전역 변수
pose_estimator = None
scoring_engine = None

# -----------------------------------------------------------------------------
# [Helper Class 1] 파일명 및 메타데이터 파싱 로직 분리
# -----------------------------------------------------------------------------
class VideoMetadataParser:
    @staticmethod
    def parse(video_path, song_id, user_id):
        # >> [변경] 파일명에서 정보 파싱
        # >> 영상 파일명 예시: userID_songID_Artist_PartNumber.mp4
        try:
            video_filename = os.path.basename(video_path)
            video_name_no_ext = os.path.splitext(video_filename)[0]
            
            # 1. Full ID 설정 (userID_songID_Artist_PartNumber)
            full_song_identifier = video_name_no_ext

            # 2. 전문가 파일명 파싱 (userID_ 접두사 제거)
            # >> [변경] 전문가 JSON 파일 이름은 songID_Artist_PartNumber.json으로 저장할거야.
            expert_json_filename = ""
            prefix = f"{user_id}_"
            
            if video_name_no_ext.startswith(prefix):
                parsed_song_part = video_name_no_ext[len(prefix):]
                expert_json_filename = f"{parsed_song_part}.json"
            else:
                # 파싱 실패 시 SQS에서 받은 song_id 사용 (Fallback)
                logger.info(f"[Parser] 파일명 파싱 실패(prefix 불일치), SQS song_id 사용: {song_id}")
                expert_json_filename = f"{song_id}.json"
                
            return full_song_identifier, expert_json_filename

        except Exception as e:
            logger.error(f"[Parser] 메타데이터 파싱 중 오류 발생: {e}")
            # 파싱 실패 시 최소한의 정보 반환
            return song_id, f"{song_id}.json"

# -----------------------------------------------------------------------------
# [Helper Class 2] S3 업로드/다운로드 로직 분리
# -----------------------------------------------------------------------------
class S3Manager:
    def __init__(self):
        self.enabled = Config.USE_AWS
        self.client = None
        if self.enabled:
            try:
                self.client = boto3.client(
                    's3',
                    aws_access_key_id=Config.AWS_ACCESS_KEY_ID,
                    aws_secret_access_key=Config.AWS_SECRET_ACCESS_KEY,
                    region_name=Config.AWS_REGION
                )
            except Exception as e:
                logger.error(f"[S3] 클라이언트 초기화 실패: {e}")

    def download_file(self, bucket, key, local_path):
        if not self.enabled:
            logger.info("[S3] AWS 비활성화됨. 다운로드 건너뜀 (로컬 테스트 모드)")
            return False

        try:
            logger.info(f"[S3] 다운로드 시작: s3://{bucket}/{key} -> {local_path}")
            self.client.download_file(bucket, key, local_path)
            logger.info(f"[S3] 다운로드 완료")
            return True
        except ClientError as e:
            logger.error(f"[S3] 다운로드 실패 (ClientError): {e}")
            raise e
        except Exception as e:
            logger.error(f"[S3] 다운로드 실패 (Unknown): {e}")
            raise e

    def upload_json_gzipped(self, json_path, bucket, s3_key):
        if not self.enabled:
            return ""

        gz_path = json_path + ".gz"
        try:
            # 1. 로컬에서 GZIP 압축 파일 생성
            with open(json_path, 'rb') as f_in:
                with gzip.open(gz_path, 'wb') as f_out:
                    shutil.copyfileobj(f_in, f_out)
            
            logger.info(f"[S3] 업로드 시작 (GZIP): {s3_key}")
            
            # 2. 압축된 파일 업로드
            self.client.upload_file(
                gz_path, 
                bucket, 
                s3_key,
                ExtraArgs={
                    'ContentType': 'application/json',
                    'ContentEncoding': 'gzip'
                }
            )
            
            # S3 URL 생성
            s3_url = f"https://{bucket}.s3.{Config.AWS_REGION}.amazonaws.com/{s3_key}"
            logger.info(f"[S3] 업로드 완료: {s3_url}")
            
            return s3_url
            
        except Exception as e:
            logger.error(f"[S3] 업로드 실패: {e}")
            return ""
        finally:
            # 3. 임시 압축 파일 삭제
            if os.path.exists(gz_path):
                os.remove(gz_path)

# -----------------------------------------------------------------------------
# [Helper Class 3] 콜백 로직 분리
# -----------------------------------------------------------------------------
class CallbackNotifier:
    @staticmethod
    def send_success(user_id, song_identifier, result_data, s3_url):
        if not Config.USE_AWS: return

        url = f"{Config.API_GATEWAY_URL}/analysis/complete"
        # >> [변경] 완료 통보 API 규격에서, video_id를 "song_id": "userID_songID_Artist_PartNumber"로 바꿔서 사용
        payload = {
            "status": "success",
            "user_id": user_id,
            "song_id": song_identifier, 
            "data": {
                "total_score": result_data["summary"]["total_score"],
                "grade": result_data["summary"]["accuracy_grade"],
                "s3_json_url": s3_url,
                "s3_thumbnail_url": "" 
            }
        }
        try:
            response = requests.post(url, json=payload, timeout=5)
            response.raise_for_status()
            logger.info(f"[API] 완료 통보 성공: {payload}")
        except requests.exceptions.RequestException as e:
            logger.error(f"[API] 완료 통보 실패: {e}")

    @staticmethod
    def send_error(user_id, song_identifier, error_msg):
        if not Config.USE_AWS: return
        
        url = f"{Config.API_GATEWAY_URL}/analysis/complete"
        payload = {
            "status": "fail",
            "user_id": user_id,
            "song_id": song_identifier, 
            "error_code": "ANALYSIS_ERROR",
            "message": str(error_msg)
        }
        try:
            requests.post(url, json=payload, timeout=5)
            logger.info(f"[API] 에러 통보 전송: {payload}")
        except Exception as e:
            logger.error(f"[API] 에러 통보 실패: {e}")

# -----------------------------------------------------------------------------
# Celery Tasks
# -----------------------------------------------------------------------------

# >> Celery 워커 초기화 시그널 (생명 주기 연동)
# >> 워커 프로세스가 시작될 때(부모에서 분기된 직후) 실행된다.
@worker_process_init.connect
def init_worker(**kwargs):
    global pose_estimator, scoring_engine
    
    logger.info("[Worker] 워커 프로세스 초기화 감지! 모델 로드를 시작합니다...")
    
    try:
        if pose_estimator is None:
            # 여기서 모델을 로드하면, tasks.py가 실행되는 각 워커 프로세스마다
            # 독립적인 모델 인스턴스와 GPU 컨텍스트를 가지게 된다.
            pose_estimator = PoseEstimator(model_path='yolo11l-pose.pt')
            scoring_engine = Scoring()
            logger.info("[Worker] 모델 로드 및 워밍업 완료, 작업을 기다립니다.")
    except Exception as e:
        logger.critical(f"[Worker] 모델 초기화 실패: {e}")

# >> Task 1: 영상 다운로드 (S3 <-> Local)
@app.task(
    name='tasks.download_video_task',
    queue='io_queue',
    bind=True,             
    max_retries=3,         
    default_retry_delay=5  
)
def download_video_task(self, bucket_name, video_key):
    logger.info(f"[Task 1] 다운로드 요청 시작 (IO Queue): {video_key}")
    
    try:
        file_name = os.path.basename(video_key)
        local_file_path = os.path.join(Config.DOWNLOAD_DIR, file_name)
        
        # S3 Manager 사용
        s3_manager = S3Manager()
        
        if Config.USE_AWS:
            s3_manager.download_file(bucket_name, video_key, local_file_path)
        else:
            # === [MOCK] 로컬 테스트 모드 ===
            logger.info("[TEST MODE] AWS 연결 없이 로컬 파일을 사용합니다.")
            if not os.path.exists(local_file_path):
                logger.warning(f"[Warn] 로컬 테스트 파일이 없습니다: {local_file_path}")
        
        return local_file_path
        
    except ClientError as e:
        logger.error(f"[Task 1] S3 다운로드 중 ClientError 발생: {e}")
        raise self.retry(exc=e)
    except Exception as e:
        logger.error(f"[Task 1] 다운로드 중 알 수 없는 오류 발생: {e}")
        raise self.retry(exc=e)


# >> Task 2: AI 분석 및 채점 (YOLO v11 + DTW)
@app.task(
    name='tasks.pose_estimation_task',
    queue='gpu_queue'
)
def pose_estimation_task(video_path, song_id, user_id):
    global pose_estimator, scoring_engine
    
    logger.info(f"[Task 2] AI 분석 및 채점 시작 (GPU Queue): {video_path}")
    logger.info(f"요청 정보 | Song: {song_id} | User: {user_id}")
    
    # 1. 파일명 파싱 (Helper Class 사용)
    full_song_identifier, expert_json_filename = VideoMetadataParser.parse(video_path, song_id, user_id)
    logger.info(f"[Info] 타겟 전문가 데이터 파일: {expert_json_filename}")

    # [안전장치] 모델 로드 체크
    if pose_estimator is None:
        logger.warning("[Warning] 모델이 미리 로드되지 않았습니다. 지금 로드합니다 (지연 발생 가능).")
        try:
            pose_estimator = PoseEstimator(model_path='yolo11l-pose.pt')
            scoring_engine = Scoring()
        except Exception as e:
            err_msg = f"Model load failed: {str(e)}"
            logger.error(err_msg)
            CallbackNotifier.send_error(user_id, full_song_identifier, err_msg)
            return {"status": "error", "message": str(e)}

    # >> [변경] 결과 파일명 규칙: userID_songID_Artist_PartNumber_result.json
    result_filename = f"{full_song_identifier}_result.json"
    result_json_path = os.path.join(Config.RESULT_DIR, result_filename)
    temp_json_path = ""

    try:
        # 2. Pose Estimation 실행 (User Video)
        try:
            temp_json_path = pose_estimator.process_video(video_path, Config.RESULT_DIR)
        except Exception as pe_err:
            raise RuntimeError(f"Pose Estimation 실패: {pe_err}")
        
        # 3. 전문가 데이터 준비 (S3 Manager 사용)
        expert_json_path = os.path.join(Config.EXPERT_DIR, expert_json_filename)
        s3_manager = S3Manager()
        
        # 로컬에 없으면 다운로드 시도
        if not os.path.exists(expert_json_path) and Config.USE_AWS:
            logger.info(f"[Info] 전문가 데이터가 로컬에 없습니다. S3 다운로드 시도: {expert_json_filename}")
            try:
                # expert_bucket = "kpop-dance-app-data"
                expert_bucket = Config.S3_BUCKET_NAME
                expert_key = f"expert/{expert_json_filename}"
                s3_manager.download_file(expert_bucket, expert_key, expert_json_path)
            except Exception as dl_err:
                logger.warning(f"[Warning] 전문가 데이터 다운로드 실패: {dl_err}")

        # 전문가 데이터 부재 시 Fallback (Test Mode)
        if not os.path.exists(expert_json_path):
            logger.info(f"[Info] 전문가 데이터({expert_json_path})가 없어 사용자 데이터를 비교 대상으로 사용합니다 (Test Mode).")
            expert_json_path = temp_json_path

        # 4. Scoring 수행
        score_data = None
        if scoring_engine:
            try:
                score_data = scoring_engine.compare(temp_json_path, expert_json_path)
            except Exception as sc_err:
                logger.error(f"Scoring Engine 오류: {sc_err}")
                # 채점 실패 시에도 기본 데이터는 남기기 위해 진행하거나 에러 처리
        
        # 5. 최종 결과 JSON 구성
        final_data = {}
        with open(temp_json_path, 'r', encoding='utf-8') as f:
            final_data = json.load(f)
        
        if score_data:
            final_data["summary"]["total_score"] = score_data["total_score"]
            final_data["summary"]["worst_points"] = score_data["worst_points"]
            final_data["summary"]["part_accuracies"] = score_data["part_accuracies"]
            
            visibility_ratio = score_data.get("visibility_ratio", 1.0)
            final_data["summary"]["accuracy_grade"] = _calculate_grade(score_data["total_score"], visibility_ratio)
            
            # final_data["timeline_feedback"] = score_data["timeline"]  <-- 제거됨
            
            frame_scores = score_data["frame_scores"]
            frame_errors = score_data["frame_errors"]
            
            for i, frame in enumerate(final_data["frames"]):
                if i < len(frame_scores) and frame["is_valid"]:
                    frame["score"] = frame_scores[i]
                    if i < len(frame_errors):
                        frame["errors"] = frame_errors[i]
        
        # 결과 파일 저장
        with open(result_json_path, 'w', encoding='utf-8') as f:
            json.dump(final_data, f, indent=None)

        logger.info(f"[Task 2] 분석 및 점수 계산 완료: {final_data['summary']['total_score']}점 (Grade: {final_data['summary']['accuracy_grade']})")

        # 6. 결과 업로드 (S3 Manager 사용)
        s3_url = ""
        if Config.USE_AWS:
            # bucket = "kpop-dance-app-data"
            bucket = Config.S3_BUCKET_NAME
            s3_key = f"analyzed/{user_id}/{result_filename}"
            s3_url = s3_manager.upload_json_gzipped(result_json_path, bucket, s3_key)

        # 7. 성공 콜백 전송 (Callback Notifier 사용)
        CallbackNotifier.send_success(user_id, full_song_identifier, final_data, s3_url)
        
        # 임시 파일 정리
        if temp_json_path and os.path.exists(temp_json_path) and temp_json_path != result_json_path:
            try:
                os.remove(temp_json_path)
                logger.info(f"[Info] 중간 임시 파일 삭제됨: {temp_json_path}")
            except OSError as e:
                logger.warning(f"[Warning] 임시 파일 삭제 실패: {e}")

        return {
            "status": "success",
            "video_path": video_path,
            "result_path": result_json_path
        }

    except Exception as e:
        logger.critical(f"분석 중 치명적 오류: {e}", exc_info=True)
        CallbackNotifier.send_error(user_id, full_song_identifier, str(e))
        return {"status": "error", "error_message": str(e)}


# >> 점수에 따른 등급 계산 헬퍼
def _calculate_grade(score, visibility_ratio=1.0):
    # 1차 등급 산정 (S:85이상, A:75~84, B:60~74, C:60미만)
    if score >= 85: grade = "S"
    elif score >= 75: grade = "A"
    elif score >= 60: grade = "B"
    else: grade = "C"

    # >> [보완 Step 2-2] 소실된 시간(1 - visibility_ratio)이 30%를 넘으면 등급 강등
    if visibility_ratio < 0.7:
        logger.info(f"[Grade] 페널티 적용됨 (Ratio: {visibility_ratio:.2f}) -> 등급 하향 조정")
        grades_order = ["S", "A", "B", "C"]
        try:
            current_idx = grades_order.index(grade)
            # 한 단계 강등 (C 밑으로는 유지)
            new_idx = min(current_idx + 1, len(grades_order) - 1)
            grade = grades_order[new_idx]
        except ValueError:
            pass 
            
    return grade
