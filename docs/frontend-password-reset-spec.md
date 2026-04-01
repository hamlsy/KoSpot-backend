# 비밀번호 재설정 프론트엔드 연동 명세

## 개요

이메일/비밀번호(로컬) 계정 전용 기능입니다.
소셜 로그인(Google, Naver, Kakao) 계정은 비밀번호 재설정을 지원하지 않습니다.

**전체 플로우**
```
[로그인 페이지] → 비밀번호 찾기 클릭
    → [이메일 입력 페이지] → 이메일 입력 후 전송
    → [안내 페이지] → 메일함 확인 안내
    → [링크 클릭] → [비밀번호 재설정 페이지] → 새 비밀번호 입력
    → [완료] → 로그인 페이지로 이동
```

---

## API 명세

### Base URL
```
/api/auth
```
> 로컬 개발 환경: `http://localhost:8080/api/auth`

---

### 1. 비밀번호 재설정 메일 발송

```
POST /auth/password-reset/request
```

**인증 불필요** (Bearer 토큰 없이 호출)

#### Request

```json
{
  "email": "user@example.com"
}
```

| 필드 | 타입 | 필수 | 유효성 |
|------|------|------|--------|
| `email` | string | ✅ | 이메일 형식 |

#### Response (항상 200)

```json
{
  "isSuccess": true,
  "code": 2000,
  "message": "OK",
  "data": null
}
```

> **보안 정책**: 이메일 존재 여부와 무관하게 항상 200을 반환합니다.
> 소셜 계정 이메일이거나 존재하지 않는 이메일이어도 동일하게 200이 반환됩니다.
> 따라서 프론트는 항상 "메일을 발송했습니다" 안내 문구를 표시하면 됩니다.

#### Error Response

| HTTP | code | message | 원인 |
|------|------|---------|------|
| 429 | 4412 | 이메일 요청 횟수를 초과했습니다. 잠시 후 다시 시도해주세요. | 1시간 내 5회 초과 요청 |
| 500 | 4411 | 이메일 발송에 실패했습니다. 잠시 후 다시 시도해주세요. | SMTP 서버 오류 |
| 400 | 4000 | 잘못된 요청입니다. | 이메일 형식 오류 |

---

### 2. 비밀번호 재설정 확인

```
POST /auth/password-reset/confirm
```

**인증 불필요** (Bearer 토큰 없이 호출)

#### Request

```json
{
  "token": "550e8400-e29b-41d4-a716-446655440000",
  "newPassword": "newSecureP@ss1"
}
```

| 필드 | 타입 | 필수 | 유효성 |
|------|------|------|--------|
| `token` | string | ✅ | 공백 불가 |
| `newPassword` | string | ✅ | 최소 8자 이상 |

#### Response (성공)

```json
{
  "isSuccess": true,
  "code": 2000,
  "message": "OK",
  "data": null
}
```

#### Error Response

| HTTP | code | message | 원인 | 프론트 처리 |
|------|------|---------|------|------------|
| 400 | 4106 | 유효하지 않거나 만료된 비밀번호 재설정 토큰입니다. | 토큰 만료(15분) / 이미 사용 / 잘못된 토큰 | "링크가 만료되었습니다" 안내 후 재요청 유도 |
| 400 | 4000 | 잘못된 요청입니다. | 비밀번호 8자 미만 등 validation 실패 | 입력 필드 에러 표시 |

---

## 페이지 구성

### 페이지 1. 이메일 입력 페이지
**경로 예시**: `/forgot-password`

**UI 구성**
```
[제목] 비밀번호를 잊으셨나요?
[설명] 가입하신 이메일 주소를 입력하시면 재설정 링크를 보내드립니다.

[이메일 입력 필드]
  placeholder: example@email.com
  validation: 이메일 형식

[전송 버튼] 재설정 링크 발송

[하단 링크] 로그인으로 돌아가기
```

**동작**
1. 이메일 입력 후 전송 버튼 클릭 → `POST /auth/password-reset/request` 호출
2. 응답 코드와 무관하게(200이면) → 페이지 2로 이동
3. 전송 버튼은 API 호출 중 `disabled` 처리 (중복 클릭 방지)
4. `4412` 에러 수신 시 → "1시간 후 다시 시도해주세요" 메시지 표시 (페이지 이동 없음)
5. `4411` 에러 수신 시 → "잠시 후 다시 시도해주세요" 메시지 표시

---

### 페이지 2. 메일 발송 안내 페이지
**경로 예시**: `/forgot-password/sent`

**UI 구성**
```
[아이콘] 메일 발송 아이콘

[제목] 이메일을 확인해주세요

[설명]
  입력하신 이메일 주소로 비밀번호 재설정 링크를 발송했습니다.
  링크는 15분간 유효합니다.

[재발송 버튼] 이메일을 받지 못하셨나요? 재발송
  → 클릭 시 페이지 1로 이동

[하단 링크] 로그인으로 돌아가기
```

> 보안상 실제로 메일이 발송되었는지 여부를 사용자에게 노출하지 않습니다.

---

### 페이지 3. 비밀번호 재설정 페이지
**경로 예시**: `/reset-password?token={UUID}`

> URL에서 `token` 쿼리 파라미터를 추출하여 API에 전달합니다.

**UI 구성**
```
[제목] 새 비밀번호 설정

[새 비밀번호 입력 필드]
  type: password
  placeholder: 8자 이상 입력
  validation: 최소 8자

[비밀번호 확인 입력 필드]
  type: password
  placeholder: 비밀번호를 다시 입력
  validation: 위 필드와 동일한 값

[변경 버튼] 비밀번호 변경
```

**동작**
1. 페이지 진입 시 URL에서 `token` 파라미터 추출
   - `token` 없으면 → 로그인 페이지로 redirect
2. 비밀번호 변경 버튼 클릭 → `POST /auth/password-reset/confirm` 호출
3. **성공** → 페이지 4로 이동
4. **`4106` 에러** → "링크가 만료되었거나 이미 사용되었습니다." 안내 + 재요청 버튼 표시
5. **Validation 에러** → 입력 필드 하단에 에러 메시지 표시

---

### 페이지 4. 비밀번호 변경 완료 페이지
**경로 예시**: `/reset-password/success`

**UI 구성**
```
[아이콘] 체크 아이콘

[제목] 비밀번호가 변경되었습니다

[설명] 새로운 비밀번호로 로그인해주세요.

[버튼] 로그인하러 가기 → /login 으로 이동
```

---

## API 연동 코드 예시 (TypeScript)

```typescript
const API_BASE = '/api';

// 1. 비밀번호 재설정 요청
async function requestPasswordReset(email: string): Promise<void> {
  const res = await fetch(`${API_BASE}/auth/password-reset/request`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ email }),
  });

  const data = await res.json();

  if (!data.isSuccess) {
    // 4412: rate limit / 4411: SMTP 오류
    throw { code: data.code, message: data.message };
  }
}

// 2. 비밀번호 재설정 확인
async function confirmPasswordReset(token: string, newPassword: string): Promise<void> {
  const res = await fetch(`${API_BASE}/auth/password-reset/confirm`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ token, newPassword }),
  });

  const data = await res.json();

  if (!data.isSuccess) {
    // 4106: 토큰 만료 / 4000: validation 실패
    throw { code: data.code, message: data.message };
  }
}

// 3. URL에서 token 추출
function getTokenFromUrl(): string | null {
  const params = new URLSearchParams(window.location.search);
  return params.get('token');
}
```

---

## 에러 코드 전체 목록

| code | HTTP | 메시지 | 발생 위치 | 프론트 처리 |
|------|------|--------|----------|------------|
| 4106 | 400 | 유효하지 않거나 만료된 비밀번호 재설정 토큰입니다. | confirm API | 만료 안내 + 재요청 버튼 |
| 4411 | 500 | 이메일 발송에 실패했습니다. 잠시 후 다시 시도해주세요. | request API | 에러 토스트 표시 |
| 4412 | 429 | 이메일 요청 횟수를 초과했습니다. 잠시 후 다시 시도해주세요. | request API | 에러 메시지 + 버튼 비활성화 |
| 4000 | 400 | 잘못된 요청입니다. | 모든 API | 입력 필드 에러 표시 |

---

## 주의사항

1. **`/reset-password` URL은 백엔드가 아닌 프론트엔드 라우팅 경로입니다.**
   백엔드 `application.yml`의 `app.password-reset.base-url`에 프론트엔드 도메인을 설정해야 합니다.
   예: `https://kospot.kr/reset-password` → 프론트 라우터에서 `/reset-password` 경로를 처리

2. **토큰 유효 시간은 15분**입니다. 사용자에게 명확히 안내하세요.

3. **동일 토큰은 1회만 사용 가능**합니다. 비밀번호 변경 성공 후 같은 링크를 다시 클릭하면 `4106` 에러가 반환됩니다.

4. **소셜 계정은 이 기능을 사용할 수 없습니다.** 소셜 계정 사용자가 이메일을 입력해도 서버는 200을 반환하지만 메일은 발송되지 않습니다. 로그인 페이지에서 소셜/로컬 계정 여부에 따라 분기 처리를 권장합니다.
