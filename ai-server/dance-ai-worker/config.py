import os
from dotenv import load_dotenv

# .env 파일 로드
load_dotenv()

class Config:
    # ==========================================
    # [스위치] AWS 연결 여부 (True: 실제 연결, False: 테스트 모드)
    # 팀원이 AWS를 구축하기 전까지는 False로 두고 개발하시면 됩니다.
    # ==========================================
    USE_AWS = os.getenv('USE_AWS', 'False').lower() == 'true'

    # 1. AWS 설정 (IAM 사용자 키)
    # USE_AWS가 True일 때만 실제 값이 필요합니다.
    AWS_ACCESS_KEY_ID = os.getenv('AWS_ACCESS_KEY_ID')
    AWS_SECRET_ACCESS_KEY = os.getenv('AWS_SECRET_ACCESS_KEY')
    AWS_REGION = os.getenv('AWS_REGION', 'ap-northeast-2')

    # 2. SQS 설정
    SQS_QUEUE_URL = os.getenv('SQS_QUEUE_URL')

    # 3. Redis & Celery 설정
    CELERY_BROKER_URL = os.getenv('CELERY_BROKER_URL', 'redis://localhost:6379/0')
    CELERY_RESULT_BACKEND = os.getenv('CELERY_RESULT_BACKEND', 'redis://localhost:6379/0')

    # 4. 경로 설정
    BASE_DIR = os.path.dirname(os.path.abspath(__file__))
    
    # 영상이 저장될 폴더 (다운로드 폴더)
    DOWNLOAD_DIR = os.path.join(BASE_DIR, 'data', 'raw_videos')
    
    # 분석 결과가 저장될 폴더
    RESULT_DIR = os.path.join(BASE_DIR, 'data', 'analyzed_json')
    
    # 폴더가 없으면 자동 생성
    os.makedirs(DOWNLOAD_DIR, exist_ok=True)
    os.makedirs(RESULT_DIR, exist_ok=True)