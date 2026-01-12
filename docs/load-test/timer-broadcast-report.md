# WebSocket Timer Broadcasting 부하테스트 결과 보고서

**테스트 일시**: [YYYY-MM-DD HH:MM]  
**테스트 도구**: k6 v1.5.0  
**테스트 스크립트**: `k6-scripts/timer-broadcast-test.js`

---

## 1. 테스트 환경

| 항목        | 값                   |
| ----------- | -------------------- |
| OS          | Windows [버전]       |
| CPU         | [모델명, 코어수]     |
| RAM         | [GB]                 |
| JVM         | Java 17, Heap [설정] |
| Spring Boot | 3.4.1                |

### 서버 설정

- `gameTimerTaskScheduler`: pool size 10
- `WebSocket Outbound`: core 2, max 4
- Timer Sync Interval: 5000ms

---

## 2. 테스트 시나리오

| Stage | Duration | Target VUs |
| ----- | -------- | ---------- |
| 1     | 30s      | 0 → 50     |
| 2     | 60s      | 50 (hold)  |
| 3     | 30s      | 50 → 200   |
| 4     | 60s      | 200 (hold) |
| 5     | 30s      | 200 → 500  |
| 6     | 60s      | 500 (hold) |
| 7     | 30s      | 500 → 0    |

---

## 3. 핵심 지표 결과

### Timer Message Interval (5초 기대)

| Metric  | Value |
| ------- | ----- |
| Average | [ ]ms |
| Min     | [ ]ms |
| Max     | [ ]ms |
| p(95)   | [ ]ms |

### Message Receive Latency

| Metric  | Value |
| ------- | ----- |
| Average | [ ]ms |
| p(95)   | [ ]ms |

### Accuracy

| Metric                  | Value |
| ----------------------- | ----- |
| Interval Accuracy Rate  | [ ]%  |
| Total Messages Received | [ ]   |

---

## 4. VisualVM 모니터링

### CPU Usage

[스크린샷 첨부]

### Live Threads

[스크린샷 첨부]

### Heap Memory

[스크린샷 첨부]

---

## 5. Breakpoint 분석

### 발견된 병목 지점

- [ ] Thread Pool Exhaustion
- [ ] Outbound Channel Blocking
- [ ] GC Pause
- [ ] 기타:

### 세부 분석

[상세 내용 기술]

---

## 6. 결론

### 왜 5초인가?

[테스트 결과 기반 설명]

### 확장성 평가

[현재 설정으로 지원 가능한 최대 동시접속자 수]

### 개선 권고사항

[필요시 튜닝 포인트]

---

## 7. 참고 명령어

```powershell
# k6 테스트 실행
k6 run k6-scripts/timer-broadcast-test.js

# JSON 결과 출력
k6 run --out json=k6-results.json k6-scripts/timer-broadcast-test.js
```
