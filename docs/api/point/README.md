# Point History API Documentation

포인트 기록 관련 API 문서입니다.

---

## 포인트 기록 조회
**GET** `/pointHistory/`

사용자의 포인트 변동 기록을 페이징 조회합니다.

**Headers**
```
Authorization: Bearer {access_token}
```

**Query Parameters**
| 필드 | 타입 | 필수 | 기본값 | 설명 |
|------|------|------|--------|------|
| page | Integer | X | 0 | 페이지 번호 (0부터 시작) |

**Response**
```json
{
  "isSuccess": true,
  "code": "2000",
  "message": "OK",
  "data": [
    {
      "changeAmount": 100,
      "description": "게임 완료 보상",
      "createdDate": "2025-01-01T10:00:00"
    },
    {
      "changeAmount": -50,
      "description": "아이템 구매",
      "createdDate": "2025-01-01T11:00:00"
    },
    {
      "changeAmount": 200,
      "description": "퀘스트 보상",
      "createdDate": "2025-01-01T12:00:00"
    }
  ]
}
```

**응답 필드 설명**
- `changeAmount`: 포인트 변동량 (양수: 획득, 음수: 사용)
- `description`: 변동 사유 설명
- `createdDate`: 변동 일시

**포인트 변동 사유 예시**
- 게임 완료 보상
- 아이템 구매
- 퀘스트 보상
- 출석 체크 보상
- 랭크 달성 보상
- 이벤트 보상
- 환불

**참고사항**
- 최신 기록부터 시간 역순으로 정렬됩니다.
- 페이지당 기본 20개 항목이 조회됩니다.
- 모든 포인트 변동 내역이 투명하게 기록됩니다.

