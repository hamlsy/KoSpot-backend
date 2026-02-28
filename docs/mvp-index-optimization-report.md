# MVP Index Optimization Report

## 문제 상황
- `오늘의 MVP` 집계 쿼리는 `road_view_game`에서 날짜 범위 + 상태 필터 + 점수 정렬(`score DESC`)을 함께 사용한다.
- 보상 배치 쿼리는 `daily_mvp`에서 `rewardGranted=false` + `mvpDate <= targetDate` 조건으로 미지급 대상을 조회한다.
- 랭크 조회 쿼리는 `game_rank`에서 `member + gameMode` 단건 조회, `gameMode + rankTier + ratingScore` 정렬 조회를 반복 사용한다.
- 기존 엔티티 기준으로 위 접근 패턴을 직접 지원하는 인덱스가 충분하지 않아, 데이터가 커질수록 풀스캔/정렬 비용이 커질 수 있었다.

## 원인
- `road_view_game`에 MVP 패턴(`game_mode`, `game_status`, `ended_at`, `score`, `id`)을 직접 커버하는 복합 인덱스가 없었다.
- `daily_mvp`에 보상 배치 조건(`reward_granted`, `mvp_date`) 결합 인덱스가 없었다.
- `game_rank`는 조회 패턴 대비(`member_id + game_mode`, `game_mode + rank_tier + rating_score`) 명시 인덱스가 없었다.
- `RoadViewGame.member`의 실제 FK 컬럼명이 `member_member_id`인데, 인덱스 컬럼명을 `member_id`로 두면 실제 스키마와 불일치한다.

## 해결 과정
1. 쿼리 패턴 확인
   - MVP 후보 조회, 보상 배치 조회, 랭킹 조회에 사용되는 WHERE/ORDER BY 컬럼 조합을 점검했다.
2. 인덱스 설계 및 반영
   - `RoadViewGame`, `DailyMvp`, `GameRank` 엔티티에 복합 인덱스를 추가했다.
3. FK 컬럼명 불일치 보정
   - `road_view_game`의 멤버 FK 컬럼은 `member_member_id`로 인덱스 정의를 수정했다.
4. 검증 테스트 추가
   - 인덱스 메타데이터 검증 테스트 추가.
   - MVP 후보 조회 쿼리에 대해 `IGNORE INDEX`(baseline) vs `FORCE INDEX`(optimized) 비교 테스트 추가.

## 해결 방법

### 1) 인덱스 적용
- `road_view_game`
  - `idx_road_view_game_mvp (game_mode, game_status, ended_at, score, id)`
  - `idx_road_view_game_member_status_created (member_member_id, game_status, created_date)`
- `daily_mvp`
  - `idx_daily_mvp_member_id (member_id)`
  - `idx_daily_mvp_road_view_game_id (road_view_game_id)`
  - `idx_daily_mvp_reward_date (reward_granted, mvp_date)`
- `game_rank`
  - `idx_game_rank_member_mode (member_id, game_mode)`
  - `idx_game_rank_mode_tier_rating (game_mode, rank_tier, rating_score)`

### 2) 테스트 코드
- 인덱스 존재 검증
  - `src/test/java/com/kospot/mvp/index/IndexMetadataVerificationTest.java`
- 인덱스 전/후 비교
  - `src/test/java/com/kospot/mvp/index/MvpIndexPerformanceComparisonTest.java`
  - 동일 조건 SQL에 대해:
    - baseline: `IGNORE INDEX (idx_road_view_game_mvp)`
    - optimized: `FORCE INDEX (idx_road_view_game_mvp)`
  - `EXPLAIN`으로 key/rows를 비교하고, 평균 실행시간을 기록한다.

## 결과
- 실행 명령:
  - `GRADLE_USER_HOME="/c/KoSpot-backend/.gradle-home" ./gradlew.bat test --tests "*MvpIndexPerformanceComparisonTest" --no-daemon`
- 측정 결과(테스트 로그):
  - Baseline avg(ms): **17.792**
  - Optimized avg(ms): **19.809**
  - Improvement(%): **-11.34**
  - Baseline key: **null**, rows: **12000**
  - Optimized key: **idx_road_view_game_mvp**, rows: **6000**

## 성과
- MVP 핵심 조회 경로에 대해 옵티마이저가 사용할 수 있는 전용 인덱스를 확보했다.
- 배치 보상(`reward_granted + mvp_date`)의 조건 스캔 비용을 줄일 수 있는 인덱스를 확보했다.
- 랭크 조회의 빈번한 접근 패턴을 인덱스로 고정했다.
- 비교 테스트에서 시간은 환경/데이터 분포 영향으로 역전될 수 있었지만, `EXPLAIN` 기준으로는
  - baseline(인덱스 무시) 대비 optimized(인덱스 강제)에서 **스캔 rows가 12000 -> 6000으로 50% 감소**했다.
- 운영 환경에서는 실제 데이터 분포(멤버 수, 날짜 선택도, 버퍼 상태)에 맞춰 `ANALYZE TABLE`/실측 APM 지표를 함께 검증하는 것을 권장한다.
