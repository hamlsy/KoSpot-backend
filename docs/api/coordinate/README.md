# Coordinate API Documentation

좌표 관련 API 문서입니다.

> 💡 **참고**: 관리자용 좌표 관리 API는 [Admin API 문서](../admin/README.md#좌표-관리-api)를 참조하세요.

---

## 랜덤 좌표 조회 (테스트용)
**GET** `/randomCoord/{sido}`

특정 시도의 랜덤 좌표를 조회합니다.

> ⚠️ **주의**: 이 API는 테스트 용도로 제공됩니다.

**Path Parameters**
| 필드 | 타입 | 설명 |
|------|------|------|
| sido | String | 시도 코드 (예: SEOUL, BUSAN, INCHEON) |

**시도 코드 목록**
| 코드 | 지역명 |
|------|--------|
| SEOUL | 서울특별시 |
| BUSAN | 부산광역시 |
| DAEGU | 대구광역시 |
| INCHEON | 인천광역시 |
| GWANGJU | 광주광역시 |
| DAEJEON | 대전광역시 |
| ULSAN | 울산광역시 |
| SEJONG | 세종특별자치시 |
| GYEONGGI | 경기도 |
| GANGWON | 강원도 |
| CHUNGBUK | 충청북도 |
| CHUNGNAM | 충청남도 |
| JEONBUK | 전라북도 |
| JEONNAM | 전라남도 |
| GYEONGBUK | 경상북도 |
| GYEONGNAM | 경상남도 |
| JEJU | 제주특별자치도 |

**Response**
```json
{
  "isSuccess": true,
  "code": "2000",
  "message": "OK",
  "data": {
    "address": {
      "sido": {
        "key": "SEOUL",
        "name": "서울특별시"
      },
      "sigungu": "중구",
      "detailAddress": "서울역 광장"
    },
    "lat": 37.5665,
    "lng": 126.9780,
    "locationType": "LANDMARK",
    "createdDate": "2025-01-01T10:00:00",
    "poiName": "서울역"
  }
}
```

**응답 필드 설명**
- `address`: 주소 정보
  - `sido`: 시도 정보
    - `key`: 시도 코드
    - `name`: 시도명
  - `sigungu`: 시군구
  - `detailAddress`: 상세 주소
- `lat`: 위도
- `lng`: 경도
- `locationType`: 위치 타입
  - `LANDMARK`: 랜드마크
  - `STREET`: 도로/거리
  - `NATURE`: 자연/관광지
  - `CULTURE`: 문화시설
  - `SHOPPING`: 쇼핑/상업시설
  - `FOOD`: 음식점
  - `ACCOMMODATION`: 숙박시설
  - `TRANSPORTATION`: 교통시설
  - `ETC`: 기타
- `createdDate`: 생성일시
- `poiName`: POI(관심지점) 이름

**참고사항**
- 해당 시도에 등록된 좌표 중 랜덤으로 하나를 반환합니다.
- 게임 테스트 및 개발 용도로 사용할 수 있습니다.

