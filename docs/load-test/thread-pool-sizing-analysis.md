# WebSocket Thread Pool 크기 계산 근거

## 1. 핵심 공식: Little's Law

```
필요 스레드 수 = 동시 요청 수 × 요청당 처리 시간
             = (요청 도착률) × (평균 응답 시간)
```

---

## 2. 테스트 데이터 기반 계산

### 측정된 값 (k6-results.json 기준)

| 지표                    | 값         | 출처                      |
| ----------------------- | ---------- | ------------------------- |
| 최대 VUs (동시 연결)    | 400        | `vus_max.max = 400`       |
| WS 연결 시간 (avg)      | 162ms      | `ws_connecting.avg`       |
| WS 연결 시간 (p95)      | 877ms      | `ws_connecting.p(95)`     |
| WS 세션 지속 시간 (avg) | 1,609ms    | `ws_session_duration.avg` |
| 메시지 송신율           | 76.3 msg/s | `ws_msgs_sent.rate`       |
| 메시지 수신율           | 76.3 msg/s | `ws_msgs_received.rate`   |

### 계산: Outbound Thread Pool

**시나리오**: 50개 방에서 5초마다 동시 메시지 브로드캐스팅

```
메시지 생성률 = 50개 방 / 5초 = 10 msg/s (서버 → 클라이언트)

각 메시지당 브로드캐스팅 대상 = 8명 (방당 플레이어)

실제 전송률 = 10 msg/s × 8명 = 80 msg/s
```

**메시지 전송 처리 시간 추정**:

- JSON 직렬화: ~1ms
- 네트워크 I/O (로컬): ~2ms
- 총 처리 시간: **~3ms/메시지**

**필요 스레드 수 (Little's Law)**:

```
필요 스레드 = 전송률 × 처리시간
            = 80 msg/s × 0.003s
            = 0.24 스레드 (이론적 최소)
```

**하지만 현실은 다름**:

- 버스트 트래픽: 50개 방이 **동시에** 5초마다 전송
- 버스트 시 순간 전송률: 50개 방 × 8명 = **400 msg/순간**

```
버스트 시 필요 스레드 = 400msg × 0.003s = 1.2 스레드 (이론)
안전 계수 (p95 고려) = 1.2 × 3 = 3.6 스레드
여유분 (50%) = 3.6 × 1.5 ≈ 6 스레드 (최소)
```

---

## 3. CPU 100% 발생 원인 분석

### 현재 설정

```java
// WebSocketConfig.java
configureClientOutboundChannel:
    corePoolSize = 2
    maxPoolSize = 4
```

### 문제 발생 메커니즘

```
시간 T=0: 50개 방에서 동시에 메시지 전송 시작
         → 400개 메시지가 Outbound Queue에 적재

Queue 상태:
┌─────────────────────────────────────────────┐
│ Outbound Queue: [msg1, msg2, ... msg400]    │
└─────────────────────────────────────────────┘
                    ↓
         4개 스레드가 순차 처리
                    ↓
┌─────┐ ┌─────┐ ┌─────┐ ┌─────┐
│ T-1 │ │ T-2 │ │ T-3 │ │ T-4 │  ← 4개만 동시 처리
└─────┘ └─────┘ └─────┘ └─────┘

처리 시간 계산:
- 400 메시지 ÷ 4 스레드 = 100 메시지/스레드
- 100 메시지 × 3ms = 300ms (최소 처리 시간)
- 실제로는 CPU 경쟁으로 2~3배 증가 = 600~900ms
```

**CPU 100% 원인**:

- 4개 스레드가 300ms 동안 **100% CPU 사용**
- 스레드 간 문맥 교환 오버헤드
- GC 발생 가능성

---

## 4. 최적 Thread Pool 크기 도출

### 공식: Brian Goetz의 스레드 풀 크기 공식

```
최적 스레드 수 = CPU 코어 수 × (1 + 대기시간/처리시간)
```

**EC2 프리티어 (1 vCPU) 기준**:

```
I/O 대기 비율(W/C) = 네트워크 I/O 시간 / CPU 처리 시간
                  ≈ 2ms / 1ms = 2

최적 스레드 수 = 1 × (1 + 2) = 3 스레드 (I/O 바운드 작업 기준)
```

**하지만 버스트 트래픽 고려**:

```
피크 처리량 = 400 msg/5s = 80 msg/s
안전 계수 = 2배 (지연 시 누적 방지)
권장 스레드 = 3 × 2 = 6 스레드 (최소)
```

---

## 5. 권장 설정 (근거 포함)

### WebSocketConfig.java 변경안

```java
@Override
public void configureClientOutboundChannel(ChannelRegistration registration) {
    registration.taskExecutor()
            .corePoolSize(8)     // 이유: 6(최소) + 여유 2
            .maxPoolSize(16)     // 이유: 버스트 시 2배 확장
            .queueCapacity(500)  // 이유: 50방×8명×1.25 = 500
            .keepAliveSeconds(60);
}

@Override
public void configureClientInboundChannel(ChannelRegistration registration) {
    registration.interceptors(webSocketChannelInterceptor);
    registration.taskExecutor()
            .corePoolSize(4)     // 이유: 연결/구독 처리 (상대적 저빈도)
            .maxPoolSize(8)      // 이유: 동시 연결 버스트 대비
            .keepAliveSeconds(60);
}
```

### 계산 근거 요약

| 설정           | 현재 | 권장    | 계산 근거                 |
| -------------- | ---- | ------- | ------------------------- |
| Outbound Core  | 2    | **8**   | 6(Little's Law) + 2(여유) |
| Outbound Max   | 4    | **16**  | Core의 2배 (버스트 대비)  |
| Outbound Queue | 기본 | **500** | 50방 × 8명 × 1.25(버퍼)   |
| Inbound Core   | 2    | **4**   | 연결 처리 2배 개선        |
| Inbound Max    | 4    | **8**   | Core의 2배                |

---

## 6. 예상 효과

| 지표               | 현재      | 개선 후 예상 |
| ------------------ | --------- | ------------ |
| CPU 100% 도달 시점 | 200 VUs   | 400+ VUs     |
| 메시지 처리 지연   | 600-900ms | 100-200ms    |
| 동시 처리 메시지   | 4개       | 16개         |
| 처리량             | 76 msg/s  | 300+ msg/s   |
