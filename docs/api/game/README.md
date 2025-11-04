# Game API Documentation

로드뷰 게임(싱글 플레이) 관련 API 문서입니다.

## 📋 목차

- [연습 모드](#연습-모드)
- [랭크 모드](#랭크-모드)
- [게임 기록 조회](#게임-기록-조회)

---

## 연습 모드

### 1. 로드뷰 연습 게임 시작
**POST** `/roadView/practice/start`

로드뷰 연습 게임을 시작합니다. 원하는 지역(시도)을 선택하여 플레이할 수 있습니다.

**Headers**
```
Authorization: Bearer {access_token}
Content-Type: application/json
```

**Query Parameters**
| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| sido | String | O | 시도 코드 (예: SEOUL, BUSAN) |

**Response**
```json
{
  "isSuccess": true,
  "code": "2000",
  "message": "OK",
  "data": {
    "gameId": "1",
    "targetLat": "37.5665",
    "targetLng": "126.9780",
    "markerImageUrl": "https://example.com/marker.png"
  }
}
```

**응답 필드 설명**
- `gameId`: 게임 ID (암호화됨)
- `targetLat`: 목표 위도 (암호화됨)
- `targetLng`: 목표 경도 (암호화됨)
- `markerImageUrl`: 마커 이미지 URL

---

### 2. 로드뷰 연습 게임 종료
**POST** `/roadView/practice/end`

로드뷰 연습 게임을 종료하고 결과를 조회합니다.

**Headers**
```
Authorization: Bearer {access_token}
Content-Type: application/json
```

**Request Body**
```json
{
  "gameId": 1,
  "submittedLat": 37.5665,
  "submittedLng": 126.9780,
  "answerTime": 45.5
}
```

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| gameId | Long | O | 게임 ID |
| submittedLat | Double | O | 사용자가 제출한 위도 |
| submittedLng | Double | O | 사용자가 제출한 경도 |
| answerTime | Double | O | 답변 시간 (초) |

**Response**
```json
{
  "isSuccess": true,
  "code": "2000",
  "message": "OK",
  "data": {
    "score": 95.5
  }
}
```

**응답 필드 설명**
- `score`: 게임 점수 (0~100)

---

## 랭크 모드

### 1. 로드뷰 랭크 게임 시작
**POST** `/roadView/rank/start`

로드뷰 랭크 게임을 시작합니다. 랭크 게임은 랜덤 지역에서 진행됩니다.

**Headers**
```
Authorization: Bearer {access_token}
```

**Response**
```json
{
  "isSuccess": true,
  "code": "2000",
  "message": "OK",
  "data": {
    "gameId": "1",
    "targetLat": "37.5665",
    "targetLng": "126.9780",
    "markerImageUrl": "https://example.com/marker.png"
  }
}
```

---

### 2. 로드뷰 랭크 게임 종료
**POST** `/roadView/rank/end`

로드뷰 랭크 게임을 종료하고 결과 및 랭크 변동을 조회합니다.

**Headers**
```
Authorization: Bearer {access_token}
Content-Type: application/json
```

**Request Body**
```json
{
  "gameId": 1,
  "submittedLat": 37.5665,
  "submittedLng": 126.9780,
  "answerTime": 45.5
}
```

**Response**
```json
{
  "isSuccess": true,
  "code": "2000",
  "message": "OK",
  "data": {
    "score": 95.5,
    "previousRatingScore": 1500,
    "currentRatingScore": 1520,
    "ratingScoreChange": 20,
    "previousRankTier": "GOLD",
    "previousRankLevel": "LEVEL_3",
    "currentRankTier": "GOLD",
    "currentRankLevel": "LEVEL_2"
  }
}
```

**응답 필드 설명**
- `score`: 게임 점수
- `previousRatingScore`: 이전 레이팅 점수
- `currentRatingScore`: 현재 레이팅 점수
- `ratingScoreChange`: 레이팅 점수 변화량
- `previousRankTier`: 이전 랭크 티어
- `previousRankLevel`: 이전 랭크 레벨
- `currentRankTier`: 현재 랭크 티어
- `currentRankLevel`: 현재 랭크 레벨

**랭크 티어 목록**
- `BRONZE`: 브론즈
- `SILVER`: 실버
- `GOLD`: 골드
- `PLATINUM`: 플래티넘
- `DIAMOND`: 다이아몬드
- `MASTER`: 마스터
- `GRANDMASTER`: 그랜드마스터
- `CHALLENGER`: 챌린저

---

## 게임 기록 조회

### 1. 로드뷰 메인 페이지 조회
**GET** `/roadView/history/recent`

로드뷰 메인 페이지에 필요한 정보를 조회합니다.
- 현재 랭크 정보 (티어, 레벨, 레이팅 점수, 상위 퍼센트)
- 통계 정보 (총 플레이 수, 최고 점수)
- 최근 3개 게임 기록

**Headers**
```
Authorization: Bearer {access_token}
```

**Response**
```json
{
  "isSuccess": true,
  "code": "2000",
  "message": "OK",
  "data": {
    "rankInfo": {
      "rankTier": "GOLD",
      "rankLevel": "LEVEL_2",
      "ratingScore": 1520,
      "rankPercentage": 25.5
    },
    "statisticInfo": {
      "totalPlayCount": 50,
      "bestScore": 98.5
    },
    "recentGames": [
      {
        "gameId": 1,
        "poiName": "서울역",
        "answerDistance": 150.5,
        "score": 95.5,
        "answerTime": 45.5,
        "playedAt": "2025-01-01T10:00:00",
        "gameType": "RANK",
        "practiceSido": null
      }
    ]
  }
}
```

**응답 필드 설명**
- `rankInfo`: 랭크 정보
  - `rankTier`: 현재 티어
  - `rankLevel`: 현재 레벨
  - `ratingScore`: 레이팅 점수
  - `rankPercentage`: 상위 퍼센트 (0~100)
- `statisticInfo`: 통계 정보
  - `totalPlayCount`: 총 플레이 수 (연습 + 랭크)
  - `bestScore`: 최고 점수
- `recentGames`: 최근 3개 게임 기록
  - `gameType`: 게임 타입 (PRACTICE, RANK)
  - `practiceSido`: 연습 모드인 경우 선택한 시도

---

### 2. 로드뷰 게임 전체 기록 조회
**GET** `/roadView/history`

로드뷰 게임의 전체 완료된 기록을 페이지네이션으로 조회합니다.

**Headers**
```
Authorization: Bearer {access_token}
```

**Query Parameters**
| 필드 | 타입 | 필수 | 기본값 | 설명 |
|------|------|------|--------|------|
| page | Integer | X | 0 | 페이지 번호 (0부터 시작) |
| size | Integer | X | 10 | 페이지 크기 |

**Response**
```json
{
  "isSuccess": true,
  "code": "2000",
  "message": "OK",
  "data": {
    "games": [
      {
        "gameId": 1,
        "poiName": "서울역",
        "answerDistance": 150.5,
        "score": 95.5,
        "answerTime": 45.5,
        "playedAt": "2025-01-01T10:00:00",
        "gameType": "RANK",
        "practiceSido": null
      }
    ],
    "currentPage": 0,
    "totalPages": 5,
    "totalElements": 50,
    "size": 10
  }
}
```

---

## 테스트 API

### 점수 계산 테스트
**GET** `/roadView/scoreTest/{distance}`

거리를 기반으로 점수를 계산하는 테스트 API입니다.

**Path Parameters**
| 필드 | 타입 | 설명 |
|------|------|------|
| distance | Double | 거리 (미터) |

### 좌표 암호화 테스트
**GET** `/roadView/encrypt/{lat}`

위도를 암호화하는 테스트 API입니다.

**Path Parameters**
| 필드 | 타입 | 설명 |
|------|------|------|
| lat | String | 위도 |

