# KoSpot Backend API Documentation

KoSpot 백엔드 API 전체 문서입니다. 프론트엔드 개발자가 쉽게 API를 이해하고 연동할 수 있도록 작성되었습니다.

## 📚 도메인별 API 문서

### 🔐 인증 & 회원
- [Auth API](./auth/README.md) - 인증 (로그인, 토큰 재발급, 로그아웃)
- [Member API](./member/README.md) - 회원 정보 및 프로필

### 🎮 게임
- [Game API](./game/README.md) - 로드뷰 게임 (싱글 플레이)
- [GameRank API](./gamerank/README.md) - 게임 랭크 시스템
- [Multi Game API](./multi/README.md) - 멀티플레이 게임

### 🏪 아이템 & 포인트
- [Item API](./item/README.md) - 아이템 상점
- [MemberItem API](./memberitem/README.md) - 사용자 인벤토리
- [Point API](./point/README.md) - 포인트 기록

### 📍 좌표 & 지역
- [Coordinate API](./coordinate/README.md) - 좌표 조회

### 📢 공지사항 & 배너
- [Notice API](./notice/README.md) - 공지사항
- [Banner API](./banner/README.md) - 배너

### 🏠 메인 페이지
- [Main Page API](./main/README.md) - 메인 페이지 정보

### 🖼️ 이미지
- [Image API](./image/README.md) - 이미지 업로드/수정

### 👑 관리자
- [Admin API](./admin/README.md) - 관리자 전용 API

---

## 🚀 빠른 시작

### 기본 정보

**Base URL**
```
http://localhost:8080
```

**응답 형식**
모든 API는 다음과 같은 공통 응답 형식을 사용합니다:

```json
{
  "isSuccess": true,
  "code": "2000",
  "message": "OK",
  "data": { }
}
```

### 인증

대부분의 API는 JWT 토큰을 사용한 인증이 필요합니다.

**헤더에 토큰 포함**
```
Authorization: Bearer {access_token}
```

**토큰 획득 방법**
1. 테스트: `GET /auth/tempLogin/{username}`
2. 실제: OAuth2 로그인 (카카오, 구글 등) - 별도 문서 참조

**토큰 재발급**
```
POST /auth/reIssue
{
  "refreshToken": "your_refresh_token"
}
```

---

## 📖 API 사용 흐름

### 1. 메인 페이지 진입
```
GET /main
```
→ 게임 모드 활성화 상태, 공지사항, 배너 정보 획득

### 2. 싱글 게임 플레이

**연습 모드**
```
1. POST /roadView/practice/start?sido=SEOUL
2. (게임 플레이)
3. POST /roadView/practice/end
```

**랭크 모드**
```
1. POST /roadView/rank/start
2. (게임 플레이)
3. POST /roadView/rank/end
```

### 3. 멀티 게임 플레이

**방 생성 및 참여**
```
1. GET /rooms?page=0 (방 목록 조회)
2. POST /rooms (방 생성) 또는 POST /rooms/{roomId}/players (방 참여)
3. WebSocket 연결: ws://localhost:8080/ws
4. 구독: /topic/room/{roomId}/playerList
```

**게임 시작**
```
1. POST /rooms/{roomId}/roadview/games/solo (게임 시작)
2. POST /rooms/{roomId}/games/{gameId}/rounds/{roundId}/submissions/player (정답 제출)
3. POST /rooms/{roomId}/roadview/games/{gameId}/rounds (다음 라운드)
```

### 4. 아이템 구매 및 장착
```
1. GET /item/MARKER (아이템 목록 조회)
2. GET /memberItem/{itemId}/purchase (아이템 구매)
3. GET /memberItem/{memberItemId} (아이템 장착)
```

### 5. 프로필 조회
```
GET /member/profile
```
→ 회원 정보, 게임 통계, 랭크 정보 획득

---

## 🔑 주요 개념

### 게임 모드
- **ROADVIEW**: 로드뷰 모드 (구글 스트리트뷰 스타일)
- **PHOTO**: 포토 모드 (사진으로 위치 맞추기)

### 게임 타입
- **PRACTICE**: 연습 모드 (랭크 영향 없음)
- **RANK**: 랭크 모드 (랭크 점수 변동)

### 멀티 게임 매치 타입
- **SOLO**: 개인전 (1 vs 1 vs 1 ...)
- **TEAM**: 팀전 (팀 vs 팀)

### 아이템 타입
- **MARKER**: 마커 스킨
- **PROFILE**: 프로필 아이템
- **EFFECT**: 이펙트
- **THEME**: 테마

### 랭크 티어
1. **BRONZE** (브론즈) - 0~999
2. **SILVER** (실버) - 1000~1499
3. **GOLD** (골드) - 1500~1999
4. **PLATINUM** (플래티넘) - 2000~2499
5. **DIAMOND** (다이아몬드) - 2500~2999
6. **MASTER** (마스터) - 3000~3499
7. **GRANDMASTER** (그랜드마스터) - 3500~3999
8. **CHALLENGER** (챌린저) - 4000+

### 방 상태
- **WAITING**: 대기 중
- **PLAYING**: 게임 중
- **FINISHED**: 종료됨

---

## ❗ 에러 코드

### 공통 에러
| 코드 | 설명 |
|------|------|
| 2000 | 성공 |
| 4000 | 잘못된 요청 |
| 4001 | 인증 실패 (유효하지 않은 토큰) |
| 4002 | 만료된 토큰 |
| 4003 | 권한 없음 |
| 4004 | 리소스를 찾을 수 없음 |
| 5000 | 서버 에러 |

### 도메인별 상세 에러 코드
각 도메인 문서를 참조하세요.

---

## 📝 참고사항

### Pagination
페이징이 지원되는 API는 다음 파라미터를 사용합니다:
- `page`: 페이지 번호 (0부터 시작)
- `size`: 페이지 크기 (기본값은 API마다 다름)
- `sort`: 정렬 기준 (예: `createdAt,DESC`)

### File Upload
파일 업로드가 필요한 API는 `multipart/form-data` 형식을 사용합니다.

### WebSocket
실시간 통신이 필요한 기능(멀티 게임, 채팅)은 WebSocket을 사용합니다.
- 엔드포인트: `ws://localhost:8080/ws`
- 프로토콜: STOMP over SockJS
- 인증: Connection 시 JWT 토큰 필요

---

## 🛠️ 개발 도구

### Swagger UI
API를 테스트하고 문서를 확인할 수 있습니다:
```
http://localhost:8080/swagger-ui.html
```

### Postman Collection
(추후 제공 예정)

---

## 📞 문의

API 문서에 대한 질문이나 피드백은 백엔드 팀에 문의해주세요.

---

## 🔄 변경 이력

### v1.0.0 (2025-01-01)
- 초기 API 문서 작성
- 모든 도메인 API 문서화 완료

---

**Happy Coding! 🚀**

