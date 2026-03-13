# MVP 조회 개선 구현 계획

## 목표
- 오늘 MVP 조회 시 어제 MVP를 함께 제공한다.
- MVP 조회 경로를 `Redis 우선 -> DB fallback`으로 통일한다.
- 정각 스케줄러(집계/반영) 정책은 변경하지 않는다.

## 요구사항 해석
1. **어제 MVP 동시 조회**
   - 사용자의 "오늘 MVP" 화면에 어제 MVP도 같이 노출할 수 있어야 한다.
   - 기존 단일 날짜 조회 기능은 유지하되, 오늘 조회 UX를 위한 확장 응답을 추가하는 방향이 안전하다.

2. **조회 소스 우선순위**
   - 1순위: Redis 캐시
   - 2순위: DB (`daily_mvp`)
   - DB fallback 성공 시 Redis write-through(캐시 갱신) 수행

3. **flush/scheduler 정책 유지**
   - 정각 스케줄러/현재 크론/락 전략은 변경하지 않는다.
   - 조회 개선은 read path 중심으로만 반영한다.

## 설계 원칙
- **하위 호환성 우선**: 기존 `/mvps/daily` 응답 계약을 깨지 않는다.
- **읽기 책임 분리**: "단일 날짜 조회"와 "오늘+어제 조회"를 use case 레벨에서 분리한다.
- **중복 최소화**: 기존 `GetDailyMvpUseCase`의 캐시-DB 조회 흐름을 재사용 가능한 메서드로 추출한다.
- **운영 안정성**: 스케줄러, 집계 쓰기 로직, 보상 로직은 수정하지 않는다.

## API/응답 계약 제안
### 1) 기존 API 유지
- `GET /mvps/daily?date=YYYY-MM-DD`
  - 기존처럼 특정 날짜 단건 조회.
  - read-through(캐시 miss 시 DB 조회 후 캐시 반영)만 명확히 보장.

### 2) 신규 API 추가 (권장)
- `GET /mvps/daily/today-with-yesterday`
  - 응답 예시:
  - `today`: 오늘 MVP (없으면 `null`)
  - `yesterday`: 어제 MVP (없으면 `null`)

> 기존 `/mvps/daily` 응답 타입을 직접 변경하면 클라이언트 호환성 리스크가 커서 신규 엔드포인트 추가가 안전하다.

## 도메인/애플리케이션 변경 계획
### A. Response DTO 추가
- 파일: `src/main/java/com/kospot/mvp/presentation/response/DailyMvpResponse.java`
- `DailyWithYesterday`(가칭) DTO 추가
  - 필드: `Daily today`, `Daily yesterday`

### B. UseCase 추가
- 파일: `src/main/java/com/kospot/mvp/application/usecase/`
- `GetTodayAndYesterdayMvpUseCase` 신설
  - 내부 로직:
    1. KST 기준 `today`, `yesterday` 계산
    2. 기존 단일 조회 로직(캐시 우선)을 오늘/어제 각각 호출
    3. 묶어서 반환

### C. 기존 단일 조회 로직 정리
- 파일: `src/main/java/com/kospot/mvp/application/usecase/GetDailyMvpUseCase.java`
- 단일 날짜 조회 흐름을 명확히 정리
  - cache hit 반환
  - cache miss -> DB 조회
  - DB hit -> 캐시 저장 후 반환
  - DB miss -> none cache 저장 후 `null`
- 오늘/어제 번들 조회에서 재사용 가능하도록 메서드 가시성/구조 조정

### D. Controller 확장
- 파일: `src/main/java/com/kospot/mvp/presentation/controller/DailyMvpController.java`
- 신규 엔드포인트 추가
  - `GET /mvps/daily/today-with-yesterday`
  - `ApiResponseDto<DailyMvpResponse.DailyWithYesterday>` 반환

## 캐시 정책
- 기존 키/TTL 정책 유지
  - `mvp:daily:{date}:v1`
  - `mvp:daily:none:{date}:v1`
- 오늘/어제 동시 조회 시에도 각 날짜를 동일 정책으로 독립 처리
- 스케줄러 flush 정책 및 주기 변경 없음

## 테스트 계획
### 단위 테스트
1. 오늘+어제 모두 캐시 hit
2. 오늘 hit + 어제 DB fallback
3. 오늘/어제 모두 miss -> none cache 저장
4. 한쪽만 존재할 때 null-safe 응답 검증

### 통합 테스트
1. 신규 API 응답 스키마 검증
2. Redis miss -> DB hit -> Redis write-through 검증
3. 기존 `/mvps/daily` 회귀 테스트(응답/동작 불변)

### 회귀 체크
- 스케줄러 크론/락/집계 경로가 기존과 동일하게 동작하는지 로그 기반 확인

## 배포/릴리즈 전략
1. 코드 배포
2. 신규 엔드포인트를 클라이언트에서 점진 적용
3. 모니터링 지표 확인
   - `/mvps/daily/today-with-yesterday` 호출량
   - 캐시 hit ratio
   - DB fallback 비율

## 리스크와 대응
- **리스크: 응답 계약 변경으로 인한 클라이언트 장애**
  - 대응: 기존 API 유지 + 신규 API 추가
- **리스크: 오늘/어제 2건 조회로 인한 read 비용 증가**
  - 대응: Redis 우선 조회로 비용 상쇄, miss 비율 모니터링
- **리스크: 날짜 경계(KST) 오류**
  - 대응: KST 고정 유틸/공통 메서드 사용 및 경계 시간 테스트 추가

## 구현 순서(작업 단위)
1. DTO 추가 (`DailyWithYesterday`)
2. 신규 UseCase 구현 (`today + yesterday`)
3. Controller 신규 API 추가
4. 기존 UseCase 리팩터링(재사용 가능한 read-through 정리)
5. 단위/통합 테스트 작성
6. 회귀 검증 및 운영 확인 가이드 작성
