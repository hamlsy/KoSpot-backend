# PR: 멀티 결과창 상대 화면 상태 실시간 동기화 기획/명세 정리

## 배경
- 멀티플레이 종료 후 Result View에 남아있는 사용자는 상대가 Room View로 복귀했는지 즉시 알기 어려웠다.
- 기존 구조는 playerList를 Redis + WebSocket으로 이미 운영하고 있어, 별도 상태 채널 신설보다 기존 채널 확장이 유지보수/운영 측면에서 유리했다.

## 이번 PR에서 한 작업
- 화면 상태 동기화 전략을 **전용 채널 분리안**에서 **playerList 통합안**으로 재정의했다.
- 실제 코드(`GameRoomRedisRepository`, `GameRoomEventHandler`, `GameRoomPlayerInfo`)를 재검토해 계획의 현실성을 다시 맞췄다.
- 프론트(Vue.js) 연동 관점에서 필요한 전송/수신/스토어 병합 규칙을 명세화했다.

## 산출물
- 실행 계획서 업데이트: `docs/pr/Multi_ResultView_ScreenState_RealTime_Execution_Plan.md`
- 프론트 연동 명세서: `docs/frontend-integration/MULTI_RESULT_SCREEN_STATE_WEBSOCKET_GUIDE.md`

## 핵심 설계 결정
- 상태 데이터 모델은 `GameRoomPlayerInfo` 확장으로 처리
  - `screenState`
  - `screenStateSeq`
  - `screenStateUpdatedAt`
- 상태 변경 이벤트는 기존 채널 `/topic/room/{roomId}/playerList`로 전파
  - delta: `SCREEN_STATE_UPDATED`
  - full sync: `PLAYER_LIST_UPDATED` (복구용 유지)
- 정합성은 `screenStateSeq` 기반으로 보장
  - 역전(seq 낮음) 이벤트 drop
  - 동일 seq 멱등 처리

## 코드 재검토로 반영된 리스크
- 현재 Redis 저장은 `memberId -> playerJson` 문자열 구조이며 조건부 갱신이 없다.
- 따라서 멀티 인스턴스 환경에서 read-modify-write만으로는 seq 역전 위험이 있다.
- 대응: `GameRoomRedisRepository`에 Lua 기반 원자 갱신(CAS 성격) 추가를 P0 핵심 작업으로 명시했다.

## Vue.js 프론트 반영 포인트
- Pinia store에서 `PLAYER_LIST_UPDATED`와 `SCREEN_STATE_UPDATED`를 분리 처리
- 멤버 단위 seq 비교 후 병합
- 전송 타이밍 표준화
  - Result View 진입: `RESULT`
  - Room 복귀 액션/진입: `ROOM`

## 기대 효과
- 결과창에서 상대 복귀 여부를 실시간으로 표시 가능
- 기존 playerList 인프라 재사용으로 구현 범위/리스크 축소
- delta + full sync 병행으로 실시간성과 복구력을 함께 확보

## 범위 밖 (이번 PR에서 미구현)
- 실제 Java 코드 구현(UseCase/Repository/Lua/Notification 타입 추가)
- Vue 코드 반영(Pinia, 컴포넌트, 전송 훅)
- 통합 테스트/부하 테스트 실행

## 다음 작업 제안
1. 백엔드 P0 구현: `GameRoomPlayerInfo` 확장 + 상태 업데이트 UseCase + delta 브로드캐스트
2. Redis 원자 갱신(Lua) 적용 및 stale-drop 메트릭 추가
3. Vue store/Result View 연동 후 2인 멀티 E2E 검증
