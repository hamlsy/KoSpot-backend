# 프론트엔드 연동 가이드

> KoSpot 백엔드 API 프론트엔드 연동을 위한 종합 문서

## 📚 문서 개요

이 폴더에는 프론트엔드 개발자가 KoSpot 로드뷰 개인전 게임을 연동하는 데 필요한 모든 명세와 가이드가 포함되어 있습니다.

---

## 📋 문서 목록

### 1️⃣ [REST API 명세](./ROADVIEW_SOLO_API_GUIDE.md)
**게임 관련 모든 REST API 엔드포인트 상세 명세**

- 게임방 생성/조회/참여/퇴장 API
- 게임 시작 API (첫 라운드)
- 정답 제출 API
- Request/Response 형식
- 인증 방식
- 에러 코드

**이 문서를 먼저 읽어야 하는 경우:**
- REST API 엔드포인트를 찾고 있을 때
- Request Body 형식을 확인하고 싶을 때
- HTTP 상태 코드와 에러 처리를 이해하고 싶을 때

---

### 2️⃣ [WebSocket 명세](./ROADVIEW_SOLO_WEBSOCKET_GUIDE.md)
**WebSocket STOMP 채널 및 실시간 메시지 명세**

- WebSocket 연결 설정
- 구독해야 할 모든 채널 목록
- 타이머 동기화 메시지
- 라운드 결과 브로드캐스트
- 라운드 전환 타이머
- 제출 알림
- 게임 종료 알림
- 클라이언트 구현 예제 (JavaScript)

**이 문서를 먼저 읽어야 하는 경우:**
- WebSocket 연결 방법을 찾고 있을 때
- 실시간 메시지 구독 채널을 확인하고 싶을 때
- 타이머 동기화 로직을 구현하고 싶을 때

---

### 3️⃣ [통합 플로우 가이드](./ROADVIEW_SOLO_INTEGRATION_FLOW.md)
**전체 게임 플로우 및 상세 구현 가이드**

- 전체 게임 플로우 다이어그램
- 단계별 상세 구현 가이드
- React 구현 예제
- 상태 관리 (Redux)
- 에러 처리 및 예외 상황
- 최적화 팁
- 메모리 관리
- 재연결 처리

**이 문서를 먼저 읽어야 하는 경우:**
- 게임 전체 흐름을 이해하고 싶을 때
- React/Redux를 사용한 구현 예제가 필요할 때
- 실제 프로젝트에 통합하는 방법을 알고 싶을 때

---

### 4️⃣ [Response 명세](./ROADVIEW_SOLO_RESPONSE_SPEC.md)
**모든 Response DTO 구조 상세 명세**

- 공통 응답 형식
- 게임방 관련 Response
- 게임 진행 Response
- WebSocket 메시지 Response
- TypeScript 인터페이스 정의
- 데이터 변환 유틸리티
- 검증 및 에러 처리

**이 문서를 먼저 읽어야 하는 경우:**
- Response 데이터 구조를 정확히 알고 싶을 때
- TypeScript 타입 정의가 필요할 때
- 필드별 상세 설명이 필요할 때

---

## 🎮 게임 플로우 요약

```
┌─────────────────────────────────────────────────────────────┐
│                    로드뷰 개인전 게임 플로우                   │
└─────────────────────────────────────────────────────────────┘

1. 게임방 입장
   ↓
2. WebSocket 연결 및 대기실 구독
   ↓
3. 게임 시작 (방장)
   ↓
4. 게임 채널 구독 + 첫 라운드 시작
   ↓
5. 플레이어 정답 제출
   ↓
6. 라운드 결과 수신 (WebSocket)
   ↓
7. 라운드 전환 대기 (10초)
   ↓
8. 다음 라운드 시작 (자동)
   ↓
9. 5~8 반복 (총 라운드 수만큼)
   ↓
10. 게임 종료 및 최종 결과
```

---

## 🚀 빠른 시작 가이드

### Step 1: WebSocket 연결

```javascript
import SockJS from 'sockjs-client';
import { Client } from '@stomp/stompjs';

const socket = new SockJS('http://your-domain.com/ws');
const client = new Client({
  webSocketFactory: () => socket,
  connectHeaders: {
    'Authorization': `Bearer ${token}`
  },
  onConnect: () => {
    console.log('WebSocket connected');
    // 채널 구독 시작
  }
});

client.activate();
```

### Step 2: 게임방 참여

```javascript
const joinRoom = async (roomId) => {
  const response = await fetch(`/rooms/${roomId}/players`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${token}`
    },
    body: JSON.stringify({ password: null })
  });
  
  const data = await response.json();
  if (data.isSuccess) {
    // 게임방 채널 구독
    subscribeToRoomChannels(roomId);
  }
};
```

### Step 3: 게임 시작 (방장)

```javascript
const startGame = async (roomId) => {
  const response = await fetch(`/rooms/${roomId}/roadview/games/solo`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${token}`
    },
    body: JSON.stringify({
      gameRoomId: parseInt(roomId),
      playerMatchTypeKey: 'SOLO',
      totalRounds: 5,
      timeLimit: 60000
    })
  });
  
  const data = await response.json();
  if (data.isSuccess) {
    const { gameId, roundInfo, gamePlayers } = data.result;
    
    // 게임 채널 구독
    subscribeToGameChannels(roomId, gameId);
    
    // 첫 라운드 시작
    startRound(roundInfo);
  }
};
```

### Step 4: WebSocket 구독

```javascript
const subscribeToGameChannels = (roomId, gameId) => {
  // 타이머 시작 구독
  client.subscribe(`/topic/game/${roomId}/timer`, (message) => {
    const data = JSON.parse(message.body);
    handleTimerStart(data);
  });
  
  // 라운드 결과 구독
  client.subscribe(`/topic/game/${roomId}/round/result`, (message) => {
    const data = JSON.parse(message.body);
    handleRoundResult(data);
  });
  
  // 라운드 전환 구독
  client.subscribe(`/topic/game/${roomId}/round/transition`, (message) => {
    const data = JSON.parse(message.body);
    handleRoundTransition(data);
  });
  
  // 다음 라운드 시작 구독
  client.subscribe(`/topic/game/${roomId}/round/start`, (message) => {
    const data = JSON.parse(message.body);
    handleNextRound(data);
  });
  
  // 게임 종료 구독
  client.subscribe(`/topic/game/${roomId}/game/finished`, (message) => {
    const data = JSON.parse(message.body);
    handleGameFinished(data);
  });
  
  // 제출 알림 구독
  client.subscribe(`/topic/game/${gameId}/roadview/submissions/player`, (message) => {
    const data = JSON.parse(message.body);
    handlePlayerSubmission(data);
  });
};
```

### Step 5: 정답 제출

```javascript
const submitAnswer = async (roomId, gameId, roundId, lat, lng) => {
  const timeToAnswer = Date.now() - roundStartTime;
  
  const response = await fetch(
    `/rooms/${roomId}/games/${gameId}/rounds/${roundId}/submissions/player`,
    {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${token}`
      },
      body: JSON.stringify({
        lat,
        lng,
        timeToAnswer
      })
    }
  );
  
  const data = await response.json();
  if (data.isSuccess) {
    console.log('제출 완료');
  }
};
```

---

## 📊 주요 WebSocket 채널

| 채널 | 용도 | 메시지 타입 |
|------|------|------------|
| `/topic/room/{roomId}/playerList` | 플레이어 입장/퇴장 | PlayerJoinedMessage |
| `/topic/room/{roomId}/chat` | 게임방 채팅 | ChatMessage |
| `/topic/room/{roomId}/status` | 게임방 상태 변경 | GameStartedMessage |
| `/topic/game/{roomId}/timer` | 타이머 시작 | TimerStartMessage |
| `/topic/game/{roomId}/round/result` | 라운드 결과 | RoundResult |
| `/topic/game/{roomId}/round/transition` | 라운드 전환 대기 | RoundTransitionTimerMessage |
| `/topic/game/{roomId}/round/start` | 다음 라운드 시작 | NextRoundResponse |
| `/topic/game/{roomId}/game/finished` | 게임 종료 | GameFinishedMessage |
| `/topic/game/{gameId}/roadview/submissions/player` | 제출 알림 | PlayerSubmissionMessage |

---

## 🔑 주요 REST API 엔드포인트

### 게임방 관리
- `GET /rooms?page={page}` - 게임방 목록 조회
- `POST /rooms` - 게임방 생성
- `GET /rooms/{roomId}` - 게임방 상세 조회
- `POST /rooms/{roomId}/players` - 게임방 참여
- `DELETE /rooms/{roomId}/players` - 게임방 퇴장

### 게임 진행
- `POST /rooms/{roomId}/roadview/games/solo` - 게임 시작 (첫 라운드)
- `POST /rooms/{roomId}/games/{gameId}/rounds/{roundId}/submissions/player` - 정답 제출

---

## 💡 중요 개념

### 1. 타이머 동기화

서버와 클라이언트의 시간 차이를 보정하여 정확한 타이머 표시:

```javascript
const timeDiff = serverTimestamp - Date.now();
const syncedTime = Date.now() + timeDiff;
```

### 2. 라운드 전환

각 라운드 종료 후 10초 대기 시간:

```
라운드 결과 표시 (무제한)
  ↓
10초 카운트다운
  ↓
다음 라운드 자동 시작
```

### 3. 조기 종료

모든 플레이어가 제출 완료 시 타이머를 기다리지 않고 즉시 라운드 종료

---

## 🛠 기술 스택

### 백엔드
- Spring Boot 3.x
- WebSocket (STOMP)
- MySQL (게임 데이터)
- Redis (실시간 데이터)

### 권장 프론트엔드 스택
- React 18+
- TypeScript
- SockJS + @stomp/stompjs
- Redux Toolkit (상태 관리)
- Axios (HTTP 클라이언트)

---

## 🎯 체크리스트

### 연동 전 준비사항
- [ ] JWT 인증 토큰 획득 방법 확인
- [ ] WebSocket 엔드포인트 확인
- [ ] 지도 API (Google Maps / Kakao Maps) 연동
- [ ] Street View API 연동

### 필수 구현 사항
- [ ] WebSocket 연결 및 재연결
- [ ] 게임방 입장/퇴장
- [ ] 게임 시작 (방장)
- [ ] 타이머 동기화
- [ ] 정답 제출
- [ ] 라운드 결과 표시
- [ ] 라운드 전환 카운트다운
- [ ] 게임 종료 처리

### 선택 구현 사항
- [ ] 게임방 채팅
- [ ] 플레이어 목록 실시간 업데이트
- [ ] 제출 알림 (누가 제출했는지)
- [ ] 로컬 스토리지를 통한 게임 상태 복구
- [ ] 에러 처리 및 재시도 로직

---

## 🐛 문제 해결

### WebSocket 연결 실패
```javascript
// 재연결 로직
onStompError: (frame) => {
  console.error('STOMP error:', frame);
  setTimeout(() => {
    client.activate(); // 재연결 시도
  }, 5000);
}
```

### 타이머 동기화 오류
```javascript
// 서버 시간으로 동기화
const timeDiff = serverTimestamp - Date.now();
const syncedNow = Date.now() + timeDiff;
```

### 메모리 누수
```javascript
// 컴포넌트 언마운트 시 정리
useEffect(() => {
  return () => {
    // 모든 구독 해제
    subscriptions.forEach(sub => sub.unsubscribe());
    // 타이머 정리
    clearInterval(timerInterval);
  };
}, []);
```

---

## 📞 지원

### 문서 관련 문의
- GitHub Issues 등록
- 백엔드 팀 Slack 채널

### API 관련 문의
- Swagger UI: `http://your-domain.com/swagger-ui.html`
- API 문서: 이 폴더의 문서들 참고

---

## 📝 변경 이력

### 2024-10-26
- 초기 문서 작성
- REST API 명세 작성
- WebSocket 명세 작성
- 통합 플로우 가이드 작성
- Response 명세 작성

---

## 🔗 관련 문서

### 백엔드 내부 문서
- [아키텍처 흐름도](../아키텍처_흐름도.md)
- [WebSocket 구독 가이드](../WebSocket-Subscription-Guide.md)
- [Redis STOMP 통합 가이드](../Redis-STOMP-Integration-Guide.md)
- [조기 종료 로직 구현](../EARLY_ROUND_COMPLETION_IMPLEMENTATION.md)

### 외부 리소스
- [Spring WebSocket 공식 문서](https://docs.spring.io/spring-framework/reference/web/websocket.html)
- [STOMP Protocol](https://stomp.github.io/)
- [SockJS](https://github.com/sockjs/sockjs-client)

---

## 📄 라이선스

이 문서는 KoSpot 프로젝트의 일부입니다.

