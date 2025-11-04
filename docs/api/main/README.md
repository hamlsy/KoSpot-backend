# Main Page API Documentation

메인 페이지 관련 API 문서입니다.

---

## 메인 페이지 정보 조회
**GET** `/main`

메인 페이지에 필요한 모든 정보를 한 번에 조회합니다.
- 활성화된 게임 모드
- 최근 공지사항 3개
- 활성화된 배너
- 관리자 여부

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
    "isAdmin": false,
    "gameModeStatus": {
      "roadviewEnabled": true,
      "photoEnabled": false,
      "multiplayEnabled": true
    },
    "recentNotices": [
      {
        "noticeId": 1,
        "title": "서비스 점검 안내",
        "createdDate": "2025-01-01T10:00:00"
      },
      {
        "noticeId": 2,
        "title": "신규 기능 업데이트",
        "createdDate": "2025-01-02T10:00:00"
      },
      {
        "noticeId": 3,
        "title": "이벤트 안내",
        "createdDate": "2025-01-03T10:00:00"
      }
    ],
    "banners": [
      {
        "bannerId": 1,
        "title": "신규 이벤트",
        "imageUrl": "https://example.com/banner1.jpg",
        "linkUrl": "https://example.com/event",
        "description": "신규 회원 가입 이벤트",
        "displayOrder": 1
      }
    ]
  }
}
```

**응답 필드 설명**
- `isAdmin`: 관리자 여부
- `gameModeStatus`: 게임 모드 활성화 상태
  - `roadviewEnabled`: 로드뷰 모드 활성화 여부
  - `photoEnabled`: 포토 모드 활성화 여부
  - `multiplayEnabled`: 멀티플레이 모드 활성화 여부
- `recentNotices`: 최근 공지사항 3개
  - `noticeId`: 공지사항 ID
  - `title`: 제목
  - `createdDate`: 작성일시
- `banners`: 활성화된 배너 목록
  - `bannerId`: 배너 ID
  - `title`: 배너 제목
  - `imageUrl`: 배너 이미지 URL
  - `linkUrl`: 클릭 시 이동할 URL
  - `description`: 배너 설명
  - `displayOrder`: 노출 순서

**참고사항**
- 이 API는 메인 페이지 로딩 시 한 번만 호출하여 필요한 모든 정보를 받아올 수 있습니다.
- 게임 모드 활성화 상태에 따라 UI를 동적으로 표시할 수 있습니다.
- 관리자인 경우 관리자 메뉴를 표시할 수 있습니다.

