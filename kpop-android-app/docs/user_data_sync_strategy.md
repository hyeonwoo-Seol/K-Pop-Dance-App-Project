# 사용자 최신 데이터 동기화 전략 (설정 > 최신 데이터 동기화)

## 목표
- 설정 화면의 **"최신 데이터 동기화"** 버튼 1회로, 사용자 관련 데이터를
  - 앱 내부(Room)
  - AWS(DynamoDB/API)
  에서 일관되게 반영한다.
- 중복 데이터 업데이트 비용을 줄이기 위해 **원본(Source of Truth)** 을 분리한다.
- 정확도 그래프를 위해 **날짜별 + 노래 파트별** 시계열을 유지한다.

---

## 1) 데이터 소유권(중복 데이터 해결)

중복 필드가 많은 구조에서 성능 저하를 막기 위해, 다음처럼 소유권을 명확히 둔다.

### A. 정적 마스터 데이터(앱/서버 공통)
- `Achievements.title/description/goal_count/reward_type/reward_id`
- `Badges.name/description/iconResName/category`
- `LightSticks.name/artist`
- `Songs`, `SongParts`

**정책**
- 마스터는 서버(또는 앱 번들 seed)가 원본.
- 동기화 시점에 upsert 하되, "사용자 상태 필드"는 덮어쓰지 않는다.

### B. 사용자 상태 데이터(동적)
- 업적 진행: `current_count`, `is_unlocked`, `achieved_at`
- 보유 아이템: `is_owned`, `obtained_at`, 배지 해금/선택 상태
- 통계: `UserStatistics`(누적 시간, 평균 정확도, 레벨/경험치 등)
- 분석 결과: `PracticeResults`

**정책**
- **서버 우선 + 로컬 미전송 변경 병합(양방향)** 권장.
- 충돌 시 `updated_at` 기준 Last-Write-Wins, 단 누적값은 규칙 기반 병합:
  - 카운트성(`completed_parts`, `badge_count` 등): `max(local, remote)`
  - 누적시간: 서버 기준 + 로컬 미동기화 delta 반영
  - 평균값: 원시 이벤트(`PracticeResults`) 재집계 가능하면 재집계 우선

---

## 2) 정확도 데이터 모델 (그래프 대응)

현재 `PracticeResults`는 1회 연습 단위 기록을 보관하므로, 그래프용으로 충분히 확장 가능.
다만 조회 성능을 위해 일별 집계 테이블을 추가 권장.

### 신규 권장 테이블: `AccuracyDaily`
- `user_uuid` (PK 일부)
- `date` (PK 일부, yyyy-MM-dd)
- `song_id` (PK 일부)
- `part_number` (PK 일부)
- `attempt_count` (Int)
- `avg_total_score` (Double)
- `avg_accuracy_json` (JSON: 신체부위 평균)
- `best_score` (Int)
- `updated_at` (Long)

**적재 방식**
1. `PracticeResults` upsert
2. 해당 row 기준으로 `AccuracyDaily` 증분 집계

이렇게 하면 차트 화면은 `AccuracyDaily` 조회만으로 빠르게 렌더링 가능.

---

## 3) 동기화 파이프라인 (버튼 클릭 시)

아래 순서를 권장:

1. **로컬 플러시**
   - 앱 사용시간 등 메모리 누적치 `syncAppUsageTime(userId)` 반영
2. **Pull (서버 → 로컬)**
   - User profile / UserStatistics
   - Achievement progress
   - Badge collection / LightStick collection
   - PracticeResults(증분: since=last_sync_at)
3. **로컬 병합 트랜잭션**
   - 엔티티별 upsert + 병합 규칙 적용
   - `AccuracyDaily` 재계산 또는 증분 갱신
4. **Push (로컬 미전송 → 서버)**
   - 오프라인 중 생성된 연습 결과, 업적 진행, 선택 배지 등
5. **서버 응답 재반영(최종 정합화)**
6. **last_sync_at 갱신**

> 핵심: Pull만 하면 "내 기기 오프라인 작업"이 유실될 수 있으므로 Push 단계가 필요.

---

## 4) API/Dynamo 매핑 권장

### Achievements
- Dynamo: `PK=user_uuid, SK=achievementId`
- 속성: `{ cur, unlocked, at, updated_at }`

### Collections(배지/응원봉)
- Dynamo 한 아이템에 String Set으로 관리 가능하지만,
  향후 메타(획득일, 출처) 필요 시 항목 단위 레코드 분리 권장:
  - `PK=user_uuid, SK=BADGE#badge_id`
  - `PK=user_uuid, SK=LIGHTSTICK#stick_id`

### PracticeResults
- 조회량이 많으면 Dynamo 단독보다
  API 레이어에서 기간/페이지네이션 제공 권장 (`since`, `limit`, `nextToken`).

---

## 5) Room 구현 체크리스트

- [ ] `last_sync_at` 저장(DataStore 또는 별도 SyncState 테이블)
- [ ] 엔티티별 `updated_at` 추가(없으면 추가)
- [ ] 로컬 변경 추적 필드 `is_dirty`, `sync_state`(PENDING/SYNCED/FAILED)
- [ ] 동기화 전 과정 `@Transaction` 처리
- [ ] 대용량 `PracticeResults`는 페이징/배치 upsert
- [ ] 동기화 완료 후 ViewModel Flow 자동 갱신 (현재 구조 유지 가능)

---

## 6) 현재 코드 기준 적용 포인트

- `MainViewModel.loadInitialData()`의 "최신 데이터 동기화" 진입점은 이미 존재.
- `AppRepository.fetchInitialData()`는 현재 로컬 초기화 중심이므로,
  실제 서버 Pull/Push를 담당할 `syncLatestUserData(userId)` 메서드로 분리 권장.
- `ApiService`는 현재 stats/history만 정의되어 있어,
  achievements/collections/profile endpoint 추가 필요.

---

## 7) 실패/재시도 정책

- 네트워크 실패 시: 로컬 데이터는 유지하고 `sync_state=FAILED` 표기
- 다음 동기화 시 FAILED 우선 재전송
- 사용자 메시지:
  - 부분 성공: "일부 데이터만 동기화됨"
  - 완전 성공: "최신 데이터 동기화 완료"

---

## 8) 권장 최소 구현 단계 (MVP)

1. Pull 전용 동기화(프로필/통계/업적/컬렉션/최근 결과)
2. `PracticeResults` 증분 수신(`since`) 적용
3. `AccuracyDaily` 집계 추가
4. Push(오프라인 로컬 변경 반영) 추가

이 순서로 가면 기능 가시성이 빠르고, 이후 정합성도 점진적으로 강화 가능.
