# >> config.py
# >> 시스템 전체의 환경 변수 및 상수를 관리한다.
# >> USE_AWS 변수를 통해 실제 배포 모드와 로컬 테스트 모드를 전환할 수 있다.
# >> AWS Access Key, Region, SQS URL 등의 정보를 .env로부터 불러온다.
# >> os.path 설정을 통해 파일 입출력 경로를 표준화한다.

import os
from dotenv import load_dotenv

# >> .env 파일 로드
load_dotenv()

class Config:
    # >> 이 값이 True이면 AWS와 실제 연결하는 것이고, False이면 로컬에서 테스트하는 것이다.
    USE_AWS = os.getenv('USE_AWS', 'False').lower() == 'true'

    # >> AWS 설정 (IAM 사용자 키)
    AWS_ACCESS_KEY_ID = os.getenv('AWS_ACCESS_KEY_ID')
    AWS_SECRET_ACCESS_KEY = os.getenv('AWS_SECRET_ACCESS_KEY')
    AWS_REGION = os.getenv('AWS_REGION', 'ap-northeast-2')

    # >> SQS 설정
    SQS_QUEUE_URL = os.getenv('SQS_QUEUE_URL')

    # >> S3 버킷 설정 (추가됨)
    # >> .env에 S3_BUCKET_NAME이 없으면 기본값인 'kpop-dance-app-data'를 사용한다.
    S3_BUCKET_NAME = os.getenv('S3_BUCKET_NAME', 'kpop-dance-app-data')

    # >> DynamoDB 테이블 설정 (추가됨)
    DYNAMODB_TABLE = os.getenv('DYNAMODB_TABLE', 'UserVideos')

    # >> 분석 완료 통보를 보낼 API Gateway 주소
    API_GATEWAY_URL = os.getenv('API_GATEWAY_URL', '')

    # >> Redis & Celery 설정
    CELERY_BROKER_URL = os.getenv('CELERY_BROKER_URL', 'redis://localhost:6379/0')
    CELERY_RESULT_BACKEND = os.getenv('CELERY_RESULT_BACKEND', 'redis://localhost:6379/0')

    # >> 경로 설정
    BASE_DIR = os.path.dirname(os.path.abspath(__file__))
    
    # >> 영상이 저장될 폴더 (다운로드 폴더)
    DOWNLOAD_DIR = os.path.join(BASE_DIR, 'data', 'raw_videos')
    
    # >> 분석 결과가 저장될 폴더
    RESULT_DIR = os.path.join(BASE_DIR, 'data', 'analyzed_json')

    # >> 전문가 데이터가 저장된 폴더
    EXPERT_DIR = os.path.join(BASE_DIR, 'data', 'expert_videos')
    
    # >> 폴더가 없으면 자동 생성
    os.makedirs(DOWNLOAD_DIR, exist_ok=True)
    os.makedirs(RESULT_DIR, exist_ok=True)
    os.makedirs(EXPERT_DIR, exist_ok=True)