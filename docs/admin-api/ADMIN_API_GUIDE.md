# 관리자 API 가이드

## 개요

KoSpot 백엔드의 관리자 기능 API 문서입니다. 관리자는 회원 관리, 좌표 관리, 배너 관리, 게임 설정 관리 기능을 사용할 수 있습니다.

## 목차

1. [회원 관리 API](#1-회원-관리-api)
2. [좌표 관리 API](#2-좌표-관리-api)
3. [배너 관리 API](#3-배너-관리-api)
4. [게임 설정 관리 API](#4-게임-설정-관리-api)
5. [메인 페이지 API](#메인-페이지-api)
6. [도메인 구조](#도메인-구조)
7. [UseCase 패턴](#usecase-패턴)
8. [에러 코드](#에러-코드)
9. [사용 플로우 예시](#사용-플로우-예시)
10. [주의사항](#주의사항)
11. [향후 확장 가능성](#향후-확장-가능성)

## 인증

모든 관리자 API는 **관리자 권한(ROLE_ADMIN)**이 필요합니다. 요청 시 JWT 토큰을 Bearer 방식으로 전달해야 합니다.

```
Authorization: Bearer {access_token}
```

## API 엔드포인트

### 1. 회원 관리 API

#### 1.1 회원 목록 조회 (페이징)

```http
GET /admin/members
```

**Query Parameters:**
- `page` (optional): 페이지 번호 (default: 0)
- `size` (optional): 페이지 크기 (default: 20)
- `role` (optional): 권한 필터 (GUEST, USER, ADMIN)

**Response:**
```json
{
  "code": 2000,
  "isSuccess": true,
  "message": "OK",
  "result": {
    "content": [
      {
        "memberId": 1,
        "username": "user123",
        "nickname": "사용자123",
        "email": "user@example.com",
        "role": "USER",
        "point": 1000,
        "createdAt": "2024-01-01T00:00:00",
        "updatedAt": "2024-01-02T00:00:00"
      }
    ],
    "pageable": {...},
    "totalElements": 100,
    "totalPages": 5
  }
}
```

#### 1.2 회원 상세 조회

```http
GET /admin/members/{memberId}
```

**Response:**
```json
{
  "code": 2000,
  "isSuccess": true,
  "message": "OK",
  "result": {
    "memberId": 1,
    "username": "user123",
    "nickname": "사용자123",
    "email": "user@example.com",
    "role": "USER",
    "point": 1000,
    "createdAt": "2024-01-01T00:00:00",
    "updatedAt": "2024-01-02T00:00:00",
    "singlePracticeGames": 50,
    "singlePracticeAvgScore": 8500.5,
    "singleRankGames": 30,
    "singleRankAvgScore": 9000.0,
    "multiGames": 20,
    "multiAvgScore": 7500.0,
    "multiFirstPlace": 5,
    "multiSecondPlace": 8,
    "multiThirdPlace": 7,
    "bestScore": 9999.0,
    "currentStreak": 5,
    "longestStreak": 15
  }
}
```

---

### 2. 좌표 관리 API

#### 2.1 좌표 폼 등록

```http
POST /admin/coordinates
```

**Request Body:**
```json
{
  "lat": 37.5665,
  "lng": 126.9780,
  "poiName": "서울시청",
  "sidoKey": "SEOUL",
  "sigungu": "중구",
  "detailAddress": "태평로1가 31",
  "locationType": "LANDMARK"
}
```

**Response:**
```json
{
  "code": 2000,
  "isSuccess": true,
  "message": "OK",
  "result": 1
}
```

#### 2.2 좌표 엑셀 업로드

```http
POST /admin/coordinates/import-excel
Content-Type: multipart/form-data
```

**Request Body (Form Data):**
- `file` (required, file): 엑셀 파일 (.xlsx, .xls)

**예시 (curl):**
```bash
curl -X POST "http://localhost:8080/admin/coordinates/import-excel" \
  -H "Authorization: Bearer {access_token}" \
  -F "file=@coordinates.xlsx"
```

**엑셀 파일 형식:**

| A (시도) | B (시군구) | C (상세주소1) | D (상세주소2) | E (경도) | F (위도) | G | H (POI명) | I (위치타입) |
|---------|-----------|-------------|-------------|---------|---------|---|----------|------------|
| 서울특별시 | 중구 | 태평로1가 | 31 | 126.9780 | 37.5665 | | 서울시청 | LANDMARK |

> **Note:** 
> - 첫 번째 행은 헤더로 건너뜁니다.
> - 1000개 단위로 배치 처리됩니다.
> - 지원 형식: xlsx, xls

**Response:**
```json
{
  "code": 2000,
  "isSuccess": true,
  "message": "OK"
}
```

#### 2.3 좌표 목록 조회 (페이징)

```http
GET /admin/coordinates
```

**Query Parameters:**
- `page` (optional): 페이지 번호 (default: 0)
- `size` (optional): 페이지 크기 (default: 20)

**Response:**
```json
{
  "code": 2000,
  "isSuccess": true,
  "message": "OK",
  "result": {
    "content": [
      {
        "coordinateId": 1,
        "lat": 37.5665,
        "lng": 126.9780,
        "poiName": "서울시청",
        "sido": "서울특별시",
        "sigungu": "중구",
        "detailAddress": "태평로1가 31",
        "locationType": "LANDMARK"
      }
    ],
    "pageable": {...},
    "totalElements": 500,
    "totalPages": 25
  }
}
```

#### 2.4 좌표 삭제

```http
DELETE /admin/coordinates/{coordinateId}
```

**Response:**
```json
{
  "code": 2000,
  "isSuccess": true,
  "message": "OK"
}
```

---

### 3. 배너 관리 API

> **중요**: 배너 이미지는 S3에 업로드되며, 배너 삭제 시 S3에서도 자동으로 삭제됩니다.

#### 3.1 배너 생성

```http
POST /admin/banners
Content-Type: multipart/form-data
```

**Request Body (Form Data):**
- `title` (required, string): 배너 제목
- `image` (required, file): 배너 이미지 파일 (jpg, jpeg, png, gif)
- `linkUrl` (optional, string): 클릭 시 이동할 URL
- `description` (optional, string): 배너 설명
- `displayOrder` (required, number): 노출 순서

**예시 (curl):**
```bash
curl -X POST "http://localhost:8080/admin/banners" \
  -H "Authorization: Bearer {access_token}" \
  -F "title=신규 이벤트" \
  -F "image=@banner.jpg" \
  -F "linkUrl=https://example.com/event" \
  -F "description=새로운 이벤트가 시작되었습니다!" \
  -F "displayOrder=1"
```

**Response:**
```json
{
  "code": 2000,
  "isSuccess": true,
  "message": "OK",
  "result": 1
}
```

#### 3.2 배너 수정

```http
PUT /admin/banners/{bannerId}
Content-Type: multipart/form-data
```

**Request Body (Form Data):**
- `title` (required, string): 배너 제목
- `image` (optional, file): 배너 이미지 파일 (변경 시에만 전송)
- `linkUrl` (optional, string): 클릭 시 이동할 URL
- `description` (optional, string): 배너 설명
- `displayOrder` (required, number): 노출 순서

> **Note**: `image` 필드를 전송하지 않으면 기존 이미지가 유지됩니다. 전송하면 기존 이미지는 S3에서 삭제되고 새 이미지로 교체됩니다.

**예시 (curl - 이미지 변경 없이 정보만 수정):**
```bash
curl -X PUT "http://localhost:8080/admin/banners/1" \
  -H "Authorization: Bearer {access_token}" \
  -F "title=수정된 이벤트" \
  -F "linkUrl=https://example.com/event-updated" \
  -F "description=이벤트 내용이 변경되었습니다!" \
  -F "displayOrder=2"
```

**예시 (curl - 이미지도 함께 변경):**
```bash
curl -X PUT "http://localhost:8080/admin/banners/1" \
  -H "Authorization: Bearer {access_token}" \
  -F "title=수정된 이벤트" \
  -F "image=@new-banner.jpg" \
  -F "linkUrl=https://example.com/event-updated" \
  -F "description=이벤트 내용이 변경되었습니다!" \
  -F "displayOrder=2"
```

**Response:**
```json
{
  "code": 2000,
  "isSuccess": true,
  "message": "OK"
}
```

#### 3.3 배너 목록 조회

```http
GET /admin/banners
```

**Response:**
```json
{
  "code": 2000,
  "isSuccess": true,
  "message": "OK",
  "result": [
    {
      "bannerId": 1,
      "title": "신규 이벤트",
      "imageUrl": "https://example.com/banner.jpg",
      "linkUrl": "https://example.com/event",
      "description": "새로운 이벤트가 시작되었습니다!",
      "displayOrder": 1,
      "isActive": true,
      "createdAt": "2024-01-01T00:00:00",
      "updatedAt": "2024-01-02T00:00:00"
    }
  ]
}
```

#### 3.4 배너 활성화

```http
PUT /admin/banners/{bannerId}/activate
```

**Response:**
```json
{
  "code": 2000,
  "isSuccess": true,
  "message": "OK"
}
```

#### 3.5 배너 비활성화

```http
PUT /admin/banners/{bannerId}/deactivate
```

**Response:**
```json
{
  "code": 2000,
  "isSuccess": true,
  "message": "OK"
}
```

#### 3.6 배너 삭제

```http
DELETE /admin/banners/{bannerId}
```

> **중요**: 배너 삭제 시 S3에 업로드된 이미지 파일도 자동으로 삭제됩니다.

**Response:**
```json
{
  "code": 2000,
  "isSuccess": true,
  "message": "OK"
}
```

---

### 4. 게임 설정 관리 API

게임 모드를 활성화하거나 비활성화할 수 있습니다.

#### 게임 모드 구성

**싱글 게임:**
- `ROADVIEW` (로드뷰 모드)
- `PHOTO` (사진 모드)

**멀티 게임:**
- `ROADVIEW` + `SOLO` (로드뷰 개인전)
- `ROADVIEW` + `TEAM` (로드뷰 팀전)
- `PHOTO` + `SOLO` (사진 개인전)
- `PHOTO` + `TEAM` (사진 팀전)

#### 4.1 게임 설정 생성

```http
POST /admin/game-configs
```

**Request Body (싱글 모드):**
```json
{
  "gameModeKey": "ROADVIEW",
  "isSingleMode": true
}
```

**Request Body (멀티 모드):**
```json
{
  "gameModeKey": "ROADVIEW",
  "playerMatchTypeKey": "SOLO",
  "isSingleMode": false
}
```

**Response:**
```json
{
  "code": 2000,
  "isSuccess": true,
  "message": "OK",
  "result": 1
}
```

#### 4.2 게임 설정 목록 조회

```http
GET /admin/game-configs
```

**Response:**
```json
{
  "code": 2000,
  "isSuccess": true,
  "message": "OK",
  "result": [
    {
      "configId": 1,
      "gameMode": "ROADVIEW",
      "playerMatchType": null,
      "isSingleMode": true,
      "isActive": true,
      "description": "싱글 로드뷰 모드",
      "createdAt": "2024-01-01T00:00:00",
      "updatedAt": "2024-01-02T00:00:00"
    },
    {
      "configId": 2,
      "gameMode": "ROADVIEW",
      "playerMatchType": "SOLO",
      "isSingleMode": false,
      "isActive": true,
      "description": "멀티 로드뷰 모드 - 개인전",
      "createdAt": "2024-01-01T00:00:00",
      "updatedAt": "2024-01-02T00:00:00"
    }
  ]
}
```

#### 4.3 게임 설정 활성화

```http
PUT /admin/game-configs/{configId}/activate
```

**Response:**
```json
{
  "code": 2000,
  "isSuccess": true,
  "message": "OK"
}
```

#### 4.4 게임 설정 비활성화

```http
PUT /admin/game-configs/{configId}/deactivate
```

**Response:**
```json
{
  "code": 2000,
  "isSuccess": true,
  "message": "OK"
}
```

#### 4.5 게임 설정 삭제

```http
DELETE /admin/game-configs/{configId}
```

**Response:**
```json
{
  "code": 2000,
  "isSuccess": true,
  "message": "OK"
}
```

---

## 에러 코드

| 코드 | HTTP 상태 | 설명 |
|------|-----------|------|
| 4003 | 403 | 관리자 권한이 필요합니다 |
| 4100 | 404 | 회원을 찾을 수 없습니다 |
| 4150 | 404 | 좌표를 찾을 수 없습니다 |
| 4361 | 404 | 배너를 찾을 수 없습니다 |
| 4371 | 404 | 게임 설정을 찾을 수 없습니다 |

---

## 일반 사용자용 배너 API

### 활성화된 배너 목록 조회

```http
GET /banners
```

**인증 불필요** - 누구나 접근 가능합니다.

**Response:**
```json
{
  "code": 2000,
  "isSuccess": true,
  "message": "OK",
  "result": [
    {
      "bannerId": 1,
      "title": "신규 이벤트",
      "imageUrl": "https://example.com/banner.jpg",
      "linkUrl": "https://example.com/event",
      "description": "새로운 이벤트가 시작되었습니다!",
      "displayOrder": 1
    }
  ]
}
```

---

## 도메인 구조

### 배너 (Banner)
- **위치**: `com.kospot.domain.banner`
- **엔티티**: `Banner` (Image와 One-to-One 관계)
- **리포지토리**: `BannerRepository`
- **서비스**: `BannerService`
- **어댑터**: `BannerAdaptor`
- **이미지 처리**: `ImageService` (S3 업로드/삭제)

### 게임 설정 (GameConfig)
- **위치**: `com.kospot.domain.gameconfig`
- **엔티티**: `GameConfig`
- **리포지토리**: `GameConfigRepository`
- **서비스**: `GameConfigService`
- **어댑터**: `GameConfigAdaptor`

---

## UseCase 패턴

모든 관리자 기능은 UseCase 패턴을 따릅니다:

```java
@UseCase
@RequiredArgsConstructor
public class CreateBannerUseCase {

    private final BannerService bannerService;

    @Transactional
    public Long execute(Member admin, AdminBannerRequest.Create request) {
        admin.validateAdmin();  // 관리자 권한 검증
        
        // BannerService에서 이미지를 S3에 업로드하고 Banner 생성
        Banner banner = bannerService.createBanner(
            request.getTitle(),
            request.getImage(),  // MultipartFile
            request.getLinkUrl(),
            request.getDescription(),
            request.getDisplayOrder()
        );
        
        return banner.getId();
    }
}
```

---

## 테스트 시나리오

### 1. 배너 관리 플로우

1. 배너 생성 (이미지 파일 업로드) → POST `/admin/banners` (multipart/form-data)
2. 배너 목록 조회 → GET `/admin/banners`
3. 배너 수정 (이미지 변경 선택적) → PUT `/admin/banners/{bannerId}` (multipart/form-data)
4. 배너 비활성화 → PUT `/admin/banners/{bannerId}/deactivate`
5. 일반 사용자가 활성 배너 조회 → GET `/banners` (비활성화된 배너는 보이지 않음)
6. 배너 활성화 → PUT `/admin/banners/{bannerId}/activate`
7. 배너 삭제 (S3 이미지도 자동 삭제) → DELETE `/admin/banners/{bannerId}`

### 2. 게임 모드 설정 플로우

1. 싱글 로드뷰 모드 생성 → POST `/admin/game-configs` (gameModeKey: ROADVIEW, isSingleMode: true)
2. 멀티 로드뷰 개인전 생성 → POST `/admin/game-configs` (gameModeKey: ROADVIEW, playerMatchTypeKey: SOLO, isSingleMode: false)
3. 설정 목록 조회 → GET `/admin/game-configs`
4. 특정 모드 비활성화 → PUT `/admin/game-configs/{configId}/deactivate`
5. 특정 모드 활성화 → PUT `/admin/game-configs/{configId}/activate`

### 3. 좌표 관리 플로우

1. 폼으로 단일 좌표 등록 → POST `/admin/coordinates` (JSON)
2. 엑셀로 대량 좌표 등록 → POST `/admin/coordinates/import-excel` (multipart/form-data, 파일 직접 업로드)
3. 좌표 목록 조회 → GET `/admin/coordinates`
4. 좌표 삭제 → DELETE `/admin/coordinates/{coordinateId}`

---

## 주의사항

1. **관리자 권한**: 모든 관리자 API는 `ROLE_ADMIN` 권한이 필요합니다.
2. **트랜잭션**: 생성, 수정, 삭제 작업은 모두 트랜잭션으로 처리됩니다.
3. **배너 이미지**:
   - 배너 생성 시 이미지 파일은 S3에 업로드됩니다.
   - 배너 수정 시 이미지 파일을 전송하면 기존 이미지는 S3에서 삭제되고 새 이미지로 교체됩니다.
   - 배너 삭제 시 S3에 업로드된 이미지도 자동으로 삭제됩니다 (cascade delete).
   - 지원 형식: jpg, jpeg, png, gif
   - S3 경로: `file/image/banner/`
4. **배너 순서**: `displayOrder` 값이 작을수록 먼저 노출됩니다.
5. **좌표 관리**:
   - 엑셀 업로드: 클라이언트에서 파일을 직접 multipart/form-data로 전송합니다.
   - 배치 처리: 1000개 단위로 자동 배치 처리됩니다.
   - 엑셀 형식: 첫 번째 행은 헤더로 간주하고 건너뜁니다.
6. **좌표 타입**: `LocationType`은 `LANDMARK`, `TOURIST_SPOT`, `STREET`, `BUILDING` 등이 있습니다.
7. **게임 설정**: 멀티 모드는 반드시 `playerMatchTypeKey`를 지정해야 합니다.

---

## 메인 페이지 API

사용자가 메인 페이지에 접속할 때 필요한 모든 정보를 한 번에 제공하는 API입니다.

### 메인 페이지 정보 조회

```http
GET /main
```

**인증:** 선택 사항 (로그인하지 않은 사용자도 접근 가능)

**Response:**
```json
{
  "code": 2000,
  "isSuccess": true,
  "message": "OK",
  "result": {
    "isAdmin": true,
    "gameModeStatus": {
      "roadviewEnabled": true,
      "photoEnabled": true,
      "multiplayEnabled": false
    },
    "recentNotices": [
      {
        "noticeId": 3,
        "title": "신규 게임 모드 오픈",
        "createdDate": "2025-10-28T10:00:00"
      },
      {
        "noticeId": 2,
        "title": "서버 정기 점검 안내",
        "createdDate": "2025-10-27T15:30:00"
      },
      {
        "noticeId": 1,
        "title": "KoSpot 서비스 오픈",
        "createdDate": "2025-10-26T09:00:00"
      }
    ],
    "banners": [
      {
        "bannerId": 1,
        "title": "신규 이벤트",
        "imageUrl": "https://s3.amazonaws.com/bucket/banner/image1.jpg",
        "linkUrl": "https://kospot.com/events/1",
        "description": "신규 이벤트에 참여하세요!",
        "displayOrder": 1
      }
    ]
  }
}
```

**Response 필드 설명:**

| 필드 | 타입 | 설명 |
|------|------|------|
| `isAdmin` | Boolean | 현재 사용자가 관리자인지 여부 (네비게이션바에 관리자 버튼 표시 결정) |
| `gameModeStatus` | Object | 게임 모드별 활성화 상태 |
| `gameModeStatus.roadviewEnabled` | Boolean | 로드뷰 모드 활성화 여부 (싱글/멀티 포함) |
| `gameModeStatus.photoEnabled` | Boolean | 포토 모드 활성화 여부 (싱글/멀티 포함) |
| `gameModeStatus.multiplayEnabled` | Boolean | 멀티플레이 모드 활성화 여부 (로드뷰/포토 포함) |
| `recentNotices` | Array | 최근 공지사항 3개 (생성일 기준 내림차순) |
| `recentNotices[].noticeId` | Long | 공지사항 ID |
| `recentNotices[].title` | String | 공지사항 제목 |
| `recentNotices[].createdDate` | DateTime | 공지사항 생성일시 |
| `banners` | Array | 활성화된 배너 목록 (displayOrder 오름차순) |
| `banners[].bannerId` | Long | 배너 ID |
| `banners[].title` | String | 배너 제목 |
| `banners[].imageUrl` | String | 배너 이미지 S3 URL |
| `banners[].linkUrl` | String | 배너 클릭 시 이동할 URL |
| `banners[].description` | String | 배너 설명 |
| `banners[].displayOrder` | Integer | 배너 노출 순서 |

**특징:**
- 인증이 필요하지 않은 경우 `isAdmin: false`로 응답
- 한 번의 API 호출로 메인 페이지에 필요한 모든 데이터를 조회
- 게임 모드는 3가지 카테고리(로드뷰, 포토, 멀티플레이)의 활성화 여부만 제공
  - `roadviewEnabled`: 로드뷰 싱글 또는 로드뷰 멀티가 하나라도 활성화되면 true
  - `photoEnabled`: 포토 싱글 또는 포토 멀티가 하나라도 활성화되면 true
  - `multiplayEnabled`: 모든 멀티플레이 모드 중 하나라도 활성화되면 true
- 최근 공지사항은 항상 최대 3개까지만 조회
- 비활성화된 배너는 자동으로 제외됨

---

## 향후 확장 가능성

### 테마 모드 (예정)
- 테마 생성, 수정, 삭제
- 테마별 좌표 할당
- 테마 활성화/비활성화

요구사항에는 포함되어 있으나, 현재는 구현되지 않았습니다. 필요시 추가 개발 가능합니다.

