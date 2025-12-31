# >> celery_app.py
# >> 비동기 작업 큐 시스템의 인스턴스를 생성하고, Broker와 Backend를 설정한다.
# >> 작업 메시지를 중개할 Redis Broker와 작업 결과를 저장할 Redis Backend를 연결한다.
# >> 작업 데이터의 직렬화 포멧을 json으로 강제하여 시스템 간 데이터 호환성을 보장한다.
# >> include=['task'] 설정을 통해 실행 가능한 작업 모듈을 등록한다.

from celery import Celery
from config import Config

# Celery 인스턴스 생성
app = Celery(
    'dance_ai_worker',
    broker=Config.CELERY_BROKER_URL,
    backend=Config.CELERY_RESULT_BACKEND,
    include=['tasks']  # tasks.py 모듈을 로드
)

# Celery 추가 설정
# >> task_routes 설정을 추가하여 작업별로 전송될 큐를 분리한다.
# >> 이를 통해 I/O 작업과 GPU 작업을 서로 다른 워커 프로세스가 처리하도록 유도한다.
app.conf.update(
    task_serializer='json',
    accept_content=['json'],
    result_serializer='json',
    timezone='Asia/Seoul',
    enable_utc=True,
    task_default_queue='default',
    task_routes={
        'tasks.download_video_task': {'queue': 'io_queue'},
        'tasks.pose_estimation_task': {'queue': 'gpu_queue'},
    }
)

if __name__ == '__main__':
    app.start()
