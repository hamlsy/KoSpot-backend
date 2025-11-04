# Image API Documentation

이미지 관련 API 문서입니다.

---

## 이미지 수정
**PUT** `/image/`

이미지를 수정합니다.

**Headers**
```
Authorization: Bearer {access_token}
Content-Type: multipart/form-data
```

**Request Body (Form Data)**
| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| imageId | Long | O | 이미지 ID |
| newImage | File | O | 새로운 이미지 파일 |

**Response**
```json
{
  "isSuccess": true,
  "code": "2000",
  "message": "OK"
}
```

**참고사항**
- 기존 이미지는 삭제되고 새로운 이미지로 교체됩니다.
- 지원 이미지 형식: JPG, JPEG, PNG, GIF
- 최대 파일 크기: 10MB (설정에 따라 다를 수 있음)

**사용 예시**
- 프로필 이미지 변경
- 아이템 이미지 변경
- 배너 이미지 변경

