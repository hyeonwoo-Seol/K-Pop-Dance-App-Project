# 논리적 흐름

1. 사용자가 영상을 업로드하면 AWS S3 이벤트가 SQS에 메시지를 발행한다.

2. sqs_bridge.py가 메시지를 수신한다.

3. sqs_bridge.py가 download -> pose_estimation 순서의 작업 체인을 생성하여 Redis Broker에 등록한다.

4. celery_app.py와 tasks.py를 통해, 대기 중인 Celery worker가 작업을 가져간다.

5. pose_estimation.py는 영상을 로컬로 다운로드하고, YOLO11l-pose.pt 모델을 통해 스켈레톤 데이터 추출 후 정규화하여 JSON으로 저장한다.