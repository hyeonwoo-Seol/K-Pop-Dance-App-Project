# withdraw_user_data Lambda 배포 가이드

`탈퇴 버튼 -> AWS 사용자 데이터 삭제`를 위해 필요한 최소 구성입니다.

## 1) Lambda 생성

- 런타임: `Python 3.12` (또는 3.11)
- 핸들러: `lambda_function.lambda_handler`
- 코드: 같은 폴더의 `lambda_function.py` 업로드

## 2) Lambda 환경 변수

- `USER_VIDEOS_TABLE=UserVideos`
- `S3_BUCKET_NAME=kpop-dance-app-data`
- `S3_PREFIX_TEMPLATES=analyzed/{user_id}/,uploads/{user_id}/,user_videos/{user_id}/,raw/{user_id}/,{user_id}_`
- `ALLOW_SCAN_FALLBACK=false`

`S3_PREFIX_TEMPLATES`는 현재 버킷 구조에 맞게 수정하세요.

## 3) Lambda IAM 권한

Lambda 실행 역할에 아래 권한이 필요합니다.

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": [
        "dynamodb:Query",
        "dynamodb:Scan",
        "dynamodb:DeleteItem",
        "dynamodb:BatchWriteItem"
      ],
      "Resource": [
        "arn:aws:dynamodb:ap-northeast-1:<ACCOUNT_ID>:table/UserVideos"
      ]
    },
    {
      "Effect": "Allow",
      "Action": [
        "s3:ListBucket"
      ],
      "Resource": "arn:aws:s3:::kpop-dance-app-data"
    },
    {
      "Effect": "Allow",
      "Action": [
        "s3:DeleteObject"
      ],
      "Resource": "arn:aws:s3:::kpop-dance-app-data/*"
    }
  ]
}
```

리전/계정/리소스명은 실제 값으로 변경하세요.

## 4) API Gateway 연결

- 기존 API Gateway(`https://aujfpfdg6e.execute-api.ap-northeast-1.amazonaws.com`)에
- `POST /default/withdrawUserData` 라우트를 만들고
- 위 Lambda로 통합(Proxy Integration)합니다.
- CORS 허용:
  - `OPTIONS,POST`
  - `Content-Type,Authorization`

## 5) 테스트 이벤트 예시

Lambda 콘솔 `Test`에서는 아래 둘 중 아무거나 사용 가능합니다.

### 5-1. 가장 단순한 Lambda Test 이벤트

```json
{
  "user_id": "test_user_123"
}
```

### 5-2. API Gateway 프록시 형태로 테스트

```json
{
  "httpMethod": "POST",
  "body": "{\"user_id\":\"test_user_123\"}"
}
```

### 5-3. Invoke API(HTTP 호출)일 때

`body`는 아래처럼 **이 JSON 자체**를 보내야 합니다.

```json
{
  "user_id": "test_user_123"
}
```

성공 시 응답 예시:

```json
{
  "message": "User data deleted successfully",
  "user_id": "test_user_123",
  "deleted_s3_objects": 12,
  "deleted_dynamodb_items": 5
}
```

## 6) 앱 연동 경로

앱에서 이미 `POST default/withdrawUserData` 호출하도록 반영되었습니다.

- 호출 위치: `kpop-android-app/app/src/main/java/com/example/kpopdancepracticeai/ui/AppNavigation.kt`
- API 인터페이스: `kpop-android-app/app/src/main/java/com/example/kpopdancepracticeai/data/api/UploadApiService.kt`
