import os
import boto3
import json
import time
from decimal import Decimal

# 폴더 생성
DOWNLOAD_DIR = "temp_videos"
if not os.path.exists(DOWNLOAD_DIR):
    os.makedirs(DOWNLOAD_DIR)

# AWS 설정
sqs = boto3.client('sqs', region_name='ap-northeast-1')
s3 = boto3.client('s3', region_name='ap-northeast-1')
dynamodb = boto3.resource('dynamodb', region_name='ap-northeast-1')

QUEUE_URL = 'https://sqs.ap-northeast-1.amazonaws.com/881211378731/ai-anlysis-queue'
BUCKET_NAME = 'kpop-dance-app-data'
table = dynamodb.Table('UserVideos')

def process_video(message_data):
    # 데이터 타입 강제
    user_id = str(message_data['user_id'])
    timestamp = int(message_data['timestamp'])
    s3_key = message_data['s3_key']
    filename = message_data['filename'] # 예: xooyong_Dynamite_1_인트로_1767887685286.mp4
    
    # 경로 설정
    temp_dir = os.path.join(os.getcwd(), DOWNLOAD_DIR)
    if not os.path.exists(temp_dir):
        os.makedirs(temp_dir)
    
    local_video = os.path.join(temp_dir, filename)
    print(f"[{user_id}] 분석 시작: {filename}")
    
    # 1. 상태 업데이트: processing
    table.update_item(
        Key={'user_id': user_id, 'timestamp': timestamp},
        UpdateExpression='SET #status = :status',
        ExpressionAttributeNames={'#status': 'status'},
        ExpressionAttributeValues={':status': 'processing'}
    )
    
    try:
        # 2. S3 다운로드
        s3.download_file(BUCKET_NAME, s3_key, local_video)
        print(f"다운로드 완료: {local_video}")
        
        # 3. 분석 (YOLOv11 등)
        result = analyze_video(local_video)
        
        # ✅ 4. 결과 JSON 파일명 수정 (원본 파일명 활용)
        # 확장자(.mp4)를 제거하고 _result.json을 붙입니다.
        name_without_ext = filename.rsplit('.', 1)[0]
        result_key = f"results/{name_without_ext}_result.json"
        
        result_json = {
            'user_id': user_id,
            'timestamp': timestamp,
            'score': result['score'],
            'feedback': result['feedback']
        }
        
        # S3 업로드
        s3.put_object(
            Bucket=BUCKET_NAME,
            Key=result_key,
            Body=json.dumps(result_json),
            ContentType='application/json'
        )
        print(f"결과 업로드 완료: {result_key}")
        
        # 5. 상태 업데이트: completed
        table.update_item(
            Key={'user_id': user_id, 'timestamp': timestamp},
            UpdateExpression='SET #status = :status, result_s3_key = :key',
            ExpressionAttributeNames={'#status': 'status'},
            ExpressionAttributeValues={
                ':status': 'completed',
                ':key': result_key
            }
        )
        print(f"✅ DB 업데이트 완료: {filename}")
        
        # 로컬 파일 삭제 (용량 관리)
        if os.path.exists(local_video):
            os.remove(local_video)
        return True

    except Exception as e:
        print(f"❌ 에러 발생: {e}")
        table.update_item(
            Key={'user_id': user_id, 'timestamp': timestamp},
            UpdateExpression='SET #status = :status, error_message = :msg',
            ExpressionAttributeNames={'#status': 'status'},
            ExpressionAttributeValues={':status': 'failed', ':msg': str(e)}
        )
        return False

def analyze_video(video_path):
    # 실제 YOLOv11 모델 추론 코드가 들어갈 자리
    time.sleep(3) 
    return {'score': 90, 'feedback': 'Great move!'}

def main():
    print("🚀 AI Server Polling Started...")
    while True:
        try:
            response = sqs.receive_message(
                QueueUrl=QUEUE_URL,
                MaxNumberOfMessages=1,
                WaitTimeSeconds=20
            )
            
            if 'Messages' in response:
                for message in response['Messages']:
                    message_data = json.loads(message['Body'])
                    if process_video(message_data):
                        sqs.delete_message(
                            QueueUrl=QUEUE_URL,
                            ReceiptHandle=message['ReceiptHandle']
                        )
                        print("Message deleted from SQS")
            else:
                print("Checking for messages...")
                
        except Exception as e:
            print(f"Polling Error: {e}")
            time.sleep(5)

if __name__ == '__main__':
    main()