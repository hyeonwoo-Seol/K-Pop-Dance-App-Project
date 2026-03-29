import json
import os
from typing import Dict, Iterable, List, Optional, Set
from urllib.parse import unquote, urlparse

import boto3
from boto3.dynamodb.conditions import Attr, Key
from botocore.exceptions import ClientError


USER_VIDEOS_TABLE = os.getenv("USER_VIDEOS_TABLE", "UserVideos")
S3_BUCKET_NAME = os.getenv("S3_BUCKET_NAME", "kpop-dance-app-data")
S3_PREFIX_TEMPLATES = [
    p.strip()
    for p in os.getenv(
        "S3_PREFIX_TEMPLATES",
        "analyzed/{user_id}/,uploads/{user_id}/,user_videos/{user_id}/,raw/{user_id}/,{user_id}_"
    ).split(",")
    if p.strip()
]
ALLOW_SCAN_FALLBACK = os.getenv("ALLOW_SCAN_FALLBACK", "false").lower() == "true"


dynamodb = boto3.resource("dynamodb")
s3 = boto3.client("s3")


def _response(status_code: int, body: Dict) -> Dict:
    return {
        "statusCode": status_code,
        "headers": {
            "Access-Control-Allow-Origin": "*",
            "Access-Control-Allow-Headers": "Content-Type,Authorization",
            "Access-Control-Allow-Methods": "OPTIONS,POST",
            "Content-Type": "application/json",
        },
        "body": json.dumps(body, ensure_ascii=False),
    }


def _parse_event_body(event: Dict) -> Dict:
    body = event.get("body")
    if body is None:
        return {}
    if isinstance(body, dict):
        return body
    try:
        return json.loads(body)
    except json.JSONDecodeError:
        return {}


def _get_http_method(event: Dict) -> Optional[str]:
    method = event.get("httpMethod")
    if method:
        return str(method).upper()

    request_context = event.get("requestContext") or {}
    http_context = request_context.get("http") or {}
    if http_context.get("method"):
        return str(http_context["method"]).upper()

    return None


def _extract_user_id(event: Dict, body: Dict) -> Optional[str]:
    if body.get("user_id"):
        return str(body["user_id"]).strip()

    if event.get("user_id"):
        return str(event["user_id"]).strip()

    path_params = event.get("pathParameters") or {}
    if path_params.get("user_id"):
        return str(path_params["user_id"]).strip()

    query_params = event.get("queryStringParameters") or {}
    if query_params.get("user_id"):
        return str(query_params["user_id"]).strip()

    return None


def _normalize_s3_key(value: Optional[str]) -> Optional[str]:
    if not value:
        return None

    text = str(value).strip()
    if not text:
        return None

    if text.startswith("http://") or text.startswith("https://"):
        parsed = urlparse(text)
        text = parsed.path.lstrip("/")

    text = text.split("?", 1)[0].strip().lstrip("/")
    if not text:
        return None

    return unquote(text)


def _query_user_items(table, user_id: str) -> List[Dict]:
    items: List[Dict] = []
    exclusive_start_key = None

    while True:
        query_params = {
            "KeyConditionExpression": Key("user_id").eq(user_id),
        }
        if exclusive_start_key:
            query_params["ExclusiveStartKey"] = exclusive_start_key

        response = table.query(**query_params)
        items.extend(response.get("Items", []))

        exclusive_start_key = response.get("LastEvaluatedKey")
        if not exclusive_start_key:
            break

    return items


def _scan_user_items(table, user_id: str) -> List[Dict]:
    items: List[Dict] = []
    exclusive_start_key = None

    while True:
        scan_params = {
            "FilterExpression": Attr("user_id").eq(user_id),
        }
        if exclusive_start_key:
            scan_params["ExclusiveStartKey"] = exclusive_start_key

        response = table.scan(**scan_params)
        items.extend(response.get("Items", []))

        exclusive_start_key = response.get("LastEvaluatedKey")
        if not exclusive_start_key:
            break

    return items


def _collect_s3_keys_from_items(items: Iterable[Dict]) -> Set[str]:
    keys: Set[str] = set()
    candidate_fields = [
        "file_key",
        "result_s3_key",
        "s3_key",
        "video_key",
        "json_key",
    ]

    for item in items:
        for field in candidate_fields:
            key = _normalize_s3_key(item.get(field))
            if key:
                keys.add(key)

    return keys


def _list_keys_by_prefix(bucket: str, prefix: str) -> Set[str]:
    if not prefix:
        return set()

    paginator = s3.get_paginator("list_objects_v2")
    keys: Set[str] = set()

    for page in paginator.paginate(Bucket=bucket, Prefix=prefix):
        for obj in page.get("Contents", []):
            key = obj.get("Key")
            if key:
                keys.add(key)

    return keys


def _chunked(items: List[str], chunk_size: int) -> Iterable[List[str]]:
    for i in range(0, len(items), chunk_size):
        yield items[i:i + chunk_size]


def _delete_s3_objects(bucket: str, keys: Set[str]) -> int:
    if not keys:
        return 0

    deleted_count = 0
    sorted_keys = sorted(keys)

    for batch in _chunked(sorted_keys, 1000):
        response = s3.delete_objects(
            Bucket=bucket,
            Delete={
                "Objects": [{"Key": key} for key in batch],
                "Quiet": True,
            },
        )
        deleted_count += len(response.get("Deleted", []))

    return deleted_count


def _delete_dynamodb_items(table, user_id: str, items: Iterable[Dict]) -> int:
    count = 0
    with table.batch_writer() as batch:
        for item in items:
            timestamp = item.get("timestamp")
            if timestamp is None:
                continue

            batch.delete_item(
                Key={
                    "user_id": str(user_id),
                    "timestamp": int(timestamp),
                }
            )
            count += 1
    return count


def lambda_handler(event, context):
    if not isinstance(event, dict):
        return _response(400, {"message": "invalid event payload"})

    method = _get_http_method(event)
    if method == "OPTIONS":
        return _response(200, {"message": "ok"})

    body = _parse_event_body(event)
    user_id = _extract_user_id(event, body)
    if not user_id:
        return _response(400, {"message": "user_id is required"})

    table = dynamodb.Table(USER_VIDEOS_TABLE)

    try:
        try:
            user_items = _query_user_items(table, user_id)
        except ClientError as query_error:
            if not ALLOW_SCAN_FALLBACK:
                raise query_error
            user_items = _scan_user_items(table, user_id)

        s3_keys = _collect_s3_keys_from_items(user_items)
        for template in S3_PREFIX_TEMPLATES:
            prefix = template.format(user_id=user_id).strip()
            if prefix:
                s3_keys.update(_list_keys_by_prefix(S3_BUCKET_NAME, prefix))

        deleted_s3_objects = _delete_s3_objects(S3_BUCKET_NAME, s3_keys)
        deleted_dynamodb_items = _delete_dynamodb_items(table, user_id, user_items)

        return _response(
            200,
            {
                "message": "User data deleted successfully",
                "user_id": user_id,
                "deleted_s3_objects": deleted_s3_objects,
                "deleted_dynamodb_items": deleted_dynamodb_items,
            },
        )
    except Exception as e:
        return _response(
            500,
            {
                "message": "Failed to delete user data",
                "error": str(e),
                "user_id": user_id,
            },
        )
