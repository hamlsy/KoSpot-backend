# Banner API Documentation

배너 관련 API 문서입니다. 일반 사용자용 API입니다.

> 💡 **참고**: 관리자용 배너 관리 API는 [Admin API 문서](../admin/README.md#배너-관리-api)를 참조하세요.

---

## 활성화된 배너 목록 조회
**GET** `/banners`

메인 페이지에 노출될 활성화된 배너 목록을 조회합니다.
배너는 `displayOrder` 순서대로 정렬되어 반환됩니다.

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
      "displayOrder": 1
    },
    {
      "bannerId": 2,
      "title": "업데이트 안내",
      "imageUrl": "https://example.com/banner2.jpg",
      "linkUrl": "https://example.com/notice",
      "description": "새로운 기능이 추가되었습니다",
      "displayOrder": 2
    }
  ]
}
```

**응답 필드 설명**
- `bannerId`: 배너 ID
- `title`: 배너 제목
- `imageUrl`: 배너 이미지 URL
- `linkUrl`: 배너 클릭 시 이동할 URL (선택적)
- `description`: 배너 설명
- `displayOrder`: 노출 순서 (작을수록 먼저 노출)

**참고사항**
- 인증이 필요하지 않은 공개 API입니다.
- 활성화된 배너만 반환됩니다 (`isActive = true`).
- 배너는 `displayOrder` 오름차순으로 정렬됩니다.

