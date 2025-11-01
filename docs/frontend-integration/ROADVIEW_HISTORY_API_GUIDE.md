# 로드뷰 게임 기록 조회 API 가이드

## 1. 로드뷰 메인 페이지 (최근 3개 기록 + 랭크 정보)

**주요 용도**: 로드뷰 메인 페이지(연습/랭크/테마 선택 화면)에서 사용

### 요청
```
GET /roadView/history/recent
Authorization: Bearer {JWT_TOKEN}
```

### 응답
```json
{
  "isSuccess": true,
  "code": "2000",
  "message": "OK",
  "result": {
    "rankInfo": {
      "rankTier": "SILVER",
      "rankLevel": "TWO",
      "ratingScore": 1650,
      "rankPercentage": 23.5
    },
    "statisticInfo": {
      "totalPlayCount": 47,
      "bestScore": 978.5
    },
    "recentGames": [
      {
        "gameId": 123,
        "poiName": "N서울타워",
        "answerDistance": 150.5,
        "score": 945.2,
        "answerTime": 12.5,
        "playedAt": "2025-11-01T14:30:00",
        "gameType": "RANK",
        "practiceSido": null
      },
      {
        "gameId": 122,
        "poiName": "광화문광장",
        "answerDistance": 2500.0,
        "score": 450.0,
        "answerTime": 45.3,
        "playedAt": "2025-11-01T13:15:00",
        "gameType": "PRACTICE",
        "practiceSido": "SEOUL"
      },
      {
        "gameId": 121,
        "poiName": "부산 해운대",
        "answerDistance": 5000.0,
        "score": 320.0,
        "answerTime": 60.0,
        "playedAt": "2025-11-01T12:00:00",
        "gameType": "PRACTICE",
        "practiceSido": "BUSAN"
      }
    ]
  }
}
```

### 필드 설명

#### rankInfo (랭크 정보)
- `rankTier`: 현재 티어
  - 가능한 값: `BRONZE`, `SILVER`, `GOLD`, `PLATINUM`, `DIAMOND`, `MASTER`
- `rankLevel`: 현재 레벨
  - 가능한 값: `ONE`, `TWO`, `THREE`, `FOUR`, `FIVE`
- `ratingScore`: 현재 레이팅 점수
- `rankPercentage`: 전체 랭킹 중 상위 몇 퍼센트 (소수점 첫째 자리)
  - 예: 23.5 = 상위 23.5%

#### statisticInfo (통계 정보)
- `totalPlayCount`: 총 플레이 수 (연습 + 랭크)
- `bestScore`: 최고 점수

#### recentGames (최근 3개 기록)
- `gameId`: 게임 ID
- `poiName`: 정답 지역 이름
- `answerDistance`: 정답과의 거리 (미터)
- `score`: 획득 점수
- `answerTime`: 답변까지 걸린 시간 (초)
- `playedAt`: 플레이한 시간
- `gameType`: `RANK` (랭크 게임) 또는 `PRACTICE` (연습 게임)
- `practiceSido`: 연습 게임인 경우 지역 (랭크는 null)
  - 가능한 값: `SEOUL`, `BUSAN`, `DAEGU`, `INCHEON`, `GWANGJU`, `DAEJEON`, `ULSAN`, `SEJONG`, `GYEONGGI`, `GANGWON`, `CHUNGBUK`, `CHUNGNAM`, `JEONBUK`, `JEONNAM`, `GYEONGBUK`, `GYEONGNAM`, `JEJU`

---

## 2. 전체 기록 조회 (페이지네이션)

### 요청
```
GET /roadView/history?page=0&size=10
Authorization: Bearer {JWT_TOKEN}
```

### 쿼리 파라미터
- `page`: 페이지 번호 (0부터 시작, 기본값: 0)
- `size`: 페이지 크기 (기본값: 10)

### 응답
```json
{
  "isSuccess": true,
  "code": "2000",
  "message": "OK",
  "result": {
    "games": [
      {
        "gameId": 123,
        "poiName": "N서울타워",
        "answerDistance": 150.5,
        "score": 945.2,
        "answerTime": 12.5,
        "playedAt": "2025-11-01T14:30:00",
        "gameType": "RANK",
        "practiceSido": null
      }
    ],
    "currentPage": 0,
    "totalPages": 5,
    "totalElements": 47,
    "size": 10
  }
}
```

### 페이지네이션 필드
- `currentPage`: 현재 페이지 번호 (0부터 시작)
- `totalPages`: 전체 페이지 수
- `totalElements`: 전체 기록 수
- `size`: 페이지 크기

---

## 3. 랭크 게임 종료 (업데이트됨)

### 요청
```
POST /roadView/rank/end
Authorization: Bearer {JWT_TOKEN}
Content-Type: application/json

{
  "gameId": 123,
  "submittedLat": 37.5665,
  "submittedLng": 126.9780,
  "answerTime": 15.5
}
```

### 응답 (수정됨)
```json
{
  "isSuccess": true,
  "code": "2000",
  "message": "OK",
  "result": {
    "score": 945.2,
    "previousRatingScore": 1250,
    "currentRatingScore": 1320,
    "ratingScoreChange": 70,
    "previousRankTier": "SILVER",
    "previousRankLevel": "TWO",
    "currentRankTier": "SILVER",
    "currentRankLevel": "ONE"
  }
}
```

**참고**: 게임 종료 후 포인트가 자동으로 지급됩니다. (비동기 처리)
- 획득 포인트 = `[(점수 × 0.2) + 50] × 티어배율`
- 예: SILVER 티어에서 945점 = `[(945 × 0.2) + 50] × 1.2` = **294P**

### 추가된 필드 (프론트엔드 애니메이션용)
- `previousRatingScore`: 게임 전 랭크 점수
- `currentRatingScore`: 게임 후 랭크 점수
- `ratingScoreChange`: 변동 포인트 (양수: 상승, 음수: 하락)
- `previousRankTier`: 게임 전 티어
  - 가능한 값: `BRONZE`, `SILVER`, `GOLD`, `PLATINUM`, `DIAMOND`, `MASTER`
- `previousRankLevel`: 게임 전 레벨
  - 가능한 값: `ONE`, `TWO`, `THREE`, `FOUR`, `FIVE`
- `currentRankTier`: 게임 후 티어
- `currentRankLevel`: 게임 후 레벨

### 애니메이션 처리 예시
```javascript
// 티어 승급 체크
const isTierUp = currentRankTier !== previousRankTier;

// 포인트 변화 애니메이션
const pointDiff = ratingScoreChange;
animateNumber(previousRatingScore, currentRatingScore, 1000); // 1초 동안

// 랭크 변화 애니메이션
if (isTierUp) {
  showTierUpAnimation(previousRankTier, currentRankTier);
}
```

---

## 랭크 시스템 정보

### 티어별 점수 구간
- `BRONZE`: 0 ~ 999
- `SILVER`: 1000 ~ 1999
- `GOLD`: 2000 ~ 2999
- `PLATINUM`: 3000 ~ 3999
- `DIAMOND`: 4000 ~ 4999
- `MASTER`: 5000 ~

### 레벨별 점수 구간 (각 티어 내)
- `FIVE`: 0 ~ 199
- `FOUR`: 200 ~ 399
- `THREE`: 400 ~ 599
- `TWO`: 600 ~ 799
- `ONE`: 800 ~ 999

예: SILVER TWO = 1600 ~ 1799점

---

## 4. 연습 게임 종료

### 요청
```
POST /roadView/practice/end
Authorization: Bearer {JWT_TOKEN}
Content-Type: application/json

{
  "gameId": 124,
  "submittedLat": 37.5665,
  "submittedLng": 126.9780,
  "answerTime": 18.2
}
```

### 응답
```json
{
  "isSuccess": true,
  "code": "2000",
  "message": "OK",
  "result": {
    "score": 756.3
  }
}
```

**참고**: 게임 종료 후 포인트가 자동으로 지급됩니다. (비동기 처리)
- 획득 포인트 = `(점수 × 0.08) + 30`
- 예: 756점 = `(756 × 0.08) + 30` = **90P**

---

## 포인트 지급 시스템

### 연습 게임
- 공식: `(점수 × 0.08) + 30`
- 범위: 30P ~ 110P

| 점수   | 포인트 |
|-------|--------|
| 0점   | 30P    |
| 500점 | 70P    |
| 1000점| 110P   |

### 랭크 게임
- 공식: `[(점수 × 0.2) + 50] × 티어배율`
- 티어 배율: Bronze 1.0, Silver 1.2, Gold 1.5, Platinum 1.8, Diamond 2.0, Master 2.5

| 티어 | 0점 | 500점 | 1000점 |
|------|-----|-------|--------|
| Bronze | 50P | 150P | 250P |
| Silver | 60P | 180P | 300P |
| Gold | 75P | 225P | 375P |
| Platinum | 90P | 270P | 450P |
| Diamond | 100P | 300P | 500P |
| Master | 125P | 375P | 625P |

---

## 참고사항

1. **완료된 게임만 조회됨**: `gameStatus`가 `COMPLETED`인 게임만 조회됩니다.
2. **최신순 정렬**: 모든 기록은 플레이 시간 기준 최신순으로 정렬됩니다.
3. **N+1 방지**: fetch join 적용되어 있어 성능 최적화되어 있습니다.
4. **인증 필수**: 모든 API는 JWT 인증이 필요합니다.
5. **포인트 자동 지급**: 게임 종료 시 비동기로 자동 지급됩니다.

