# >> Celery 애플리케이션 객체를 초기화하는 파일이다.

from celery import Celery
from config import Config

# Celery 인스턴스 생성
# 이름: 'dance_ai_worker'
# broker: 일감을 받는 우체통 (Redis)
# backend: 처리 결과를 저장하는 곳 (Redis)
app = Celery(
    'dance_ai_worker',
    broker=Config.CELERY_BROKER_URL,
    backend=Config.CELERY_RESULT_BACKEND,
    include=['tasks']  # tasks.py 모듈을 로드
)

# Celery 추가 설정
app.conf.update(
    task_serializer='json',
    accept_content=['json'],
    result_serializer='json',
    timezone='Asia/Seoul',
    enable_utc=True,
)

if __name__ == '__main__':
    app.start()
