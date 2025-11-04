# Auth API Documentation

인증 관련 API 문서입니다.

## 📋 API 목록

- [테스트용 임시 로그인](#1-테스트용-임시-로그인)
- [토큰 재발급](#2-토큰-재발급)
- [로그아웃](#3-로그아웃)

---

## 1. 테스트용 임시 로그인
**GET** `/auth/tempLogin/{username}`

테스트용 임시 로그인 API입니다.

> ⚠️ **주의**: 이 API는 테스트 용도로만 사용해야 합니다.

**Path Parameters**
| 필드 | 타입 | 설명 |
|------|------|------|
| username | String | 사용자명 |

**Response**
```json
{
  "isSuccess": true,
  "code": "2000",
  "message": "OK",
  "data": {
    "memberId": 1,
    "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
  }
}
```

---

## 2. 토큰 재발급
**POST** `/auth/reIssue`

Refresh Token을 사용하여 새로운 Access Token과 Refresh Token을 발급받습니다.

**Headers**
```
Content-Type: application/json
```

**Request Body**
```json
{
  "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
}
```

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| refreshToken | String | O | 리프레시 토큰 |

**Response**
```json
{
  "isSuccess": true,
  "code": "2000",
  "message": "OK",
  "data": {
    "grantType": "Bearer",
    "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "accessTokenExpirationTime": 1800000,
    "refreshTokenExpirationTime": 604800000
  }
}
```

**응답 필드 설명**
- `grantType`: 토큰 타입 (Bearer)
- `accessToken`: 새로 발급된 액세스 토큰
- `refreshToken`: 새로 발급된 리프레시 토큰
- `accessTokenExpirationTime`: 액세스 토큰 만료 시간 (밀리초)
- `refreshTokenExpirationTime`: 리프레시 토큰 만료 시간 (밀리초)

---

## 3. 로그아웃
**POST** `/auth/logout`

사용자 로그아웃을 처리하고 Refresh Token을 무효화합니다.

**Headers**
```
Content-Type: application/json
Authorization: Bearer {access_token}
```

**Request Body**
```json
{
  "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
}
```

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| refreshToken | String | O | 리프레시 토큰 |

**Response**
```json
{
  "isSuccess": true,
  "code": "2000",
  "message": "OK"
}
```

---

## 에러 코드

| 코드 | 설명 |
|------|------|
| 4001 | 유효하지 않은 토큰 |
| 4002 | 만료된 토큰 |
| 4003 | 지원하지 않는 토큰 |
| 4004 | 토큰 형식 오류 |

