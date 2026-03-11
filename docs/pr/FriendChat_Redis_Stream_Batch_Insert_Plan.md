# 채팅 메시지 DB 부하 완화 구현 계획서

## Issue
- 제목: 채팅 메시지 DB 부하 완화 — Redis Stream 기반 비동기 Batch Insert 구조 도입 (Spring 내부 Worker)
- 범위: 친구 채팅(friend) 메시지 저장 경로

## 목표
- API 요청 경로에서 메시지 즉시 DB insert를 제거해 write QPS를 완화한다.
- Redis Stream을 버퍼로 사용하고 Spring 내부 Worker가 배치 저장한다.
- 저장 성공 시에만 ACK하여 at-least-once 처리 특성을 유지한다.
- DB `UNIQUE(message_id)` 기반 멱등성으로 중복 저장을 무해화한다.

## 아키텍처

### Write Path (API Thread)
1. 친구 채팅 메시지 요청 수신
2. `chat:stream`에 `XADD`
3. Redis 적재 성공 시 API 성공 응답
4. Redis 장애 시 `503` 응답

### Flush Path (Spring Internal Worker)
1. `XREADGROUP`(BLOCK + COUNT)로 스트림 소비
2. 버퍼가 배치 조건(`batch-size`/`flush-interval-ms`)을 만족하면 JDBC batch insert 수행
3. DB 저장 성공 건 `XACK`
4. 실패 건은 미ACK 상태로 재처리 대상 유지
5. 반복 실패 초과 건은 `chat:stream:dlq`로 이동 후 ACK

## 처리 보장
- Exactly Once 미보장
- At-least-once + DB 멱등 처리
- 장애 시 재처리 우선, 반복 실패는 DLQ 격리

## Redis 설계
- Main Stream: `chat:stream`
- Consumer Group: `chat-persist-group`
- DLQ Stream: `chat:stream:dlq`
- Consumer Name: `${spring.application.name}-${HOSTNAME}` 기반

## Payload 설계
- `message_id`
- `room_id`
- `sender_member_id`
- `content`
- `created_at`
- `retry_count`

## DB 저장 전략
- 대상 테이블: `friend_chat_message`
- 저장 방식: JDBC Batch Insert
- 멱등 SQL (MySQL):

```sql
INSERT INTO friend_chat_message (
  message_id,
  room_id,
  sender_member_id,
  content,
  created_date,
  last_modified_date
) VALUES (?, ?, ?, ?, ?, ?)
ON DUPLICATE KEY UPDATE message_id = message_id;
```

## 실패 처리 정책
- DB 일시 실패: 미ACK 유지(재처리)
- 파싱/유효성 실패: DLQ 이동 후 ACK
- 반복 실패(`max-retry` 초과): DLQ 이동 후 ACK
- DLQ 적재 실패: ACK 하지 않고 pending 유지

## Graceful Shutdown
- worker 루프 중단
- 버퍼 flush 시도
- `shutdown-timeout-ms` 내 종료 대기
- 초과 시 강제 종료

## 설정값

```yaml
chat:
  persist:
    enabled: true
    friend-only: true
    stream-key: chat:stream
    dlq-stream-key: chat:stream:dlq
    group: chat-persist-group
    consumer-name: kospot-backend-local
    batch-size: 500
    read-count: 500
    read-block-ms: 2000
    flush-interval-ms: 1000
    max-retry: 5
    shutdown-timeout-ms: 10000
```

## 모니터링 항목
- Stream backlog 길이
- Pending entries 수
- Batch 처리량(TPS)
- Batch 실패율
- DLQ 증가량

## 구현 항목 체크리스트
- [x] Redis Stream/Consumer Group 초기화
- [x] friend 즉시 DB insert 제거
- [x] API -> XADD 구현
- [x] 내부 Worker(XREADGROUP + Batch Insert)
- [x] 성공 건 XACK
- [x] 실패 재처리 및 DLQ
- [x] Graceful shutdown
- [ ] 부하 테스트 및 기존 대비 DB write QPS 비교

## 롤아웃 절차
1. `chat.persist.enabled=false`로 배포 후 스트림 생성 확인
2. `enabled=true`로 전환 후 저트래픽 검증
3. 배치 파라미터 튜닝
4. DLQ/지표 기반 운영 임계치 확정

## 비고
- 현재 단일 인스턴스 내부 Worker 기준 구현
- 추후 Worker 분리 인스턴스로 확장 가능하도록 stream/group 구조 유지
