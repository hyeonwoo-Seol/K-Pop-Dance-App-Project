# >> sqs_bridge.py
# >> AWS SQS를 지속적으로 모니터링하고, 수신된 이벤트를 Celery 작업 체인으로 변환하여 로컬 Worker에게 전달한다.
# >> AWS SQS로부터 S3 Object Created 이벤트 메시지를 수신한다.
# >> 단일 작업이 아닌 "영상 다운로드 -> AI 분석" 순서대로 이어지는 작업 체인을 생성하여 실행 순서를 보장한다.
# >> Config.USE_AWS = False를 대비해, 외부 통신 없이 로컬 테스트 데이터를 통해 파이프라인을 검증한다.
import boto3
import json
import time
import sys
from botocore.exceptions import ClientError
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

def parse_s3_event(body_json):
    """AWS SQS 메시지에서 버킷과 파일 키 추출"""
    try:
        body = json.loads(body_json)
        if 'Records' in body:
            record = body['Records'][0]
            return record['s3']['bucket']['name'], record['s3']['object']['key']
    except Exception:
        pass
    return None, None

def run_bridge():
    print(f"SQS Bridge 가동 시작...")
    
    # >> [TEST] AWS가 없을 때 강제로 로컬 테스트를 수행하는 로직
    if not Config.USE_AWS:
        print("\n[TEST MODE AWS 연결이 꺼져 있습니다 (Config.USE_AWS = False)")
        print("SQS를 감시하는 대신, 로컬 테스트 영상을 강제로 작업 큐에 넣습니다.")
        
        # 테스트할 가짜 데이터
        test_bucket = "test-bucket"
        test_video_key = "IVE원영_AfterLike.mp4" # data/raw_videos 폴더에 이 파일이 있어야 함!
        
        print(f"테스트 요청 전송: {test_video_key}")
        
        #Chain: 다운로드 Task -> 분석 Task 연결
        # download_video_task의 리턴값(파일경로)이 pose_estimation_task의 인자로 자동 전달됨
        workflow = chain(
            download_video_task.s(test_bucket, test_video_key) | 
            pose_estimation_task.s()
        )
        
        workflow.delay()
        print("   🚀 Celery에 작업 체인 전송 완료! (Worker 터미널을 확인하세요)")
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
                    bucket, key = parse_s3_event(message['Body'])
                    if bucket and key:
                        print(f"\n메시지 수신: {key}")
                        
                        # 체인으로 연결하여 실행
                        chain(
                            download_video_task.s(bucket, key) | 
                            pose_estimation_task.s()
                        ).delay()
                        
                        sqs.delete_message(QueueUrl=queue_url, ReceiptHandle=message['ReceiptHandle'])
                        
        except Exception as e:
            print(f"Error: {e}")
            time.sleep(5)

if __name__ == "__main__":
    run_bridge()
