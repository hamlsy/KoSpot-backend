# Member API Documentation

회원 관련 API 문서입니다.

---

## 1. 내 정보 조회
**GET** `/member/profile`

회원의 프로필과 게임 통계, 랭킹, 아이템 정보를 조회합니다.

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
    "nickname": "홍길동",
    "email": "user@example.com",
    "profileImageUrl": "https://example.com/profile.jpg",
    "currentPoint": 1000,
    "joinedAt": "2025-01-01T10:00:00",
    "lastPlayedAt": "2025-01-10T15:30:00",
    "currentStreak": 5,
    "statistics": {
      "roadView": {
        "practice": {
          "totalGames": 10,
          "averageScore": 85.5
        },
        "rank": {
          "totalGames": 5,
          "averageScore": 90.0
        },
        "multi": {
          "totalGames": 3,
          "averageScore": 88.0,
          "firstPlaceCount": 1,
          "secondPlaceCount": 1,
          "thirdPlaceCount": 1
        }
      },
      "photo": {
        "practice": {
          "totalGames": 8,
          "averageScore": 80.0
        },
        "rank": {
          "totalGames": 4,
          "averageScore": 85.0
        },
        "multi": {
          "totalGames": 2,
          "averageScore": 83.0,
          "firstPlaceCount": 0,
          "secondPlaceCount": 1,
          "thirdPlaceCount": 1
        }
      },
      "bestScore": 95.5
    },
    "rankInfo": {
      "roadViewRank": {
        "tier": "GOLD",
        "level": "LEVEL_2",
        "ratingScore": 1520
      },
      "photoRank": {
        "tier": "SILVER",
        "level": "LEVEL_3",
        "ratingScore": 1350
      }
    }
  }
}
```

**응답 필드 설명**
- `nickname`: 닉네임
- `email`: 이메일
- `profileImageUrl`: 프로필 이미지 URL
- `currentPoint`: 현재 포인트
- `joinedAt`: 가입일시
- `lastPlayedAt`: 마지막 플레이 일시
- `currentStreak`: 현재 연속 플레이
- `statistics`: 게임 통계
  - `roadView`: 로드뷰 통계
    - `practice`: 연습 모드 통계
    - `rank`: 랭크 모드 통계
    - `multi`: 멀티플레이 통계
  - `photo`: 포토 통계 (구조 동일)
  - `bestScore`: 최고 점수
- `rankInfo`: 랭크 정보
  - `roadViewRank`: 로드뷰 랭크
  - `photoRank`: 포토 랭크

---

## 2. 상점 내 정보 조회
**GET** `/member/shop-info`

상점 페이지에서 필요한 내 포인트, 장착 아이템, 보유 아이템을 한 번에 조회합니다.

**Headers**
```
Authorization: Bearer {access_token}
```

**Response**
```json
{
  "isSuccess": true,
  "code": 2000,
  "message": "성공입니다.",
  "result": {
    "currentPoint": 1200,
    "equippedItems": [
      {
        "memberItemId": 10,
        "itemType": "MARKER",
        "itemImageUrl": "https://example.com/items/marker-basic.png",
        "name": "기본 마커",
        "description": "기본 제공 마커",
        "equipped": true,
        "purchaseTime": "2026-02-10T09:30:00"
      }
    ],
    "ownedItems": [
      {
        "memberItemId": 10,
        "itemType": "MARKER",
        "itemImageUrl": "https://example.com/items/marker-basic.png",
        "name": "기본 마커",
        "description": "기본 제공 마커",
        "equipped": true,
        "purchaseTime": "2026-02-10T09:30:00"
      },
      {
        "memberItemId": 15,
        "itemType": "MARKER_EFFECT",
        "itemImageUrl": "https://example.com/items/effect-fire.png",
        "name": "불꽃 이펙트",
        "description": "마커에 불꽃 효과를 추가합니다.",
        "equipped": false,
        "purchaseTime": "2026-02-11T14:00:00"
      }
    ]
  }
}
```

**응답 필드 설명**
- `currentPoint`: 현재 보유 포인트
- `equippedItems`: 현재 장착 중인 아이템 목록
- `ownedItems`: 현재 보유 중인 아이템 전체 목록
- 아이템 공통 필드
  - `memberItemId`: 보유 아이템 식별자
  - `itemType`: 아이템 타입 (`MARKER`, `MARKER_EFFECT`)
  - `itemImageUrl`: 아이템 이미지 URL
  - `name`: 아이템 이름
  - `description`: 아이템 설명
  - `equipped`: 장착 여부
  - `purchaseTime`: 구매 일시

---

## 3. 테스트용 멤버 조회
**GET** `/member/me`

현재 로그인한 사용자의 username을 조회합니다.

> ⚠️ **주의**: 이 API는 테스트 용도입니다.

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
  "data": "user123"
}
```

