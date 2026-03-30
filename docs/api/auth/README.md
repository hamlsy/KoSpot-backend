# Auth API Documentation

인증 관련 API 문서입니다.

## 📋 API 목록

- [이메일 회원가입](#1-이메일-회원가입) ⭐ NEW
- [이메일 로그인](#2-이메일-로그인) ⭐ NEW
- [토큰 재발급](#3-토큰-재발급)
- [로그아웃](#4-로그아웃)
- [소셜 로그인 (OAuth2)](#5-소셜-로그인-oauth2)
- [테스트용 임시 로그인](#6-테스트용-임시-로그인)

---

## 1. 이메일 회원가입
**POST** `/auth/signup`

이메일/비밀번호로 회원가입합니다. 가입 성공 시 JWT를 즉시 발급하여 별도 로그인 없이 바로 사용할 수 있습니다.

> 인증 불필요 (public 엔드포인트)

**Headers**
```
Content-Type: application/json
```

**Request Body**
```json
{
  "email": "user@example.com",
  "password": "mypassword123",
  "nickname": "코스팟유저"
}
```

| 필드 | 타입 | 필수 | 제약 | 설명 |
|------|------|------|------|------|
| email | String | O | 이메일 형식 | 로그인에 사용할 이메일 |
| password | String | O | 최소 8자 | 비밀번호 |
| nickname | String | O | 2~12자 | 닉네임 (중복 불가) |

**Response (성공)**
```json
{
  "isSuccess": true,
  "code": "2000",
  "message": "OK",
  "data": {
    "memberId": 42,
    "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
  }
}
```

| 필드 | 타입 | 설명 |
|------|------|------|
| memberId | Long | 신규 회원 ID |
| accessToken | String | 액세스 토큰 (유효기간 30분) |
| refreshToken | String | 리프레시 토큰 (유효기간 7일) |

**에러 응답**

| HTTP | 코드 | 메시지 | 원인 |
|------|------|--------|------|
| 409 | 4102 | 이미 사용 중인 이메일입니다. | 동일 이메일로 이미 가입된 계정 존재 |
| 404 | 4101 | 이미 존재하는 닉네임입니다. | 동일 닉네임 사용 중 |
| 400 | 4000 | 잘못된 요청입니다. | 유효성 검사 실패 (이메일 형식 오류, 비밀번호 8자 미만, 닉네임 범위 초과 등) |

---

## 2. 이메일 로그인
**POST** `/auth/login`

이메일/비밀번호로 로그인하여 JWT를 발급받습니다.

> 인증 불필요 (public 엔드포인트)

**Headers**
```
Content-Type: application/json
```

**Request Body**
```json
{
  "email": "user@example.com",
  "password": "mypassword123"
}
```

| 필드 | 타입 | 필수 | 제약 | 설명 |
|------|------|------|------|------|
| email | String | O | 이메일 형식 | 가입 시 사용한 이메일 |
| password | String | O | - | 비밀번호 |

**Response (성공)**
```json
{
  "isSuccess": true,
  "code": "2000",
  "message": "OK",
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
  }
}
```

| 필드 | 타입 | 설명 |
|------|------|------|
| accessToken | String | 액세스 토큰 (유효기간 30분) |
| refreshToken | String | 리프레시 토큰 (유효기간 7일) |

**에러 응답**

| HTTP | 코드 | 메시지 | 원인 |
|------|------|--------|------|
| 401 | 4103 | 존재하지 않는 이메일입니다. | 가입되지 않은 이메일 |
| 400 | 4105 | 소셜 계정 전용입니다. 소셜 로그인을 이용해주세요. | 소셜(구글/네이버/카카오)로 가입한 계정으로 이메일 로그인 시도 |
| 401 | 4104 | 비밀번호가 일치하지 않습니다. | 잘못된 비밀번호 |
| 400 | 4000 | 잘못된 요청입니다. | 유효성 검사 실패 |

> **보안 참고**: 이메일 존재 여부 노출을 방지하기 위해 `EMAIL_NOT_FOUND`는 404가 아닌 **401**을 반환합니다.

---

## 3. 토큰 재발급
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
    "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
  }
}
```

---

## 4. 로그아웃
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

## 5. 소셜 로그인 (OAuth2)

소셜 로그인은 서버가 OAuth2 리다이렉트를 처리하며, 완료 후 프론트 URL로 토큰을 쿼리파라미터로 전달합니다.

**로그인 시작 URL**

| 제공자 | URL |
|--------|-----|
| Google | `GET /oauth2/authorization/google` |
| Naver | `GET /oauth2/authorization/naver` |
| Kakao | `GET /oauth2/authorization/kakao` |

**로그인 성공 시 리다이렉트**
```
{FRONTEND_URL}?accessToken={access_token}&refreshToken={refresh_token}
```

**계정 연동 동작**

동일한 이메일로 이미 가입된 계정(이메일 가입 또는 다른 소셜)이 있을 경우, 새 계정을 생성하지 않고 기존 계정으로 로그인 처리됩니다.

| 상황 | 동작 |
|------|------|
| 최초 소셜 로그인 | 신규 회원 자동 가입 후 토큰 발급 |
| 동일 소셜로 재로그인 | 기존 계정으로 로그인 |
| 동일 이메일 계정 존재 | 기존 계정으로 로그인 (계정 연동) |

---

## 6. 테스트용 임시 로그인
**GET** `/auth/tempLogin/{username}`

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

## JWT 토큰 사용 방법

회원가입/로그인 후 발급받은 토큰은 인증이 필요한 모든 API 요청 헤더에 포함해야 합니다.

```
Authorization: Bearer {accessToken}
```

- **accessToken 만료 (30분)**: `/auth/reIssue`로 재발급
- **refreshToken 만료 (7일)**: 재로그인 필요

---

## 전체 에러 코드

| 코드 | HTTP | 메시지 | 발생 API |
|------|------|--------|----------|
| 4101 | 404 | 이미 존재하는 닉네임입니다. | 회원가입 |
| 4102 | 409 | 이미 사용 중인 이메일입니다. | 회원가입 |
| 4103 | 401 | 존재하지 않는 이메일입니다. | 이메일 로그인 |
| 4104 | 401 | 비밀번호가 일치하지 않습니다. | 이메일 로그인 |
| 4105 | 400 | 소셜 계정 전용입니다. 소셜 로그인을 이용해주세요. | 이메일 로그인 |
| 4050 | 401 | 유효하지 않은 리프레시 토큰입니다. | 토큰 재발급 |
| 4051 | 401 | 유효하지 않은 액세스 토큰입니다. | 인증 필요 API |
| 4052 | 401 | 토큰의 유효기간이 만료되었습니다. | 인증 필요 API |
