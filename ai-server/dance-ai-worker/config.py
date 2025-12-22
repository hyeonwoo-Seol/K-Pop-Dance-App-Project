# >> AWS 키, SQS 주소, Redis 설정 등 관리한다.

import os
from dotenv import load_dotenv

# .env 파일 로드 (환경 변수 관리)
load_dotenv()

class Config:
    # 1. AWS 설정 (IAM 사용자 키)
    AWS_ACCESS_KEY_ID = os.getenv('AWS_ACCESS_KEY_ID')
    AWS_SECRET_ACCESS_KEY = os.getenv('AWS_SECRET_ACCESS_KEY')
    AWS_REGION = os.getenv('AWS_REGION', 'ap-northeast-2') # 기본값: 서울

    # 2. SQS 설정
    # SQS 대기열 URL (AWS 콘솔에서 복사한 URL을 .env에 넣어야 함)
    SQS_QUEUE_URL = os.getenv('SQS_QUEUE_URL')

    # 3. Redis & Celery 설정
    # 로컬 Redis 주소
    CELERY_BROKER_URL = os.getenv('CELERY_BROKER_URL', 'redis://localhost:6379/0')
    CELERY_RESULT_BACKEND = os.getenv('CELERY_RESULT_BACKEND', 'redis://localhost:6379/0')

    # 4. 경로 설정 (영상 저장 위치)
    BASE_DIR = os.path.dirname(os.path.abspath(__file__))
    DOWNLOAD_DIR = os.path.join(BASE_DIR, 'data', 'raw_videos')
    
    # 폴더가 없으면 생성
    os.makedirs(DOWNLOAD_DIR, exist_ok=True)
