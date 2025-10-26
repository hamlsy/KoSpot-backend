# 로드뷰 개인전 통합 가이드

> 프론트엔드 개발자를 위한 전체 게임 플로우 및 상세 구현 가이드

## 📋 목차
1. [전체 게임 플로우](#1-전체-게임-플로우)
2. [단계별 상세 가이드](#2-단계별-상세-가이드)
3. [React 구현 예제](#3-react-구현-예제)
4. [상태 관리](#4-상태-관리)
5. [에러 처리 및 예외 상황](#5-에러-처리-및-예외-상황)
6. [최적화 팁](#6-최적화-팁)

---

## 1. 전체 게임 플로우

```
┌─────────────────────────────────────────────────────────────────┐
│                         게임 진행 플로우                           │
└─────────────────────────────────────────────────────────────────┘

[1] 게임방 입장
     │
     ├─ REST: POST /rooms/{roomId}/players
     ├─ WebSocket 연결 및 구독 시작
     └─ Subscribe: /topic/room/{roomId}/playerList
     
     ↓
     
[2] 대기실 (다른 플레이어 대기)
     │
     ├─ Subscribe: /topic/room/{roomId}/chat
     ├─ Subscribe: /topic/room/{roomId}/status
     └─ 방장: 게임 시작 버튼 활성화
     
     ↓
     
[3] 게임 시작 (방장만 가능)
     │
     ├─ REST: POST /rooms/{roomId}/roadview/games/solo
     │         Body: { gameRoomId, totalRounds, timeLimit }
     │
     ├─ Response: { gameId, roundInfo, gamePlayers }
     │
     ├─ Subscribe: /topic/game/{roomId}/timer
     ├─ Subscribe: /topic/game/{roomId}/round/result
     ├─ Subscribe: /topic/game/{roomId}/round/transition
     ├─ Subscribe: /topic/game/{roomId}/round/start
     ├─ Subscribe: /topic/game/{roomId}/game/finished
     └─ Subscribe: /topic/game/{gameId}/roadview/submissions/player
     
     ↓
     
[4] 라운드 1 시작
     │
     ├─ Receive: /topic/game/{roomId}/timer (타이머 시작)
     ├─ 화면: 로드뷰 문제 표시 (targetLat, targetLng)
     └─ 타이머: 60초 카운트다운
     
     ↓
     
[5] 플레이어 정답 제출
     │
     ├─ REST: POST .../rounds/{roundId}/submissions/player
     │         Body: { lat, lng, timeToAnswer }
     │
     ├─ Receive: /topic/game/{gameId}/roadview/submissions/player
     │            (다른 플레이어들의 제출 알림)
     │
     └─ 화면: "제출 완료" 대기 화면
     
     ↓
     
[6] 라운드 종료 (타이머 종료 OR 모두 제출)
     │
     ├─ Receive: /topic/game/{roomId}/round/result
     │            { 정답 좌표, 각 플레이어 결과, 순위 }
     │
     └─ 화면: 결과 화면 (지도, 순위표, 점수)
     
     ↓
     
[7] 라운드 전환 대기 (10초)
     │
     ├─ Receive: /topic/game/{roomId}/round/transition
     │            { nextRoundStartTimeMs }
     │
     └─ 화면: 10초 카운트다운
     
     ↓
     
[8] 다음 라운드 시작
     │
     ├─ Receive: /topic/game/{roomId}/round/start
     │            { currentRound: 2, roundInfo }
     │
     ├─ Receive: /topic/game/{roomId}/timer
     │
     └─ 화면: 새 로드뷰 문제
     
     ↓
     
[9] [5]~[8] 반복 (총 라운드 수만큼)
     
     ↓
     
[10] 게임 종료
     │
     ├─ Receive: /topic/game/{roomId}/game/finished
     │            { gameId, message }
     │
     └─ 화면: 최종 순위 및 게임 종료 화면
```

---

## 2. 단계별 상세 가이드

### 2.1 게임방 입장 및 대기

#### REST API 호출
```javascript
// 게임방 참여
const joinRoom = async (roomId) => {
  try {
    const response = await fetch(`/rooms/${roomId}/players`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${token}`
      },
      body: JSON.stringify({
        password: null // 비공개 방인 경우 비밀번호
      })
    });
    
    const data = await response.json();
    
    if (data.isSuccess) {
      console.log('방 참여 성공');
      // WebSocket 구독 시작
      setupRoomSubscriptions(roomId);
    }
  } catch (error) {
    console.error('방 참여 실패:', error);
  }
};
```

#### WebSocket 구독
```javascript
const setupRoomSubscriptions = (roomId) => {
  // 플레이어 목록 구독
  ws.subscribe(`/topic/room/${roomId}/playerList`, (message) => {
    console.log('플레이어 목록 업데이트:', message);
    updatePlayerList(message);
  });
  
  // 채팅 구독
  ws.subscribe(`/topic/room/${roomId}/chat`, (message) => {
    console.log('채팅 메시지:', message);
    addChatMessage(message);
  });
  
  // 방 상태 구독
  ws.subscribe(`/topic/room/${roomId}/status`, (message) => {
    console.log('방 상태 변경:', message);
    if (message.type === 'GAME_STARTED') {
      handleGameStarted(message.gameId);
    }
  });
};
```

---

### 2.2 게임 시작

#### REST API 호출 (방장만)
```javascript
const startGame = async (roomId, settings) => {
  try {
    const response = await fetch(`/rooms/${roomId}/roadview/games/solo`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${token}`
      },
      body: JSON.stringify({
        gameRoomId: parseInt(roomId),
        playerMatchTypeKey: 'SOLO',
        totalRounds: settings.totalRounds, // 1~10
        timeLimit: settings.timeLimit      // 밀리초, 예: 60000
      })
    });
    
    const data = await response.json();
    
    if (data.isSuccess) {
      const { gameId, totalRounds, currentRound, roundInfo, gamePlayers } = data.result;
      
      console.log('게임 시작:', {
        gameId,
        totalRounds,
        currentRound,
        roundInfo,
        players: gamePlayers
      });
      
      // 게임 구독 설정
      setupGameSubscriptions(roomId, gameId);
      
      // 첫 라운드 화면 표시
      showRoundScreen(roundInfo, gamePlayers);
      
      return data.result;
    }
  } catch (error) {
    console.error('게임 시작 실패:', error);
  }
};
```

#### 게임 채널 구독
```javascript
const setupGameSubscriptions = (roomId, gameId) => {
  // 1. 타이머 구독 (라운드 시작)
  ws.subscribe(`/topic/game/${roomId}/timer`, (message) => {
    console.log('타이머 시작:', message);
    startRoundTimer(message);
  });
  
  // 2. 라운드 결과 구독
  ws.subscribe(`/topic/game/${roomId}/round/result`, (message) => {
    console.log('라운드 결과:', message);
    showRoundResult(message);
  });
  
  // 3. 라운드 전환 타이머 구독
  ws.subscribe(`/topic/game/${roomId}/round/transition`, (message) => {
    console.log('라운드 전환 대기:', message);
    startTransitionTimer(message);
  });
  
  // 4. 다음 라운드 시작 구독
  ws.subscribe(`/topic/game/${roomId}/round/start`, (message) => {
    console.log('다음 라운드 시작:', message);
    showNextRound(message);
  });
  
  // 5. 게임 종료 구독
  ws.subscribe(`/topic/game/${roomId}/game/finished`, (message) => {
    console.log('게임 종료:', message);
    showGameFinished(message);
  });
  
  // 6. 제출 알림 구독
  ws.subscribe(`/topic/game/${gameId}/roadview/submissions/player`, (message) => {
    console.log('플레이어 제출:', message);
    showSubmissionNotification(message);
  });
};
```

---

### 2.3 라운드 진행

#### 타이머 시작
```javascript
const startRoundTimer = (message) => {
  const { serverStartTimeMs, durationMs, serverTimestamp } = message;
  
  // 서버-클라이언트 시간차 계산
  const timeDiff = serverTimestamp - Date.now();
  
  // 타이머 인터벌 시작
  const interval = setInterval(() => {
    const now = Date.now() + timeDiff;
    const elapsed = now - serverStartTimeMs;
    const remaining = Math.max(0, durationMs - elapsed);
    
    // UI 업데이트
    updateTimerDisplay(remaining);
    
    // 타이머 종료
    if (remaining <= 0) {
      clearInterval(interval);
      onTimerExpired();
    }
  }, 100);
  
  // 인터벌 저장 (정리용)
  window.currentTimerInterval = interval;
};

const updateTimerDisplay = (remainingMs) => {
  const seconds = Math.ceil(remainingMs / 1000);
  document.getElementById('timer').textContent = `${seconds}초`;
  
  // 마지막 10초는 빨간색
  if (seconds <= 10) {
    document.getElementById('timer').classList.add('urgent');
  }
};

const onTimerExpired = () => {
  console.log('타이머 종료!');
  // 제출하지 않은 경우 자동으로 현재 위치 제출 또는 대기
};
```

#### 정답 제출
```javascript
const submitAnswer = async (roomId, gameId, roundId, position, startTime) => {
  const timeToAnswer = Date.now() - startTime;
  
  try {
    const response = await fetch(
      `/rooms/${roomId}/games/${gameId}/rounds/${roundId}/submissions/player`,
      {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${token}`
        },
        body: JSON.stringify({
          lat: position.lat,
          lng: position.lng,
          timeToAnswer: timeToAnswer
        })
      }
    );
    
    const data = await response.json();
    
    if (data.isSuccess) {
      console.log('제출 완료');
      
      // 제출 완료 UI 표시
      showSubmissionComplete();
      
      // 타이머 정리
      if (window.currentTimerInterval) {
        clearInterval(window.currentTimerInterval);
      }
    }
  } catch (error) {
    console.error('제출 실패:', error);
    alert('제출에 실패했습니다. 다시 시도해주세요.');
  }
};
```

#### 제출 알림 처리
```javascript
const showSubmissionNotification = (message) => {
  const { playerId, roundId, timestamp } = message;
  
  // "플레이어 X가 제출했습니다" 메시지 표시
  const notification = document.createElement('div');
  notification.className = 'submission-notification';
  notification.textContent = `플레이어 ${playerId}가 제출했습니다`;
  document.body.appendChild(notification);
  
  // 3초 후 제거
  setTimeout(() => {
    notification.remove();
  }, 3000);
  
  // 제출 카운터 업데이트
  submittedPlayers.add(playerId);
  updateSubmissionCounter(submittedPlayers.size, totalPlayers);
};

const updateSubmissionCounter = (submitted, total) => {
  document.getElementById('submission-counter').textContent = 
    `${submitted}/${total}명 제출 완료`;
};
```

---

### 2.4 라운드 결과 표시

```javascript
const showRoundResult = (result) => {
  const {
    roundNumber,
    targetLat,
    targetLng,
    playerSubmissionResults,
    playerTotalResults
  } = result;
  
  console.log('라운드 결과:', result);
  
  // 1. 정답 좌표 표시
  showAnswerLocation(targetLat, targetLng);
  
  // 2. 각 플레이어의 제출 위치를 지도에 표시
  playerSubmissionResults.forEach((submission, index) => {
    const player = playerTotalResults[index];
    showPlayerMarker(
      submission.lat,
      submission.lng,
      player.nickname,
      player.markerImageUrl
    );
    
    // 정답과 제출 위치 사이에 선 그리기
    drawLine(targetLat, targetLng, submission.lat, submission.lng);
  });
  
  // 3. 순위표 표시
  showLeaderboard(playerTotalResults);
  
  // 4. 개인 결과 표시
  const myResult = playerSubmissionResults.find(s => s.playerId === myPlayerId);
  if (myResult) {
    showMyResult(myResult);
  }
};

const showLeaderboard = (players) => {
  // 점수 순으로 정렬 (이미 정렬되어 있음)
  const sortedPlayers = [...players].sort((a, b) => b.totalScore - a.totalScore);
  
  const leaderboardHTML = sortedPlayers.map((player, index) => `
    <div class="leaderboard-item rank-${index + 1}">
      <span class="rank">${index + 1}</span>
      <img src="${player.markerImageUrl}" alt="${player.nickname}">
      <span class="nickname">${player.nickname}</span>
      <span class="score">${player.totalScore.toLocaleString()}점</span>
    </div>
  `).join('');
  
  document.getElementById('leaderboard').innerHTML = leaderboardHTML;
};

const showMyResult = (result) => {
  const { distance, timeToAnswer, earnedScore } = result;
  
  const resultHTML = `
    <div class="my-result">
      <h3>내 결과</h3>
      <p>거리: ${distance.toFixed(2)}m</p>
      <p>시간: ${(timeToAnswer / 1000).toFixed(2)}초</p>
      <p>획득 점수: ${earnedScore.toLocaleString()}점</p>
    </div>
  `;
  
  document.getElementById('my-result').innerHTML = resultHTML;
};
```

---

### 2.5 라운드 전환

```javascript
const startTransitionTimer = (message) => {
  const { nextRoundStartTimeMs, serverTimestamp, isLastRound } = message;
  
  // 서버-클라이언트 시간차
  const timeDiff = serverTimestamp - Date.now();
  
  // 10초 카운트다운
  const interval = setInterval(() => {
    const now = Date.now() + timeDiff;
    const remaining = Math.max(0, nextRoundStartTimeMs - now);
    const seconds = Math.ceil(remaining / 1000);
    
    // UI 업데이트
    if (isLastRound) {
      updateTransitionDisplay(seconds, '게임 종료까지');
    } else {
      updateTransitionDisplay(seconds, '다음 라운드까지');
    }
    
    if (remaining <= 0) {
      clearInterval(interval);
    }
  }, 100);
};

const updateTransitionDisplay = (seconds, label) => {
  document.getElementById('transition-timer').innerHTML = `
    <div class="transition-countdown">
      <p>${label}</p>
      <h1>${seconds}</h1>
    </div>
  `;
};
```

---

### 2.6 다음 라운드 시작

```javascript
const showNextRound = (message) => {
  const { gameId, currentRound, roundInfo } = message;
  
  console.log(`라운드 ${currentRound} 시작:`, roundInfo);
  
  // 1. 상태 초기화
  resetRoundState();
  
  // 2. 새 라운드 정보 저장
  window.currentRoundId = roundInfo.roundId;
  window.currentRoundNumber = roundInfo.roundNumber;
  window.roundStartTime = Date.now();
  
  // 3. 새 로드뷰 문제 표시
  loadRoadView(roundInfo.targetLat, roundInfo.targetLng);
  
  // 4. 라운드 번호 UI 업데이트
  document.getElementById('round-number').textContent = 
    `라운드 ${currentRound}`;
};

const resetRoundState = () => {
  // 제출 상태 초기화
  submittedPlayers.clear();
  updateSubmissionCounter(0, totalPlayers);
  
  // 이전 타이머 정리
  if (window.currentTimerInterval) {
    clearInterval(window.currentTimerInterval);
  }
  
  // UI 초기화
  document.getElementById('submit-button').disabled = false;
  document.getElementById('timer').classList.remove('urgent');
};

const loadRoadView = (lat, lng) => {
  // Google Street View 또는 Kakao Road View 로드
  // 실제 구현은 사용하는 지도 API에 따라 다름
  
  const panorama = new google.maps.StreetViewPanorama(
    document.getElementById('roadview'),
    {
      position: { lat, lng },
      pov: { heading: 0, pitch: 0 },
      zoom: 1
    }
  );
};
```

---

### 2.7 게임 종료

```javascript
const showGameFinished = (message) => {
  const { gameId, message: finishMessage, timestamp } = message;
  
  console.log('게임 종료:', finishMessage);
  
  // 1. 모든 타이머 정리
  if (window.currentTimerInterval) {
    clearInterval(window.currentTimerInterval);
  }
  
  // 2. 최종 순위 화면 표시
  showFinalResults();
  
  // 3. 게임 종료 모달
  showGameFinishedModal({
    message: finishMessage,
    timestamp: timestamp
  });
};

const showFinalResults = () => {
  // 최종 순위는 마지막 라운드 결과에서 이미 확인 가능
  // 추가로 전체 게임 통계 표시
  
  const statsHTML = `
    <div class="final-stats">
      <h2>게임 결과</h2>
      <div class="leaderboard">
        <!-- 최종 순위표 -->
      </div>
      <div class="stats">
        <p>평균 거리: ${calculateAverageDistance()}m</p>
        <p>평균 시간: ${calculateAverageTime()}초</p>
        <p>최고 점수 라운드: ${getBestRound()}</p>
      </div>
      <button onclick="returnToLobby()">로비로 돌아가기</button>
    </div>
  `;
  
  document.getElementById('game-container').innerHTML = statsHTML;
};
```

---

## 3. React 구현 예제

### 3.1 커스텀 훅: useGameWebSocket

```typescript
import { useEffect, useRef, useState } from 'react';
import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';

interface WebSocketMessage {
  destination: string;
  data: any;
}

export const useGameWebSocket = (token: string, roomId: string) => {
  const clientRef = useRef<Client | null>(null);
  const [isConnected, setIsConnected] = useState(false);
  const [messages, setMessages] = useState<WebSocketMessage[]>([]);
  const subscriptionsRef = useRef<Map<string, any>>(new Map());

  useEffect(() => {
    const socket = new SockJS('http://your-domain.com/ws');
    
    const client = new Client({
      webSocketFactory: () => socket,
      connectHeaders: {
        'Authorization': `Bearer ${token}`
      },
      debug: (str) => console.log(str),
      reconnectDelay: 5000,
      heartbeatIncoming: 4000,
      heartbeatOutgoing: 4000,
      onConnect: () => {
        console.log('WebSocket connected');
        setIsConnected(true);
      },
      onDisconnect: () => {
        console.log('WebSocket disconnected');
        setIsConnected(false);
      },
      onStompError: (frame) => {
        console.error('STOMP error:', frame);
      }
    });

    client.activate();
    clientRef.current = client;

    return () => {
      client.deactivate();
    };
  }, [token]);

  const subscribe = (destination: string, callback: (data: any) => void) => {
    if (!clientRef.current || !isConnected) {
      console.error('Client is not connected');
      return;
    }

    const subscription = clientRef.current.subscribe(destination, (message) => {
      const data = JSON.parse(message.body);
      callback(data);
      
      setMessages(prev => [...prev, { destination, data }]);
    });

    subscriptionsRef.current.set(destination, subscription);
  };

  const unsubscribe = (destination: string) => {
    const subscription = subscriptionsRef.current.get(destination);
    if (subscription) {
      subscription.unsubscribe();
      subscriptionsRef.current.delete(destination);
    }
  };

  const send = (destination: string, body: any) => {
    if (!clientRef.current || !isConnected) {
      console.error('Client is not connected');
      return;
    }

    clientRef.current.publish({
      destination,
      body: JSON.stringify(body)
    });
  };

  return {
    isConnected,
    subscribe,
    unsubscribe,
    send,
    messages
  };
};
```

### 3.2 게임 컴포넌트

```typescript
import React, { useEffect, useState } from 'react';
import { useGameWebSocket } from './hooks/useGameWebSocket';

interface RoundInfo {
  roundId: number;
  roundNumber: number;
  targetLat: number;
  targetLng: number;
}

interface GameState {
  gameId: number | null;
  currentRound: number;
  totalRounds: number;
  roundInfo: RoundInfo | null;
  players: any[];
  isPlaying: boolean;
}

export const RoadViewGame: React.FC = () => {
  const roomId = '1'; // 실제로는 라우터에서 가져옴
  const token = 'your-token'; // 실제로는 인증 컨텍스트에서 가져옴
  
  const { isConnected, subscribe, unsubscribe } = useGameWebSocket(token, roomId);
  
  const [gameState, setGameState] = useState<GameState>({
    gameId: null,
    currentRound: 0,
    totalRounds: 5,
    roundInfo: null,
    players: [],
    isPlaying: false
  });
  
  const [timerRemaining, setTimerRemaining] = useState<number>(0);
  const [roundResult, setRoundResult] = useState<any>(null);
  const [transitionSeconds, setTransitionSeconds] = useState<number>(0);

  // WebSocket 구독 설정
  useEffect(() => {
    if (!isConnected) return;

    // 타이머 구독
    subscribe(`/topic/game/${roomId}/timer`, (message) => {
      console.log('Timer started:', message);
      handleTimerStart(message);
    });

    // 라운드 결과 구독
    subscribe(`/topic/game/${roomId}/round/result`, (message) => {
      console.log('Round result:', message);
      setRoundResult(message);
    });

    // 라운드 전환 구독
    subscribe(`/topic/game/${roomId}/round/transition`, (message) => {
      console.log('Round transition:', message);
      handleRoundTransition(message);
    });

    // 다음 라운드 시작 구독
    subscribe(`/topic/game/${roomId}/round/start`, (message) => {
      console.log('Next round start:', message);
      handleNextRound(message);
    });

    // 게임 종료 구독
    subscribe(`/topic/game/${roomId}/game/finished`, (message) => {
      console.log('Game finished:', message);
      handleGameFinished(message);
    });

    return () => {
      unsubscribe(`/topic/game/${roomId}/timer`);
      unsubscribe(`/topic/game/${roomId}/round/result`);
      unsubscribe(`/topic/game/${roomId}/round/transition`);
      unsubscribe(`/topic/game/${roomId}/round/start`);
      unsubscribe(`/topic/game/${roomId}/game/finished`);
    };
  }, [isConnected, roomId]);

  // 제출 알림 구독 (gameId가 생성된 후)
  useEffect(() => {
    if (!isConnected || !gameState.gameId) return;

    subscribe(`/topic/game/${gameState.gameId}/roadview/submissions/player`, (message) => {
      console.log('Player submitted:', message);
      // 제출 알림 처리
    });

    return () => {
      unsubscribe(`/topic/game/${gameState.gameId}/roadview/submissions/player`);
    };
  }, [isConnected, gameState.gameId]);

  const handleTimerStart = (message: any) => {
    const { serverStartTimeMs, durationMs, serverTimestamp } = message;
    const timeDiff = serverTimestamp - Date.now();
    
    const interval = setInterval(() => {
      const now = Date.now() + timeDiff;
      const elapsed = now - serverStartTimeMs;
      const remaining = Math.max(0, durationMs - elapsed);
      
      setTimerRemaining(remaining);
      
      if (remaining <= 0) {
        clearInterval(interval);
      }
    }, 100);
  };

  const handleRoundTransition = (message: any) => {
    const { nextRoundStartTimeMs, serverTimestamp } = message;
    const timeDiff = serverTimestamp - Date.now();
    
    const interval = setInterval(() => {
      const now = Date.now() + timeDiff;
      const remaining = Math.max(0, nextRoundStartTimeMs - now);
      const seconds = Math.ceil(remaining / 1000);
      
      setTransitionSeconds(seconds);
      
      if (remaining <= 0) {
        clearInterval(interval);
        setTransitionSeconds(0);
      }
    }, 100);
  };

  const handleNextRound = (message: any) => {
    const { gameId, currentRound, roundInfo } = message;
    
    setGameState(prev => ({
      ...prev,
      currentRound,
      roundInfo
    }));
    
    setRoundResult(null);
  };

  const handleGameFinished = (message: any) => {
    setGameState(prev => ({
      ...prev,
      isPlaying: false
    }));
    
    alert(message.message);
  };

  const startGame = async () => {
    try {
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
        const { gameId, totalRounds, currentRound, roundInfo, gamePlayers } = data.result;
        
        setGameState({
          gameId,
          totalRounds,
          currentRound,
          roundInfo,
          players: gamePlayers,
          isPlaying: true
        });
      }
    } catch (error) {
      console.error('게임 시작 실패:', error);
    }
  };

  const submitAnswer = async (lat: number, lng: number) => {
    if (!gameState.gameId || !gameState.roundInfo) return;
    
    const timeToAnswer = Date.now() - window.roundStartTime;
    
    try {
      const response = await fetch(
        `/rooms/${roomId}/games/${gameState.gameId}/rounds/${gameState.roundInfo.roundId}/submissions/player`,
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
    } catch (error) {
      console.error('제출 실패:', error);
    }
  };

  return (
    <div className="game-container">
      {!gameState.isPlaying ? (
        <div className="lobby">
          <h1>로드뷰 게임</h1>
          <button onClick={startGame}>게임 시작</button>
        </div>
      ) : (
        <>
          {/* 게임 진행 중 */}
          {!roundResult && !transitionSeconds ? (
            <div className="playing">
              <div className="game-header">
                <span>라운드 {gameState.currentRound}/{gameState.totalRounds}</span>
                <span className="timer">
                  {Math.ceil(timerRemaining / 1000)}초
                </span>
              </div>
              
              {/* 로드뷰 화면 */}
              <div id="roadview" style={{ width: '100%', height: '500px' }}>
                {/* 로드뷰 컴포넌트 */}
              </div>
              
              {/* 지도 (제출용) */}
              <div id="map" style={{ width: '100%', height: '300px' }}>
                {/* 지도 컴포넌트 */}
              </div>
              
              <button onClick={() => submitAnswer(37.5665, 126.9780)}>
                정답 제출
              </button>
            </div>
          ) : transitionSeconds ? (
            <div className="transition">
              <h2>다음 라운드까지</h2>
              <h1>{transitionSeconds}</h1>
            </div>
          ) : roundResult ? (
            <div className="result">
              <h2>라운드 {roundResult.roundNumber} 결과</h2>
              
              {/* 결과 지도 */}
              <div id="result-map" style={{ width: '100%', height: '400px' }}>
                {/* 정답 + 각 플레이어 위치 표시 */}
              </div>
              
              {/* 순위표 */}
              <div className="leaderboard">
                {roundResult.playerTotalResults.map((player: any, index: number) => (
                  <div key={player.playerId} className="leaderboard-item">
                    <span>{index + 1}</span>
                    <img src={player.markerImageUrl} alt={player.nickname} />
                    <span>{player.nickname}</span>
                    <span>{player.totalScore}</span>
                  </div>
                ))}
              </div>
            </div>
          ) : null}
        </>
      )}
    </div>
  );
};
```

---

## 4. 상태 관리

### 4.1 Redux Toolkit 예제

```typescript
// gameSlice.ts
import { createSlice, PayloadAction } from '@reduxjs/toolkit';

interface GameState {
  gameId: number | null;
  roomId: string | null;
  currentRound: number;
  totalRounds: number;
  roundInfo: RoundInfo | null;
  players: Player[];
  isPlaying: boolean;
  timerRemaining: number;
  roundResult: RoundResult | null;
}

const initialState: GameState = {
  gameId: null,
  roomId: null,
  currentRound: 0,
  totalRounds: 5,
  roundInfo: null,
  players: [],
  isPlaying: false,
  timerRemaining: 0,
  roundResult: null
};

const gameSlice = createSlice({
  name: 'game',
  initialState,
  reducers: {
    setGameStarted: (state, action: PayloadAction<any>) => {
      const { gameId, totalRounds, currentRound, roundInfo, gamePlayers } = action.payload;
      state.gameId = gameId;
      state.totalRounds = totalRounds;
      state.currentRound = currentRound;
      state.roundInfo = roundInfo;
      state.players = gamePlayers;
      state.isPlaying = true;
    },
    setTimerRemaining: (state, action: PayloadAction<number>) => {
      state.timerRemaining = action.payload;
    },
    setRoundResult: (state, action: PayloadAction<any>) => {
      state.roundResult = action.payload;
    },
    setNextRound: (state, action: PayloadAction<any>) => {
      const { currentRound, roundInfo } = action.payload;
      state.currentRound = currentRound;
      state.roundInfo = roundInfo;
      state.roundResult = null;
    },
    setGameFinished: (state) => {
      state.isPlaying = false;
    },
    resetGame: () => initialState
  }
});

export const {
  setGameStarted,
  setTimerRemaining,
  setRoundResult,
  setNextRound,
  setGameFinished,
  resetGame
} = gameSlice.actions;

export default gameSlice.reducer;
```

---

## 5. 에러 처리 및 예외 상황

### 5.1 WebSocket 재연결

```javascript
const handleReconnect = () => {
  console.log('WebSocket 재연결 시도...');
  
  // 현재 게임 상태 저장
  const savedState = {
    gameId: window.gameState.gameId,
    roomId: window.gameState.roomId,
    currentRound: window.gameState.currentRound
  };
  
  // 재연결
  ws.connect(() => {
    console.log('재연결 성공');
    
    // 모든 채널 재구독
    if (savedState.roomId) {
      setupRoomSubscriptions(savedState.roomId);
      
      if (savedState.gameId) {
        setupGameSubscriptions(savedState.roomId, savedState.gameId);
      }
    }
  });
};
```

### 5.2 타임아웃 처리

```javascript
const submitWithTimeout = async (roomId, gameId, roundId, position, startTime) => {
  const timeout = 5000; // 5초
  
  try {
    const response = await Promise.race([
      submitAnswer(roomId, gameId, roundId, position, startTime),
      new Promise((_, reject) => 
        setTimeout(() => reject(new Error('Timeout')), timeout)
      )
    ]);
    
    return response;
  } catch (error) {
    if (error.message === 'Timeout') {
      console.error('제출 타임아웃');
      // 재시도 로직
      return retrySubmit(roomId, gameId, roundId, position, startTime);
    }
    throw error;
  }
};
```

### 5.3 네트워크 에러

```javascript
const handleNetworkError = (error) => {
  console.error('네트워크 에러:', error);
  
  // 사용자에게 알림
  showNotification({
    type: 'error',
    message: '네트워크 연결이 불안정합니다. 다시 시도해주세요.'
  });
  
  // 재연결 시도
  if (error.type === 'connection') {
    setTimeout(() => {
      handleReconnect();
    }, 3000);
  }
};
```

---

## 6. 최적화 팁

### 6.1 메모리 관리

```javascript
// 컴포넌트 언마운트 시 정리
useEffect(() => {
  return () => {
    // 타이머 정리
    if (timerIntervalRef.current) {
      clearInterval(timerIntervalRef.current);
    }
    
    // WebSocket 구독 해제
    unsubscribeAll();
    
    // 이벤트 리스너 제거
    removeAllEventListeners();
  };
}, []);
```

### 6.2 성능 최적화

```javascript
// 메시지 디바운싱
const debouncedUpdateUI = useMemo(
  () => debounce((data) => {
    updateUI(data);
  }, 100),
  []
);

// 메모이제이션
const memoizedLeaderboard = useMemo(
  () => calculateLeaderboard(players),
  [players]
);
```

### 6.3 로컬 스토리지 활용

```javascript
// 게임 상태 저장 (새로고침 대비)
const saveGameState = (state) => {
  localStorage.setItem('gameState', JSON.stringify({
    gameId: state.gameId,
    roomId: state.roomId,
    currentRound: state.currentRound,
    timestamp: Date.now()
  }));
};

// 게임 상태 복구
const restoreGameState = () => {
  const saved = localStorage.getItem('gameState');
  if (!saved) return null;
  
  const state = JSON.parse(saved);
  const age = Date.now() - state.timestamp;
  
  // 5분 이내의 상태만 복구
  if (age > 5 * 60 * 1000) {
    localStorage.removeItem('gameState');
    return null;
  }
  
  return state;
};
```

---

## 7. 체크리스트

프론트엔드 연동 시 확인해야 할 사항:

### 게임 시작 전
- [ ] WebSocket 연결 성공
- [ ] 게임방 채널 구독 완료
- [ ] JWT 토큰 유효성 확인
- [ ] 플레이어 목록 수신

### 게임 진행 중
- [ ] 타이머 동기화 확인
- [ ] 라운드 정보 수신
- [ ] 정답 제출 성공
- [ ] 제출 알림 수신
- [ ] 라운드 결과 수신

### 게임 종료 후
- [ ] 최종 결과 수신
- [ ] 모든 구독 해제
- [ ] 메모리 정리
- [ ] 로비로 정상 복귀

---

## 8. 추가 리소스

- [REST API 가이드](./ROADVIEW_SOLO_API_GUIDE.md)
- [WebSocket 가이드](./ROADVIEW_SOLO_WEBSOCKET_GUIDE.md)
- [에러 코드 가이드](#) (별도 작성 필요)
- [테스트 가이드](#) (별도 작성 필요)

---

## 9. 문의 및 지원

문제가 발생하거나 추가 정보가 필요한 경우:
- GitHub Issues 등록
- 백엔드 팀에게 문의
- Slack #frontend-backend 채널

