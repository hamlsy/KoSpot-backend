# Admin API Documentation

관리자 전용 API 문서입니다. 모든 API는 관리자 권한(ROLE_ADMIN)이 필요합니다.

## 📋 목차

- [배너 관리 API](#배너-관리-api)
- [좌표 관리 API](#좌표-관리-api)
- [게임 설정 관리 API](#게임-설정-관리-api)
- [회원 관리 API](#회원-관리-api)

---

## 배너 관리 API

### 1. 배너 생성
**POST** `/admin/banners`

관리자가 새로운 배너를 생성합니다.

**Headers**
```
Authorization: Bearer {access_token}
Content-Type: multipart/form-data
```

**Request Body (Form Data)**
| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| title | String | O | 배너 제목 |
| image | File | O | 배너 이미지 파일 |
| linkUrl | String | X | 배너 클릭 시 이동할 URL |
| description | String | X | 배너 설명 |
| displayOrder | Integer | O | 노출 순서 (숫자가 작을수록 먼저 노출) |

**Response**
```json
{
  "isSuccess": true,
  "code": "2000",
  "message": "OK",
  "data": 1
}
```

---

### 2. 배너 수정
**PUT** `/admin/banners/{bannerId}`

관리자가 배너 정보를 수정합니다.

**Headers**
```
Authorization: Bearer {access_token}
Content-Type: multipart/form-data
```

**Path Parameters**
| 필드 | 타입 | 설명 |
|------|------|------|
| bannerId | Long | 배너 ID |

**Request Body (Form Data)**
| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| title | String | O | 배너 제목 |
| image | File | X | 배너 이미지 파일 (변경 시만 전송) |
| linkUrl | String | X | 배너 클릭 시 이동할 URL |
| description | String | X | 배너 설명 |
| displayOrder | Integer | O | 노출 순서 |

**Response**
```json
{
  "isSuccess": true,
  "code": "2000",
  "message": "OK"
}
```

---

### 3. 배너 삭제
**DELETE** `/admin/banners/{bannerId}`

관리자가 배너를 삭제합니다.

**Headers**
```
Authorization: Bearer {access_token}
```

**Path Parameters**
| 필드 | 타입 | 설명 |
|------|------|------|
| bannerId | Long | 배너 ID |

**Response**
```json
{
  "isSuccess": true,
  "code": "2000",
  "message": "OK"
}
```

---

### 4. 배너 목록 조회
**GET** `/admin/banners`

관리자가 전체 배너 목록을 조회합니다.

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
      "bannerId": 1,
      "title": "신규 이벤트",
      "imageUrl": "https://example.com/banner1.jpg",
      "linkUrl": "https://example.com/event",
      "description": "신규 회원 가입 이벤트",
      "displayOrder": 1,
      "isActive": true,
      "createdAt": "2025-01-01T10:00:00",
      "updatedAt": "2025-01-01T10:00:00"
    }
  ]
}
```

---

### 5. 배너 활성화
**PUT** `/admin/banners/{bannerId}/activate`

관리자가 배너를 활성화합니다.

**Headers**
```
Authorization: Bearer {access_token}
```

**Path Parameters**
| 필드 | 타입 | 설명 |
|------|------|------|
| bannerId | Long | 배너 ID |

**Response**
```json
{
  "isSuccess": true,
  "code": "2000",
  "message": "OK"
}
```

---

### 6. 배너 비활성화
**PUT** `/admin/banners/{bannerId}/deactivate`

관리자가 배너를 비활성화합니다.

**Headers**
```
Authorization: Bearer {access_token}
```

**Path Parameters**
| 필드 | 타입 | 설명 |
|------|------|------|
| bannerId | Long | 배너 ID |

**Response**
```json
{
  "isSuccess": true,
  "code": "2000",
  "message": "OK"
}
```

---

## 좌표 관리 API

### 1. 좌표 생성
**POST** `/admin/coordinates`

관리자가 폼을 통해 새로운 좌표를 생성합니다.

**Headers**
```
Authorization: Bearer {access_token}
Content-Type: application/json
```

**Request Body**
```json
{
  "lat": 37.5665,
  "lng": 126.9780,
  "poiName": "서울역",
  "sidoKey": "SEOUL",
  "sigungu": "중구",
  "detailAddress": "서울역 광장",
  "locationType": "LANDMARK"
}
```

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| lat | Double | O | 위도 |
| lng | Double | O | 경도 |
| poiName | String | O | POI(관심지점) 이름 |
| sidoKey | String | O | 시도 코드 (예: SEOUL, BUSAN) |
| sigungu | String | O | 시군구 |
| detailAddress | String | O | 상세 주소 |
| locationType | String | O | 위치 타입 (LANDMARK, STREET, NATURE 등) |

**Response**
```json
{
  "isSuccess": true,
  "code": "2000",
  "message": "OK",
  "data": 1
}
```

---

### 2. 좌표 엑셀 업로드
**POST** `/admin/coordinates/import-excel`

관리자가 엑셀 파일을 통해 좌표를 일괄 등록합니다.

**Headers**
```
Authorization: Bearer {access_token}
Content-Type: multipart/form-data
```

**Request Body (Form Data)**
| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| file | File | O | 좌표 정보가 담긴 엑셀 파일 (.xlsx) |

**엑셀 파일 형식**
- 컬럼: lat, lng, poiName, sidoKey, sigungu, detailAddress, locationType

**Response**
```json
{
  "isSuccess": true,
  "code": "2000",
  "message": "OK"
}
```

---

### 3. 좌표 목록 조회
**GET** `/admin/coordinates`

관리자가 전체 좌표 목록을 페이징 조회합니다.

**Headers**
```
Authorization: Bearer {access_token}
```

**Query Parameters**
| 필드 | 타입 | 필수 | 기본값 | 설명 |
|------|------|------|--------|------|
| page | Integer | X | 0 | 페이지 번호 (0부터 시작) |
| size | Integer | X | 20 | 페이지 크기 |
| sort | String | X | createdAt,DESC | 정렬 기준 |

**Response**
```json
{
  "isSuccess": true,
  "code": "2000",
  "message": "OK",
  "data": {
    "content": [
      {
        "coordinateId": 1,
        "lat": 37.5665,
        "lng": 126.9780,
        "poiName": "서울역",
        "sido": "서울특별시",
        "sigungu": "중구",
        "detailAddress": "서울역 광장",
        "locationType": "LANDMARK"
      }
    ],
    "pageable": {
      "pageNumber": 0,
      "pageSize": 20
    },
    "totalElements": 1,
    "totalPages": 1,
    "last": true,
    "first": true
  }
}
```

---

### 4. 좌표 삭제
**DELETE** `/admin/coordinates/{coordinateId}`

관리자가 좌표를 삭제합니다.

**Headers**
```
Authorization: Bearer {access_token}
```

**Path Parameters**
| 필드 | 타입 | 설명 |
|------|------|------|
| coordinateId | Long | 좌표 ID |

**Response**
```json
{
  "isSuccess": true,
  "code": "2000",
  "message": "OK"
}
```

---

## 게임 설정 관리 API

### 1. 게임 설정 생성
**POST** `/admin/game-configs`

관리자가 새로운 게임 모드 설정을 생성합니다.

**Headers**
```
Authorization: Bearer {access_token}
Content-Type: application/json
```

**Request Body**
```json
{
  "gameModeKey": "ROADVIEW",
  "playerMatchTypeKey": "SOLO",
  "isSingleMode": false
}
```

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| gameModeKey | String | O | 게임 모드 (ROADVIEW, PHOTO) |
| playerMatchTypeKey | String | X | 매치 타입 (SOLO, TEAM) - 멀티플레이 전용 |
| isSingleMode | Boolean | O | 싱글/멀티 모드 구분 (true: 싱글, false: 멀티) |

**Response**
```json
{
  "isSuccess": true,
  "code": "2000",
  "message": "OK",
  "data": 1
}
```

---

### 2. 모든 기본 게임 설정 초기화
**POST** `/admin/game-configs/initialize`

관리자가 모든 기본 게임 모드 설정을 한 번에 생성합니다.
- 이미 존재하는 설정은 건너뛰고 없는 설정만 생성
- 총 6개 설정: 싱글 로드뷰/포토, 멀티 로드뷰/포토 개인전/팀전

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
      "configId": 1,
      "gameMode": "ROADVIEW",
      "playerMatchType": null,
      "isSingleMode": true,
      "isActive": true,
      "description": "싱글 로드뷰",
      "createdAt": "2025-01-01T10:00:00",
      "updatedAt": "2025-01-01T10:00:00"
    }
  ]
}
```

---

### 3. 게임 설정 활성화
**PUT** `/admin/game-configs/{configId}/activate`

관리자가 특정 게임 모드를 활성화합니다.

**Headers**
```
Authorization: Bearer {access_token}
```

**Path Parameters**
| 필드 | 타입 | 설명 |
|------|------|------|
| configId | Long | 게임 설정 ID |

**Response**
```json
{
  "isSuccess": true,
  "code": "2000",
  "message": "OK"
}
```

---

### 4. 게임 설정 비활성화
**PUT** `/admin/game-configs/{configId}/deactivate`

관리자가 특정 게임 모드를 비활성화합니다.

**Headers**
```
Authorization: Bearer {access_token}
```

**Path Parameters**
| 필드 | 타입 | 설명 |
|------|------|------|
| configId | Long | 게임 설정 ID |

**Response**
```json
{
  "isSuccess": true,
  "code": "2000",
  "message": "OK"
}
```

---

### 5. 게임 설정 목록 조회
**GET** `/admin/game-configs`

관리자가 전체 게임 모드 설정 목록을 조회합니다.

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
      "configId": 1,
      "gameMode": "ROADVIEW",
      "playerMatchType": "SOLO",
      "isSingleMode": false,
      "isActive": true,
      "description": "멀티 로드뷰 - 개인전",
      "createdAt": "2025-01-01T10:00:00",
      "updatedAt": "2025-01-01T10:00:00"
    }
  ]
}
```

---

### 6. 게임 설정 삭제
**DELETE** `/admin/game-configs/{configId}`

관리자가 게임 모드 설정을 삭제합니다.

**Headers**
```
Authorization: Bearer {access_token}
```

**Path Parameters**
| 필드 | 타입 | 설명 |
|------|------|------|
| configId | Long | 게임 설정 ID |

**Response**
```json
{
  "isSuccess": true,
  "code": "2000",
  "message": "OK"
}
```

---

## 회원 관리 API

### 1. 회원 목록 조회
**GET** `/admin/members`

관리자가 회원 목록을 페이징 조회합니다.

**Headers**
```
Authorization: Bearer {access_token}
```

**Query Parameters**
| 필드 | 타입 | 필수 | 기본값 | 설명 |
|------|------|------|--------|------|
| page | Integer | X | 0 | 페이지 번호 (0부터 시작) |
| size | Integer | X | 20 | 페이지 크기 |
| sort | String | X | createdAt,DESC | 정렬 기준 |
| role | String | X | - | 역할 필터 (USER, ADMIN) |

**Response**
```json
{
  "isSuccess": true,
  "code": "2000",
  "message": "OK",
  "data": {
    "content": [
      {
        "memberId": 1,
        "username": "user123",
        "nickname": "홍길동",
        "email": "user@example.com",
        "role": "USER",
        "point": 1000,
        "createdAt": "2025-01-01T10:00:00",
        "updatedAt": "2025-01-01T10:00:00"
      }
    ],
    "pageable": {
      "pageNumber": 0,
      "pageSize": 20
    },
    "totalElements": 1,
    "totalPages": 1,
    "last": true,
    "first": true
  }
}
```

---

### 2. 회원 상세 조회
**GET** `/admin/members/{memberId}`

관리자가 특정 회원의 상세 정보를 조회합니다.

**Headers**
```
Authorization: Bearer {access_token}
```

**Path Parameters**
| 필드 | 타입 | 설명 |
|------|------|------|
| memberId | Long | 회원 ID |

**Response**
```json
{
  "isSuccess": true,
  "code": "2000",
  "message": "OK",
  "data": {
    "memberId": 1,
    "username": "user123",
    "nickname": "홍길동",
    "email": "user@example.com",
    "role": "USER",
    "point": 1000,
    "createdAt": "2025-01-01T10:00:00",
    "updatedAt": "2025-01-01T10:00:00",
    "roadviewPracticeGames": 10,
    "roadviewPracticeAvgScore": 85.5,
    "roadviewRankGames": 5,
    "roadviewRankAvgScore": 90.0,
    "roadviewMultiGames": 3,
    "roadviewMultiAvgScore": 88.0,
    "roadviewMultiFirstPlace": 1,
    "roadviewMultiSecondPlace": 1,
    "roadviewMultiThirdPlace": 1,
    "photoPracticeGames": 8,
    "photoPracticeAvgScore": 80.0,
    "photoRankGames": 4,
    "photoRankAvgScore": 85.0,
    "photoMultiGames": 2,
    "photoMultiAvgScore": 83.0,
    "photoMultiFirstPlace": 0,
    "photoMultiSecondPlace": 1,
    "photoMultiThirdPlace": 1,
    "bestScore": 95.5,
    "currentStreak": 3,
    "longestStreak": 7
  }
}
```

**응답 필드 설명**
- 로드뷰 통계: `roadview*` 접두사
  - `PracticeGames`: 연습 모드 게임 수
  - `RankGames`: 랭킹 모드 게임 수
  - `MultiGames`: 멀티플레이 게임 수
  - `*AvgScore`: 평균 점수
  - `*Place`: 순위별 횟수
- 포토 통계: `photo*` 접두사 (구조 동일)
- 공통 통계:
  - `bestScore`: 최고 점수
  - `currentStreak`: 현재 연속 플레이
  - `longestStreak`: 최장 연속 플레이

