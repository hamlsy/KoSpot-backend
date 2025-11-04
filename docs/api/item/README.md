# Item API Documentation

아이템(상점) 관련 API 문서입니다. 관리자 권한이 필요한 API가 포함되어 있습니다.

## 📋 목차

- [아이템 조회](#아이템-조회)
- [아이템 관리 (관리자)](#아이템-관리-관리자)

---

## 아이템 조회

### 아이템 타입별 조회
**GET** `/item/{itemTypeKey}`

타입별 아이템 목록을 조회합니다.

**Headers**
```
Authorization: Bearer {access_token}
```

**Path Parameters**
| 필드 | 타입 | 설명 |
|------|------|------|
| itemTypeKey | String | 아이템 타입 코드 |

**아이템 타입 목록**
| 코드 | 설명 |
|------|------|
| MARKER | 마커 스킨 |
| PROFILE | 프로필 아이템 |
| EFFECT | 이펙트 |
| THEME | 테마 |

**Response**
```json
{
  "isSuccess": true,
  "code": "2000",
  "message": "OK",
  "data": [
    {
      "itemId": 1,
      "name": "기본 마커",
      "description": "기본 마커 스킨",
      "price": 100,
      "stock": 999,
      "imageUrl": "https://example.com/marker1.png",
      "isOwned": false
    }
  ]
}
```

**응답 필드 설명**
- `itemId`: 아이템 ID
- `name`: 아이템 이름
- `description`: 아이템 설명
- `price`: 가격 (포인트)
- `stock`: 재고 수량
- `imageUrl`: 아이템 이미지 URL
- `isOwned`: 현재 사용자의 소유 여부

---

## 아이템 관리 (관리자)

> ⚠️ **주의**: 다음 API들은 관리자 권한(ROLE_ADMIN)이 필요합니다.

### 1. 아이템 등록
**POST** `/item/`

새로운 아이템을 등록합니다.

**Headers**
```
Authorization: Bearer {access_token}
Content-Type: multipart/form-data
```

**Request Body (Form Data)**
| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| name | String | O | 아이템 이름 |
| image | File | O | 아이템 이미지 파일 |
| description | String | O | 아이템 설명 |
| price | Integer | O | 가격 (포인트) |
| itemTypeKey | String | O | 아이템 타입 코드 |
| quantity | Integer | O | 수량 |

**Response**
```json
{
  "isSuccess": true,
  "code": "2000",
  "message": "OK"
}
```

---

### 2. 아이템 정보 업데이트
**PUT** `/item/info`

아이템 정보를 업데이트합니다.

**Headers**
```
Authorization: Bearer {access_token}
Content-Type: application/json
```

**Request Body**
```json
{
  "itemId": 1,
  "name": "프리미엄 마커",
  "description": "프리미엄 마커 스킨",
  "price": 200,
  "itemTypeKey": "MARKER",
  "quantity": 100
}
```

**Response**
```json
{
  "isSuccess": true,
  "code": "2000",
  "message": "OK"
}
```

---

### 3. 아이템 상점 삭제
**PUT** `/item/{id}/deleteShop`

아이템을 상점에서 삭제합니다 (소프트 삭제).

**Headers**
```
Authorization: Bearer {access_token}
```

**Path Parameters**
| 필드 | 타입 | 설명 |
|------|------|------|
| id | Long | 아이템 ID |

**Response**
```json
{
  "isSuccess": true,
  "code": "2000",
  "message": "OK"
}
```

---

### 4. 아이템 상점 재등록
**PUT** `/item/{id}/restoreShop`

삭제된 아이템을 상점에 재등록합니다.

**Headers**
```
Authorization: Bearer {access_token}
```

**Path Parameters**
| 필드 | 타입 | 설명 |
|------|------|------|
| id | Long | 아이템 ID |

**Response**
```json
{
  "isSuccess": true,
  "code": "2000",
  "message": "OK"
}
```

---

### 5. 아이템 삭제
**DELETE** `/item/{id}`

아이템을 완전히 삭제합니다 (하드 삭제).

**Headers**
```
Authorization: Bearer {access_token}
```

**Path Parameters**
| 필드 | 타입 | 설명 |
|------|------|------|
| id | Long | 아이템 ID |

**Response**
```json
{
  "isSuccess": true,
  "code": "2000",
  "message": "OK"
}
```

