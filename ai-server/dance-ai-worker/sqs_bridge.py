# >> sqs_bridge.py
# >> AWS SQS를 지속적으로 모니터링하고, 수신된 이벤트를 Celery 작업 체인으로 변환하여 로컬 Worker에게 전달한다.
# >> AWS SQS로부터 분석 요청 메시지를 수신한다.
# >> 단일 작업이 아닌 "영상 다운로드 -> AI 분석" 순서대로 이어지는 작업 체인을 생성하여 실행 순서를 보장한다.
# >> Config.USE_AWS = False를 대비해, 외부 통신 없이 로컬 테스트 데이터를 통해 파이프라인을 검증한다.

import boto3
import json
import time
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

# >> 규격서에 따른 메시지 파싱
def parse_analysis_request(body_json):
    """
    {
      "bucket_name": "...",
      "file_key": "raw/userID_songID_Artist_PartNumber.mp4",
      "song_id": "songID_Artist_PartNumber",
      "user_id": "userID"
    }
    """
    try:
        data = json.loads(body_json)
        # >> 필수 필드 확인 (video_id 제거됨)
        required_keys = ['bucket_name', 'file_key', 'song_id', 'user_id']
        if all(key in data for key in required_keys):
            return data
    except Exception:
        pass
    return None

def run_bridge():
    print(f"SQS Bridge 가동 시작...")
    
    # >> [TEST] AWS가 없을 때 강제로 로컬 테스트를 수행하는 로직
    if not Config.USE_AWS:
        print("\n[TEST MODE] AWS 연결이 꺼져 있습니다 (Config.USE_AWS = False)")
        print("SQS를 감시하는 대신, 로컬 테스트 영상을 강제로 작업 큐에 넣습니다.")
        
        # >> 테스트할 가짜 데이터 (통신 규격에 맞춤)
        # >> video_id 필드는 제거되었고, song_id와 user_id를 통해 식별한다.
        test_payload = {
            "bucket_name": "test-bucket",
            "file_key": "raw/test_user_song_001_IVE_Part1.mp4", 
            "song_id": "song_001_IVE_Part1",
            "user_id": "test_user"
        }
        
        print(f"테스트 요청 전송: {test_payload}")
        
        # >> Chain: 다운로드 Task -> 분석 Task 연결
        # >> download_video_task의 리턴값(파일경로)이 pose_estimation_task의 첫 번째 인자로 자동 전달됨
        # >> pose_estimation_task에서 video_id 인자 제거
        workflow = chain(
            download_video_task.s(
                test_payload['bucket_name'], 
                test_payload['file_key']
            ) | 
            pose_estimation_task.s(
                test_payload['song_id'],
                test_payload['user_id']
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
                        print(f"분석 요청: Song={req_data['song_id']}, User={req_data['user_id']}")
                        
                        # >> 체인으로 연결하여 실행 (video_id 제거)
                        chain(
                            download_video_task.s(
                                req_data['bucket_name'], 
                                req_data['file_key']
                            ) | 
                            pose_estimation_task.s(
                                req_data['song_id'],
                                req_data['user_id']
                            )
                        ).delay()
                        
                        # >> 메시지 삭제 (작업 큐에서 제거)
                        sqs.delete_message(QueueUrl=queue_url, ReceiptHandle=message['ReceiptHandle'])
                    else:
                        print(f"유효하지 않은 메시지 형식입니다. 삭제 처리합니다.")
                        sqs.delete_message(QueueUrl=queue_url, ReceiptHandle=message['ReceiptHandle'])
                        
        except Exception as e:
            print(f"Error: {e}")
            time.sleep(5)

if __name__ == "__main__":
    run_bridge()
