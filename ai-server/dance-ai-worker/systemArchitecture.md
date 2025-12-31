# 전체적인 흐름

## 1. 서버 시작 및 초기화
---

가장 먼저 redis-sever가 켜져 있는 상태에서 2개의 터미널을 통해 Worker와 Bridge를 실행한다.

`celery -A celery_app worker ...` 명령어를 사용하여 celery worker를 시작한다.

celery_app.py가 실행되면서 Redis 브로커와 연결된다. tasks.py의 init_worker 함수가 실행된다. 이 때 YOLO 모델이 GPU의 메모리에 Load 된다. 모델 로딩이 끝나면 Worker는 작업 대기 상태로 Redis 큐를 감시한다.

`python sqs_bridge.py` 명령어를 사용하여 SQS Bridge를 시작한다.

config.py의 설정을 읽어서 AWS SQS 클라이언트를 생성한다. while True 무한 루프에 진입하여 SQS 대기열을 감시한다.

## 2. 요청 감지
---

사용자가 앱에서 영상을 업로드하면, AWS Lambda가 SQS 메시지를 넣는다. sqs_bridge.py는 `sqs.receive_message`를 통해 20초 간격으로 SQS를 확인하고, 메시지가 들어오면 이를 가져온다.

가져오는 메시지 내용은 아래와 같다.

```

{
  "bucket_name": "...",
  "file_key": "raw/user/video.mp4",
  "song_id": "song_001",
  "user_id": "user_123",
  "video_id": "vid_001"
}

```

sqs_bridge.py는 메시지에서 데이터를 파싱한다. 그리고 순서가 있는 작업 체인을 만든다. 작업 체인은 "다운로드 -> 분석" 순서이다.

`workflow.delay()`를 호출하면 이 요청은 Redis 브로커로 전송된다. 그 다음에 Bridge는 SQS에서 해당 메시지를 삭제하고 다시 대기 모드로 돌아간다.

## 3. 작업 수행
---

celery worker (tasks.py)는 redis에서 작업을 가져와 Task1과 Task2를 수행한다.

### Task1

Task1에서, Worker가 S3 버킷에서 영상을 다운로드해서 `data/raw_videos/`에 저장한다. 다운로드가 완료되면 저장된 로컬 파일 경로를 다음으로 넘긴다.

### Task2

Task는, Task1에서 받은 영상 경로, song_id, user_id 등을 입력으로 사용해서 Pose Estimation을 수행한다. Pose Estimator는 영상을 프레임 단위로 분석하여 temp_json을 생성한다.

전달 받은 song_id를 이용해 `data/expert_video/song_id.json` 을 불러온다. 그리고 사용자 데이터와 전문가 데이터를 DTW 알고리즘으로 비교하여 점수와 오차 구간을 계산한다.

그 다음에 점수, 등급, 피드백이 포함된 최종 JSON 파일을 `data/analyzed_json/`에 저장한다.

## 4. 완료
---

생성된 최종 JSON 파일을 S3 버킷에 업로드한다.

그리고 requests.post를 사용해서 AWS API Gateway로 분석 완료 신호를 보낸다.

그 뒤에 worker는 다시 대기 상태로 돌아가 다음 요청을 기다린다.