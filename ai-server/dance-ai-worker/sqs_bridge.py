# >> sqs_bridge.py
# >> AWS SQS를 지속적으로 모니터링하고, 수신된 이벤트를 Celery 작업 체인으로 변환하여 로컬 Worker에게 전달한다.
# >> AWS SQS로부터 분석 요청 메시지를 수신한다.
# >> 단일 작업이 아닌 "영상 다운로드 -> AI 분석" 순서대로 이어지는 작업 체인을 생성하여 실행 순서를 보장한다.
# >> Config.USE_AWS = False를 대비해, 외부 통신 없이 로컬 테스트 데이터를 통해 파이프라인을 검증한다.

import boto3
import json
import time
import os
from celery import chain
from config import Config
from tasks import download_video_task, pose_estimation_task

def get_sqs_client():
    if Config.USE_AWS:
        return boto3.client(
            'sqs',
            region_name=Config.AWS_REGION,
            aws_access_key_id=Config.AWS_ACCESS_KEY_ID,
            aws_secret_access_key=Config.AWS_SECRET_ACCESS_KEY
        )
    else:
        # AWS 없이 테스트할 때는 클라이언트 생성을 건너뜁니다.
        return None

# >> 파일명에서 최소 메타데이터를 추출
def _parse_filename_metadata(file_key):
    try:
        filename = os.path.basename(file_key)
        name_without_ext = filename.rsplit('.', 1)[0]
        parts = name_without_ext.split('_')

        # 예상: user_song_part_partName_timestamp (partName은 언더스코어 제거되어 들어오는 전제)
        if len(parts) < 5:
            return None

        user_id = parts[0]
        timestamp = int(parts[-1])
        song_id = "_".join(parts[1:-1])  # fallback용 song_id
        return {
            "user_id": user_id,
            "song_id": song_id,
            "timestamp": timestamp
        }
    except Exception:
        return None

# >> 규격서에 따른 메시지 파싱
def parse_analysis_request(body_json):
    """
    {
      "bucket_name": "...",
      "file_key": "raw/userID_songID_Artist_PartNumber.mp4",
      "song_id": "songID_Artist_PartNumber",
      "user_id": "userID",
      "timestamp": 1234567890 (추가됨)
    }
    """
    try:
        data = json.loads(body_json)
    except Exception:
        return None

    # SNS -> SQS 형태 지원 (Message 내부에 JSON 문자열)
    if isinstance(data, dict) and isinstance(data.get("Message"), str):
        try:
            data = json.loads(data["Message"])
        except Exception:
            return None

    # S3 이벤트 원문이 그대로 들어온 경우 지원
    if isinstance(data, dict) and "Records" in data and isinstance(data["Records"], list) and data["Records"]:
        first = data["Records"][0]
        s3 = first.get("s3", {}) if isinstance(first, dict) else {}
        bucket = s3.get("bucket", {}).get("name")
        file_key = s3.get("object", {}).get("key")
        if bucket and file_key:
            data = {
                "bucket_name": bucket,
                "file_key": file_key
            }

    if not isinstance(data, dict):
        return None

    # Lambda 메시지 키 별칭 지원
    normalized = {
        "bucket_name": data.get("bucket_name") or data.get("bucket"),
        "file_key": data.get("file_key") or data.get("s3_key"),
        "song_id": data.get("song_id"),
        "user_id": data.get("user_id"),
        "timestamp": data.get("timestamp")
    }

    # 필수값 누락 시 파일명 기반 fallback 시도
    if normalized["file_key"]:
        parsed = _parse_filename_metadata(normalized["file_key"])
        if parsed:
            normalized["song_id"] = normalized["song_id"] or parsed["song_id"]
            normalized["user_id"] = normalized["user_id"] or parsed["user_id"]
            normalized["timestamp"] = normalized["timestamp"] or parsed["timestamp"]

    try:
        normalized["timestamp"] = int(normalized["timestamp"])
    except Exception:
        return None

    required_keys = ['bucket_name', 'file_key', 'song_id', 'user_id', 'timestamp']
    if all(normalized.get(key) is not None for key in required_keys):
        return normalized

    return None

def run_bridge():
    print(f"SQS Bridge 가동 시작...")
    
    # >> [TEST] AWS가 없을 때 강제로 로컬 테스트를 수행하는 로직
    if not Config.USE_AWS:
        print("\n[TEST MODE] AWS 연결이 꺼져 있습니다 (Config.USE_AWS = False)")
        print("SQS를 감시하는 대신, 로컬 테스트 영상을 강제로 작업 큐에 넣습니다.")
        
        # >> 테스트할 가짜 데이터 (통신 규격에 맞춤)
        # >> video_id 필드는 제거되었고, song_id와 user_id를 통해 식별한다.
        # >> [추가] DB 키로 사용할 timestamp 추가
        test_payload = {
            "bucket_name": "test-bucket",
            "file_key": "raw/test_user_song_001_IVE_Part1.mp4", 
            "song_id": "song_001_IVE_Part1",
            "user_id": "test_user",
            "timestamp": int(time.time() * 1000)
        }
        
        print(f"테스트 요청 전송: {test_payload}")
        
        # >> Chain: 다운로드 Task -> 분석 Task 연결
        # >> download_video_task의 리턴값(파일경로)이 pose_estimation_task의 첫 번째 인자로 자동 전달됨
        # >> pose_estimation_task에서 video_id 인자 제거, timestamp 인자 추가
        workflow = chain(
            download_video_task.s(
                test_payload['bucket_name'], 
                test_payload['file_key']
            ) | 
            pose_estimation_task.s(
                test_payload['song_id'],
                test_payload['user_id'],
                test_payload['timestamp']
            )
        )
        
        workflow.delay()
        print("   Celery에 작업 체인 전송 완료! (Worker 터미널을 확인하세요)")
        return

    # >> 실제 AWS SQS 폴링
    sqs = get_sqs_client()
    queue_url = Config.SQS_QUEUE_URL
    print(f"SQS 대기열 감시 중... {queue_url}")

    while True:
        try:
            response = sqs.receive_message(
                QueueUrl=queue_url, MaxNumberOfMessages=1, WaitTimeSeconds=20
            )
            
            if 'Messages' in response:
                for message in response['Messages']:
                    req_data = parse_analysis_request(message['Body'])
                    
                    if req_data:
                        print(f"\n메시지 수신 ID: {message['MessageId']}")
                        print(f"분석 요청: Song={req_data['song_id']}, User={req_data['user_id']}, TS={req_data['timestamp']}")
                        
                        # >> 체인으로 연결하여 실행 (video_id 제거, timestamp 전달)
                        chain(
                            download_video_task.s(
                                req_data['bucket_name'], 
                                req_data['file_key']
                            ) | 
                            pose_estimation_task.s(
                                req_data['song_id'],
                                req_data['user_id'],
                                req_data['timestamp']
                            )
                        ).delay()
                        
                        # >> 메시지 삭제 (작업 큐에서 제거)
                        sqs.delete_message(QueueUrl=queue_url, ReceiptHandle=message['ReceiptHandle'])
                    else:
                        raw_preview = message.get('Body', '')[:500]
                        print(f"유효하지 않은 메시지 형식입니다. 삭제 처리합니다. body={raw_preview}")
                        sqs.delete_message(QueueUrl=queue_url, ReceiptHandle=message['ReceiptHandle'])
                        
        except Exception as e:
            print(f"Error: {e}")
            time.sleep(5)

if __name__ == "__main__":
    run_bridge()
