# .env 파일 생성해서 AWS 키 보관하기

`emacs .env` 를 통해 .env 파일을 생성한다.

아래 내용에 사용하고자 할 AWS 정보를 입력한다.

```

# AWS IAM 사용자 키 (S3 및 SQS 권한 필수)
AWS_ACCESS_KEY_ID=AKIAxxxxxxxxxxxxxxxxx
AWS_SECRET_ACCESS_KEY=xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
AWS_REGION=ap-northeast-2

# SQS 대기열 URL (AWS 콘솔에서 복사)
SQS_QUEUE_URL=[https://sqs.ap-northeast-2.amazonaws.com/123456789012/dance-ai-queue](https://sqs.ap-northeast-2.amazonaws.com/123456789012/dance-ai-queue)

# Redis 설정
CELERY_BROKER_URL=redis://localhost:6379/0
CELERY_RESULT_BACKEND=redis://localhost:6379/0

```

# Celery 워커 실행

Worker를 Conda 환경에서 실행한다.

`celery -A celery_app worker --loglevel=info -P solo`

# 새 터미널을 열고 SQS Bridge를 실행한다.

`python sqs_bridge.py`

# 테스트한다.

이것만 성공하면, 추후 팀원이 AWS를 구축했을 때 .env 설정 한 줄만 바꾸면 바로 연동이 끝난다.

AWS가 구축되어 .env 설정을 바꾼 뒤, AWS 콘솔에서 SQS에 들어간 뒤 대기열에 접속한다.

메시지 전송 및 수신 버튼을 클릭한다.

메시지 본문에 아래 JSON을 입력하고 전송한다.

```

{
  "Records": [
    {
      "s3": {
        "bucket": { "name": "dance-ai-raw-video" },
        "object": { "key": "test_user/practice_1.mp4" }
      }
    }
  ]
}

```

그리고 터미널을 확인한다. Bridge 쪽은 [SQS] 메시지 수신, [Celery] 작업 전달 완료 가 표시되어야 하고, Celery 쪽은 Task ... received, [Celery] 다운로드 완료 가 표시되어야 한다.