# MemberItem API Documentation

사용자 아이템(인벤토리) 관련 API 문서입니다.

## 📋 목차

- [아이템 구매](#아이템-구매)
- [아이템 장착](#아이템-장착)
- [인벤토리 조회](#인벤토리-조회)

---

## 아이템 구매

### 아이템 구매
**GET** `/memberItem/{itemId}/purchase`

상점에서 아이템을 구매합니다.

> 📝 **참고**: 향후 POST 메서드로 변경될 예정입니다.

**Headers**
```
Authorization: Bearer {access_token}
```

**Path Parameters**
| 필드 | 타입 | 설명 |
|------|------|------|
| itemId | Long | 아이템 ID |

**Response**
```json
{
  "isSuccess": true,
  "code": "2000",
  "message": "OK"
}
```

**참고사항**
- 포인트가 부족한 경우 에러가 반환됩니다.
- 구매 후 자동으로 인벤토리에 추가됩니다.

---

## 아이템 장착

### 아이템 장착
**GET** `/memberItem/{memberItemId}`

인벤토리에서 아이템을 장착합니다.

**Headers**
```
Authorization: Bearer {access_token}
```

**Path Parameters**
| 필드 | 타입 | 설명 |
|------|------|------|
| memberItemId | Long | 보유 아이템 ID |

**Response**
```json
{
  "isSuccess": true,
  "code": "2000",
  "message": "OK"
}
```

**참고사항**
- 같은 타입의 다른 아이템이 장착되어 있는 경우, 자동으로 해제됩니다.
- 장착된 아이템은 게임에서 즉시 적용됩니다.

---

## 인벤토리 조회

### 1. 타입별 아이템 조회
**GET** `/memberItem/{itemType}`

내 인벤토리에서 특정 타입의 아이템 목록을 조회합니다.

**Headers**
```
Authorization: Bearer {access_token}
```

**Path Parameters**
| 필드 | 타입 | 설명 |
|------|------|------|
| itemType | String | 아이템 타입 코드 (MARKER, PROFILE, EFFECT, THEME) |

**Response**
```json
{
  "isSuccess": true,
  "code": "2000",
  "message": "OK",
  "data": [
    {
      "memberItemId": 1,
      "name": "기본 마커",
      "description": "기본 마커 스킨",
      "isEquipped": true,
      "purchaseTime": "2025-01-01T10:00:00"
    },
    {
      "memberItemId": 2,
      "name": "프리미엄 마커",
      "description": "프리미엄 마커 스킨",
      "isEquipped": false,
      "purchaseTime": "2025-01-05T14:30:00"
    }
  ]
}
```

**응답 필드 설명**
- `memberItemId`: 보유 아이템 ID
- `name`: 아이템 이름
- `description`: 아이템 설명
- `isEquipped`: 장착 여부
- `purchaseTime`: 구매 일시

---

### 2. 전체 아이템 조회
**GET** `/memberItem/inventory`

내 인벤토리의 모든 아이템을 조회합니다.

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
  "data": [
    {
      "memberItemId": 1,
      "name": "기본 마커",
      "description": "기본 마커 스킨",
      "isEquipped": true,
      "purchaseTime": "2025-01-01T10:00:00"
    },
    {
      "memberItemId": 3,
      "name": "기본 프로필",
      "description": "기본 프로필 테두리",
      "isEquipped": true,
      "purchaseTime": "2025-01-01T10:00:00"
    }
  ]
}
```

**참고사항**
- 모든 타입의 아이템이 포함됩니다.
- 구매 순서대로 정렬됩니다.

