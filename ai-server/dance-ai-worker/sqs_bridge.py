# >> SQS를 감시하고 메시지를 파싱하여 Celery Task를 실행하는 무한 루프 스크립트다.

import boto3
import json
import time
import sys
from botocore.exceptions import ClientError
from config import Config
from tasks import download_video_task

def get_sqs_client():
    """AWS SQS 클라이언트 생성"""
    try:
        sqs = boto3.client(
            'sqs',
            region_name=Config.AWS_REGION,
            aws_access_key_id=Config.AWS_ACCESS_KEY_ID,
            aws_secret_access_key=Config.AWS_SECRET_ACCESS_KEY
        )
        return sqs
    except Exception as e:
        print(f"❌ AWS 클라이언트 생성 실패: {e}")
        sys.exit(1)

def parse_s3_event(body_json):
    """
    SQS 메시지 Body(JSON)에서 버킷 이름과 파일 키를 추출
    (AWS S3 Event Notification 구조 기준)
    """
    try:
        body = json.loads(body_json)
        
        # 테스트 메시지일 경우 (Event 필드가 있는 경우)
        if 'Event' in body and body['Event'] == 's3:TestEvent':
            print("ℹ️ AWS S3 테스트 이벤트 감지 (무시함)")
            return None, None

        # 실제 S3 업로드 이벤트 파싱
        if 'Records' in body:
            record = body['Records'][0]
            bucket_name = record['s3']['bucket']['name']
            video_key = record['s3']['object']['key']
            return bucket_name, video_key
            
        else:
            print(f"⚠️ 알 수 없는 메시지 형식: {body_json[:100]}...")
            return None, None
            
    except json.JSONDecodeError:
        print("❌ JSON 파싱 에러")
        return None, None
    except KeyError as e:
        print(f"❌ 필수 키 누락: {e}")
        return None, None

def run_bridge():
    """메인 실행 루프 (Long Polling)"""
    sqs = get_sqs_client()
    queue_url = Config.SQS_QUEUE_URL

    if not queue_url:
        print("❌ 설정 오류: .env 파일에 SQS_QUEUE_URL이 없습니다.")
        sys.exit(1)

    print(f"🌉 SQS Bridge 가동 시작...")
    print(f"📍 타겟 큐: {queue_url}")
    print("⏳ 메시지 대기 중 (Long Polling 20s)...")

    while True:
        try:
            # 1. SQS에서 메시지 수신 (최대 10개, 대기 시간 20초)
            response = sqs.receive_message(
                QueueUrl=queue_url,
                MaxNumberOfMessages=1,
                WaitTimeSeconds=20,  # Long Polling (비용 절감)
                VisibilityTimeout=30 # 30초 동안 다른 워커가 못 가져가게 함
            )

            # 2. 메시지가 없으면 다시 대기
            if 'Messages' not in response:
                continue

            for message in response['Messages']:
                print(f"\n📩 [SQS] 메시지 수신! ID: {message['MessageId']}")
                
                # 3. 메시지 파싱
                bucket, key = parse_s3_event(message['Body'])

                if bucket and key:
                    print(f"   - Bucket: {bucket}")
                    print(f"   - Key: {key}")
                    
                    # 4. Celery Task 호출 (비동기)
                    # .delay()를 쓰면 Redis 큐에 넣고 즉시 리턴됨
                    download_video_task.delay(bucket, key)
                    print("   🚀 [Celery] 작업 전달 완료!")

                    # 5. 처리 완료된 메시지 SQS에서 삭제
                    sqs.delete_message(
                        QueueUrl=queue_url,
                        ReceiptHandle=message['ReceiptHandle']
                    )
                    print("   🗑️ [SQS] 메시지 삭제 완료")
                else:
                    # 파싱 실패하거나 테스트 이벤트인 경우에도 메시지 삭제 (계속 쌓이지 않게)
                    print("   ⚠️ 유효하지 않은 이벤트이므로 삭제합니다.")
                    sqs.delete_message(
                        QueueUrl=queue_url,
                        ReceiptHandle=message['ReceiptHandle']
                    )

        except ClientError as e:
            print(f"❌ AWS 통신 에러: {e}")
            time.sleep(5) # 에러 발생 시 잠시 대기
        except KeyboardInterrupt:
            print("\n🛑 Bridge 종료 중...")
            sys.exit(0)
        except Exception as e:
            print(f"❌ 예상치 못한 에러: {e}")
            time.sleep(5)

if __name__ == "__main__":
    run_bridge()
