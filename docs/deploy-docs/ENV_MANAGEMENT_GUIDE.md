# 환경변수 및 배포 설정 관리 가이드

## 📋 목차

1. [환경변수 흐름도](#환경변수-흐름도)
2. [Submodule 기반 환경변수 관리](#submodule-기반-환경변수-관리)
3. [S3 버킷 구성](#s3-버킷-구성)
4. [application.yml 설정 및 주입 방식](#applicationyml-설정-및-주입-방식)
5. [배포 시 환경변수 처리 프로세스](#배포-시-환경변수-처리-프로세스)
6. [환경별 설정 관리](#환경별-설정-관리)

---

## 🔄 환경변수 흐름도

```
┌─────────────────────────────────────────────────────────────────┐
│                    개발 환경 (Local)                             │
│  ┌──────────────────────────────────────────────────────────┐   │
│  │  application.yml (메인 설정)                              │   │
│  │    - 환경변수 플레이스홀더: ${ENV_VAR}                    │   │
│  │    - 기본값 제공: ${ENV_VAR:default}                      │   │
│  └────────────────────┬─────────────────────────────────────┘   │
│                       │                                          │
│  ┌────────────────────▼─────────────────────────────────────┐   │
│  │  application-local.yml (로컬 개발)                        │   │
│  │    - 하드코딩된 개발용 값                                │   │
│  │    - Git에 커밋 (민감정보 없음)                          │   │
│  └──────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────┘
                              │
                              │ Git Push
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                    GitHub Repository                             │
│  ┌──────────────────────────────────────────────────────────┐   │
│  │  KoSpot-backend (Public)                                  │   │
│  │    - src/main/resources/application.yml                   │   │
│  │    - src/main/resources/application-local.yml             │   │
│  └──────────────────────────────────────────────────────────┘   │
│  ┌──────────────────────────────────────────────────────────┐   │
│  │  KoSpot-backend-private (Submodule - Private)            │   │
│  │    - .env.prod (운영 환경변수)                           │   │
│  │    - .env.test (테스트 환경변수)                         │   │
│  │    - README.md (환경변수 템플릿 및 가이드)               │   │
│  └──────────────────────────────────────────────────────────┘   │
└────────────────────────────┬────────────────────────────────────┘
                             │ GitHub Actions
                             ▼
┌─────────────────────────────────────────────────────────────────┐
│                    GitHub Actions Workflow                       │
│  ┌──────────────────────────────────────────────────────────┐   │
│  │  1. Build & Test (application-local.yml 사용)            │   │
│  │  2. Docker 이미지 빌드 (JAR 파일 포함)                  │   │
│  │  3. 배포 패키지 생성:                                    │   │
│  │     - kospot-backend.tar (Docker 이미지)                 │   │
│  │     - docker-compose.yml                                  │   │
│  │     - appspec.yml                                         │   │
│  │     - scripts/                                            │   │
│  └──────────────────────────────────────────────────────────┘   │
└────────────────────────────┬────────────────────────────────────┘
                             │ Upload to S3
                             ▼
┌─────────────────────────────────────────────────────────────────┐
│                    AWS S3 (CodeDeploy 버킷)                     │
│  - kospot-deploy-bucket/deploy-{sha}.zip                        │
└────────────────────────────┬────────────────────────────────────┘
                             │ CodeDeploy
                             ▼
┌─────────────────────────────────────────────────────────────────┐
│                    EC2 Instance                                  │
│  ┌──────────────────────────────────────────────────────────┐   │
│  │  /home/ubuntu/kospot/.env (수동 배치)                    │   │
│  │    - DB_URL, DB_USERNAME, DB_PASSWORD                     │   │
│  │    - REDIS_PASSWORD                                       │   │
│  │    - JWT_SECRET                                           │   │
│  │    - AWS_ACCESS_KEY_ID, AWS_SECRET_ACCESS_KEY            │   │
│  │    - AWS_S3_BUCKET_IMAGES (새로 추가!)                   │   │
│  │    - OAUTH2 설정 등                                      │   │
│  └────────────────────┬─────────────────────────────────────┘   │
│                       │                                          │
│  ┌────────────────────▼─────────────────────────────────────┐   │
│  │  Docker Compose (start.sh에서 실행)                      │   │
│  │    - .env 파일 로드                                      │   │
│  │    - 환경변수를 컨테이너에 전달                          │   │
│  └────────────────────┬─────────────────────────────────────┘   │
│                       │                                          │
│  ┌────────────────────▼─────────────────────────────────────┐   │
│  │  Spring Boot Application Container                        │   │
│  │    - SPRING_PROFILES_ACTIVE=prod                          │   │
│  │    - 환경변수가 application.yml의 ${} 치환               │   │
│  │    - AWS S3 클라이언트 초기화                            │   │
│  └──────────────────────────────────────────────────────────┘   │
└────────────────────────────┬────────────────────────────────────┘
                             │ 외부 리소스 접근
                             ▼
┌─────────────────────────────────────────────────────────────────┐
│  ┌────────────────┐  ┌────────────────┐  ┌─────────────────┐   │
│  │   AWS RDS      │  │  AWS S3 (이미지)│  │   AWS S3 (기타) │   │
│  │   (MySQL)      │  │  kospot-images  │  │   추가 버킷들   │   │
│  └────────────────┘  └────────────────┘  └─────────────────┘   │
└─────────────────────────────────────────────────────────────────┘
```

---

## 🔐 Submodule 기반 환경변수 관리

### 1. Submodule 구조

KoSpot 프로젝트는 민감한 환경변수를 별도의 Private 리포지토리로 관리합니다.

```
KoSpot-backend/                    # 메인 리포지토리 (Public)
├── src/
│   └── main/
│       └── resources/
│           ├── application.yml           # 환경변수 플레이스홀더
│           ├── application-local.yml     # 로컬 개발용 (Git 포함)
│           └── application-test.yml      # 테스트용 (Git 포함)
└── KoSpot-backend-private/        # Submodule (Private)
    ├── .env.prod                  # 운영 환경변수 (Git 포함, Private)
    ├── .env.test                  # 테스트 환경변수 (Git 포함, Private)
    └── README.md                  # 환경변수 템플릿
```

### 2. Submodule에 생성할 파일들

#### `.env.prod` (운영 환경)

```bash
# ============================================
# KoSpot Backend - Production Environment
# ============================================

# -------------------- Database (RDS) --------------------
DB_HOST=your-rds-endpoint.ap-northeast-2.rds.amazonaws.com
DB_PORT=3306
DB_NAME=kospot_prod
DB_USERNAME=admin
DB_PASSWORD=your-strong-db-password-here

# -------------------- Redis --------------------
REDIS_HOST=redis  # Docker Compose 네트워크 내부 호스트명
REDIS_PORT=6379
REDIS_PASSWORD=your-strong-redis-password-here

# -------------------- JWT --------------------
JWT_SECRET=your-base64-encoded-256-bit-secret-key-here
JWT_ACCESS_TOKEN_EXPIRATION=3600000
JWT_REFRESH_TOKEN_EXPIRATION=604800000

# -------------------- OAuth2 --------------------
# Google OAuth2
OAUTH_GOOGLE_CLIENT_ID=your-google-client-id.apps.googleusercontent.com
OAUTH_GOOGLE_CLIENT_SECRET=your-google-client-secret

# Naver OAuth2
OAUTH_NAVER_CLIENT_ID=your-naver-client-id
OAUTH_NAVER_CLIENT_SECRET=your-naver-client-secret

# Kakao OAuth2
OAUTH_KAKAO_CLIENT_ID=your-kakao-client-id
OAUTH_KAKAO_CLIENT_SECRET=your-kakao-client-secret

# -------------------- AWS Configuration --------------------
# IAM 사용자 자격 증명 (EC2 인스턴스 프로파일 사용 시 불필요할 수 있음)
AWS_ACCESS_KEY=your-aws-access-key-id
AWS_SECRET_KEY=your-aws-secret-access-key
AWS_REGION=ap-northeast-2

# -------------------- AWS S3 Buckets --------------------
# CodeDeploy 배포용 버킷 (GitHub Actions에서 사용)
S3_BUCKET_DEPLOY=kospot-deploy-bucket

# 이미지 저장용 버킷 (애플리케이션에서 사용)
S3_BUCKET=kospot-images-prod
# 또는 세분화된 버킷 사용:
# S3_BUCKET_BANNERS=kospot-banners-prod      # 배너 이미지
# S3_BUCKET_PHOTOMODE=kospot-photomode-prod  # 포토모드 이미지
# S3_BUCKET_ITEMS=kospot-items-prod          # 아이템 이미지

# -------------------- Application --------------------
SERVER_PORT=8080

# -------------------- CORS --------------------
CORS_FRONT_URL=https://kospot.example.com
# 또는 여러 도메인:
# CORS_FRONT_URL=https://kospot.com,https://www.kospot.com

# -------------------- WebSocket --------------------
WEBSOCKET_ALLOWED_ORIGINS=https://kospot.example.com

# -------------------- AES Encryption --------------------
AES_SECRET_KEY=your-aes-encryption-key-here

# -------------------- Spring Profile --------------------
SPRING_PROFILES_ACTIVE=prod
```

#### `.env.test` (테스트 환경)

```bash
# ============================================
# KoSpot Backend - Test Environment
# ============================================

# -------------------- Database (테스트 DB) --------------------
DB_HOST=test-rds-endpoint.ap-northeast-2.rds.amazonaws.com
DB_PORT=3306
DB_NAME=kospot_test
DB_USERNAME=test_admin
DB_PASSWORD=test-password

# -------------------- Redis --------------------
REDIS_HOST=redis
REDIS_PORT=6379
REDIS_PASSWORD=test-redis-password

# -------------------- JWT --------------------
JWT_SECRET=test-jwt-secret-key-for-testing-only
JWT_ACCESS_TOKEN_EXPIRATION=3600000
JWT_REFRESH_TOKEN_EXPIRATION=604800000

# -------------------- OAuth2 (테스트용) --------------------
OAUTH_GOOGLE_CLIENT_ID=test-google-client-id
OAUTH_GOOGLE_CLIENT_SECRET=test-google-client-secret
OAUTH_NAVER_CLIENT_ID=test-naver-client-id
OAUTH_NAVER_CLIENT_SECRET=test-naver-client-secret
OAUTH_KAKAO_CLIENT_ID=test-kakao-client-id
OAUTH_KAKAO_CLIENT_SECRET=test-kakao-client-secret

# -------------------- AWS Configuration --------------------
AWS_ACCESS_KEY=test-aws-access-key
AWS_SECRET_KEY=test-aws-secret-key
AWS_REGION=ap-northeast-2

# -------------------- AWS S3 Buckets --------------------
S3_BUCKET_DEPLOY=kospot-deploy-bucket-test
S3_BUCKET=kospot-images-test

# -------------------- Application --------------------
SERVER_PORT=8080
SPRING_PROFILES_ACTIVE=test

# -------------------- CORS --------------------
CORS_FRONT_URL=http://localhost:3000,https://test.kospot.com

# -------------------- WebSocket --------------------
WEBSOCKET_ALLOWED_ORIGINS=http://localhost:3000,https://test.kospot.com

# -------------------- AES Encryption --------------------
AES_SECRET_KEY=test-aes-key
```

#### `README.md` (Submodule 가이드)

```markdown
# KoSpot Backend Private Configuration

이 리포지토리는 KoSpot Backend의 민감한 환경변수를 관리합니다.

## 파일 구조

- `.env.prod` - 운영 환경 환경변수
- `.env.test` - 테스트 환경 환경변수

## 사용 방법

### 1. EC2 서버에 환경변수 배치

운영 서버에 배포 시, `.env.prod` 파일의 내용을 EC2 인스턴스의 `/home/ubuntu/kospot/.env` 경로에 수동으로 배치해야 합니다.

```bash
# EC2 접속
ssh -i your-key.pem ubuntu@your-ec2-ip

# .env 파일 생성
cd /home/ubuntu/kospot
nano .env

# .env.prod 내용을 붙여넣고 저장
# Ctrl+X → Y → Enter

# 권한 설정 (중요!)
chmod 600 .env
```

### 2. 로컬 개발 환경

로컬 개발 시에는 `application-local.yml`을 사용하므로 이 파일들이 필요하지 않습니다.

### 3. 환경변수 업데이트

환경변수를 변경해야 하는 경우:

1. 이 리포지토리에서 `.env.prod` 또는 `.env.test` 수정
2. Git 커밋 및 푸시
3. EC2 서버의 `/home/ubuntu/kospot/.env` 파일을 수동으로 업데이트
4. 애플리케이션 재시작: `docker-compose restart app`

## 보안 주의사항

⚠️ **절대 이 파일들을 Public 리포지토리에 푸시하지 마세요!**

- 이 리포지토리는 Private으로 유지되어야 합니다.
- AWS 자격 증명, DB 비밀번호 등 민감한 정보가 포함되어 있습니다.
- 환경변수 값 변경 시 팀원에게 반드시 공유하세요.

## S3 버킷 구성

### 1. CodeDeploy 배포용 버킷
- **버킷명**: `kospot-deploy-bucket`
- **용도**: GitHub Actions에서 빌드된 배포 패키지 저장
- **접근**: CodeDeploy 서비스 계정만

### 2. 이미지 저장용 버킷
- **버킷명**: `kospot-images-prod`
- **용도**: 배너, 포토모드, 아이템 이미지 저장
- **접근**: 애플리케이션 서버 (IAM 역할)

또는 세분화된 버킷 구성:
- `kospot-banners-prod` - 배너 이미지
- `kospot-photomode-prod` - 포토모드 이미지
- `kospot-items-prod` - 아이템 이미지
```

### 3. Submodule 설정 방법

#### 메인 리포지토리에서 Submodule 추가

```bash
# Private 리포지토리를 Submodule로 추가
git submodule add https://github.com/your-org/KoSpot-backend-private.git KoSpot-backend-private

# 커밋
git add .gitmodules KoSpot-backend-private
git commit -m "Add private configuration submodule"
git push
```

#### 새로운 클론 시 Submodule 포함

```bash
# Submodule을 포함하여 클론
git clone --recurse-submodules https://github.com/your-org/KoSpot-backend.git

# 또는 이미 클론된 경우
git submodule init
git submodule update
```

#### Submodule 업데이트

```bash
# Submodule 최신 버전으로 업데이트
git submodule update --remote KoSpot-backend-private

# 커밋
git add KoSpot-backend-private
git commit -m "Update private configuration"
git push
```

---

## 🪣 S3 버킷 구성

### 버킷 아키텍처

```
┌─────────────────────────────────────────────────────────────────┐
│                        AWS S3 Buckets                            │
├─────────────────────────────────────────────────────────────────┤
│                                                                   │
│  ┌────────────────────────────────────────────────────────────┐ │
│  │  kospot-deploy-bucket (CodeDeploy용)                       │ │
│  ├────────────────────────────────────────────────────────────┤ │
│  │  - 용도: 배포 패키지 저장                                 │ │
│  │  - 접근: GitHub Actions → CodeDeploy → EC2                │ │
│  │  - 내용: deploy-{commit-sha}.zip                          │ │
│  │  - 리전: ap-northeast-2                                    │ │
│  │  - 버저닝: 비활성화 (임시 파일)                           │ │
│  │  - 수명 주기: 30일 후 자동 삭제                           │ │
│  └────────────────────────────────────────────────────────────┘ │
│                                                                   │
│  ┌────────────────────────────────────────────────────────────┐ │
│  │  kospot-images-prod (애플리케이션 이미지)                 │ │
│  ├────────────────────────────────────────────────────────────┤ │
│  │  - 용도: 사용자 업로드 및 정적 이미지 저장                │ │
│  │  - 접근: EC2 (IAM 역할), CloudFront (선택)                │ │
│  │  - 폴더 구조:                                              │ │
│  │    ├── banners/         # 배너 이미지                     │ │
│  │    ├── photomode/       # 포토모드 이미지                 │ │
│  │    ├── items/           # 아이템 이미지                   │ │
│  │    ├── profiles/        # 사용자 프로필 이미지            │ │
│  │    └── thumbnails/      # 썸네일 이미지                   │ │
│  │  - 리전: ap-northeast-2                                    │ │
│  │  - 버저닝: 활성화 (중요 파일 복구)                        │ │
│  │  - 퍼블릭 액세스: 차단 (Presigned URL 사용)               │ │
│  │  - 암호화: SSE-S3 (서버 측 암호화)                        │ │
│  └────────────────────────────────────────────────────────────┘ │
│                                                                   │
└─────────────────────────────────────────────────────────────────┘
```

### S3 버킷 생성 가이드

#### 1. CodeDeploy 배포용 버킷 (이미 존재)

```bash
# AWS CLI로 버킷 생성
aws s3 mb s3://kospot-deploy-bucket --region ap-northeast-2

# 수명 주기 정책 적용 (30일 후 자동 삭제)
aws s3api put-bucket-lifecycle-configuration \
  --bucket kospot-deploy-bucket \
  --lifecycle-configuration file://deploy-bucket-lifecycle.json
```

**deploy-bucket-lifecycle.json**:
```json
{
  "Rules": [
    {
      "Id": "DeleteOldDeployments",
      "Status": "Enabled",
      "Prefix": "",
      "Expiration": {
        "Days": 30
      }
    }
  ]
}
```

#### 2. 이미지 저장용 버킷 (새로 생성)

```bash
# 이미지 저장용 버킷 생성
aws s3 mb s3://kospot-images-prod --region ap-northeast-2

# 버저닝 활성화
aws s3api put-bucket-versioning \
  --bucket kospot-images-prod \
  --versioning-configuration Status=Enabled

# 퍼블릭 액세스 차단
aws s3api put-public-access-block \
  --bucket kospot-images-prod \
  --public-access-block-configuration \
    "BlockPublicAcls=true,IgnorePublicAcls=true,BlockPublicPolicy=true,RestrictPublicBuckets=true"

# 서버 측 암호화 활성화
aws s3api put-bucket-encryption \
  --bucket kospot-images-prod \
  --server-side-encryption-configuration \
    '{"Rules": [{"ApplyServerSideEncryptionByDefault": {"SSEAlgorithm": "AES256"}}]}'

# CORS 설정 (프론트엔드에서 직접 업로드하는 경우)
aws s3api put-bucket-cors \
  --bucket kospot-images-prod \
  --cors-configuration file://images-bucket-cors.json
```

**images-bucket-cors.json**:
```json
{
  "CORSRules": [
    {
      "AllowedOrigins": ["https://kospot.example.com"],
      "AllowedMethods": ["GET", "PUT", "POST", "DELETE"],
      "AllowedHeaders": ["*"],
      "MaxAgeSeconds": 3000
    }
  ]
}
```

### IAM 정책 설정

#### EC2 인스턴스 역할에 S3 접근 권한 추가

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Sid": "CodeDeployBucketReadOnly",
      "Effect": "Allow",
      "Action": [
        "s3:GetObject",
        "s3:ListBucket"
      ],
      "Resource": [
        "arn:aws:s3:::kospot-deploy-bucket",
        "arn:aws:s3:::kospot-deploy-bucket/*"
      ]
    },
    {
      "Sid": "ImagesBucketFullAccess",
      "Effect": "Allow",
      "Action": [
        "s3:PutObject",
        "s3:GetObject",
        "s3:DeleteObject",
        "s3:ListBucket",
        "s3:PutObjectAcl"
      ],
      "Resource": [
        "arn:aws:s3:::kospot-images-prod",
        "arn:aws:s3:::kospot-images-prod/*"
      ]
    }
  ]
}
```

---

## ⚙️ application.yml 설정 및 주입 방식

### 1. application.yml 구조 이해

Spring Boot의 `application.yml`은 **플레이스홀더**를 사용하여 환경변수를 주입받습니다.

```yaml
spring:
  datasource:
    url: jdbc:mysql://${DB_HOST}:${DB_PORT}/${DB_NAME}?serverTimezone=UTC
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}
```

**플레이스홀더 문법**:
- `${ENV_VAR}`: 환경변수 `ENV_VAR` 값을 주입
- `${ENV_VAR:default}`: 환경변수가 없으면 `default` 값 사용

### 2. 환경변수 주입 우선순위

Spring Boot는 다음 순서로 환경변수를 찾습니다 (높은 우선순위부터):

```
1. Java 시스템 속성 (-D플래그)
2. OS 환경변수
3. docker-compose의 environment 섹션
4. .env 파일 (docker-compose에서 로드)
5. application-{profile}.yml
6. application.yml
```

### 3. 현재 프로젝트의 주입 방식

```
┌─────────────────────────────────────────────────────────────────┐
│  EC2: /home/ubuntu/kospot/.env                                   │
│    DB_HOST=rds-endpoint.amazonaws.com                            │
│    DB_PORT=3306                                                  │
│    DB_NAME=kospot_prod                                           │
│    ...                                                           │
└────────────────────┬────────────────────────────────────────────┘
                     │
                     │ start.sh에서 로드
                     ▼
┌─────────────────────────────────────────────────────────────────┐
│  docker-compose.yml                                              │
│    environment:                                                  │
│      - SPRING_PROFILES_ACTIVE=prod                               │
│      - SPRING_DATASOURCE_URL=${DB_URL}     ← .env에서 주입      │
│      - SPRING_DATASOURCE_USERNAME=${DB_USERNAME}                 │
│      - ...                                                       │
└────────────────────┬────────────────────────────────────────────┘
                     │
                     │ Docker 컨테이너 시작
                     ▼
┌─────────────────────────────────────────────────────────────────┐
│  Spring Boot Application Container                               │
│    환경변수:                                                     │
│      SPRING_PROFILES_ACTIVE=prod                                 │
│      SPRING_DATASOURCE_URL=jdbc:mysql://rds-endpoint/kospot_prod │
│      ...                                                         │
└────────────────────┬────────────────────────────────────────────┘
                     │
                     │ Spring Boot 시작
                     ▼
┌─────────────────────────────────────────────────────────────────┐
│  application.yml                                                 │
│    spring:                                                       │
│      datasource:                                                 │
│        url: jdbc:mysql://${DB_HOST}:${DB_PORT}/${DB_NAME}        │
│                             ▲         ▲         ▲               │
│                             │         │         │               │
│                             └─────────┴─────────┘               │
│                             환경변수 값으로 치환                 │
└─────────────────────────────────────────────────────────────────┘
```

### 4. 현재 docker-compose.yml 문제점 및 개선

**현재 docker-compose.yml**의 문제:
```yaml
environment:
  - SPRING_DATASOURCE_URL=${DB_URL}  # ❌ DB_URL은 .env에 없음
```

**application.yml**에서는:
```yaml
url: jdbc:mysql://${DB_HOST}:${DB_PORT}/${DB_NAME}  # ✅ DB_HOST, DB_PORT, DB_NAME 사용
```

**불일치 해결 방법**:

#### 방법 1: docker-compose.yml 수정 (권장)

`docker-compose.yml`을 `application.yml`의 플레이스홀더와 일치시킵니다:

```yaml
environment:
  # Database
  - DB_HOST=${DB_HOST}
  - DB_PORT=${DB_PORT}
  - DB_NAME=${DB_NAME}
  - DB_USERNAME=${DB_USERNAME}
  - DB_PASSWORD=${DB_PASSWORD}
  
  # Redis
  - REDIS_HOST=redis
  - REDIS_PORT=6379
  - REDIS_PASSWORD=${REDIS_PASSWORD}
  
  # JWT
  - JWT_SECRET=${JWT_SECRET}
  
  # OAuth2
  - OAUTH_GOOGLE_CLIENT_ID=${OAUTH_GOOGLE_CLIENT_ID}
  - OAUTH_GOOGLE_CLIENT_SECRET=${OAUTH_GOOGLE_CLIENT_SECRET}
  - OAUTH_NAVER_CLIENT_ID=${OAUTH_NAVER_CLIENT_ID}
  - OAUTH_NAVER_CLIENT_SECRET=${OAUTH_NAVER_CLIENT_SECRET}
  - OAUTH_KAKAO_CLIENT_ID=${OAUTH_KAKAO_CLIENT_ID}
  - OAUTH_KAKAO_CLIENT_SECRET=${OAUTH_KAKAO_CLIENT_SECRET}
  
  # AWS
  - AWS_ACCESS_KEY=${AWS_ACCESS_KEY}
  - AWS_SECRET_KEY=${AWS_SECRET_KEY}
  - AWS_REGION=${AWS_REGION}
  - S3_BUCKET=${S3_BUCKET}
  
  # Application
  - SERVER_PORT=${SERVER_PORT:-8080}
  - CORS_FRONT_URL=${CORS_FRONT_URL}
  - WEBSOCKET_ALLOWED_ORIGINS=${WEBSOCKET_ALLOWED_ORIGINS}
  - AES_SECRET_KEY=${AES_SECRET_KEY}
  
  # Spring Profile
  - SPRING_PROFILES_ACTIVE=prod
```

#### 방법 2: application.yml에 prod 프로파일 추가

새 파일 생성: `src/main/resources/application-prod.yml`

```yaml
spring:
  datasource:
    url: ${SPRING_DATASOURCE_URL}
    username: ${SPRING_DATASOURCE_USERNAME}
    password: ${SPRING_DATASOURCE_PASSWORD}
  
  data:
    redis:
      host: ${SPRING_DATA_REDIS_HOST}
      port: ${SPRING_DATA_REDIS_PORT}
      password: ${SPRING_DATA_REDIS_PASSWORD}
```

---

## 🚀 배포 시 환경변수 처리 프로세스

### 전체 배포 플로우

```
1. 개발자 로컬 PC
   └─> git push origin main

2. GitHub Actions
   ├─> Checkout 코드 (submodule 포함하지 않음)
   ├─> 테스트 실행 (application-local.yml 사용)
   ├─> 빌드 (JAR 파일 생성)
   ├─> Docker 이미지 빌드
   │   └─> JAR 파일만 포함, .env는 포함하지 않음
   ├─> 배포 패키지 생성
   │   ├─> kospot-backend.tar
   │   ├─> docker-compose.yml
   │   ├─> appspec.yml
   │   └─> scripts/
   └─> S3 업로드

3. AWS CodeDeploy
   ├─> S3에서 배포 패키지 다운로드
   ├─> EC2로 배포 패키지 복사
   └─> 배포 스크립트 실행

4. EC2 Instance
   ├─> stop.sh: 기존 컨테이너 중지
   ├─> before_install.sh: 디렉토리 준비
   ├─> after_install.sh: Docker 이미지 로드
   │   └─> .env 파일 존재 확인
   ├─> start.sh:
   │   ├─> .env 파일 로드
   │   ├─> 환경변수를 docker-compose에 전달
   │   └─> docker-compose up -d
   └─> validate.sh: 헬스체크

5. Docker Container
   ├─> 환경변수를 Spring Boot에 전달
   └─> Spring Boot 시작
       ├─> SPRING_PROFILES_ACTIVE=prod 읽기
       ├─> application.yml + application-prod.yml 로드
       └─> 환경변수 플레이스홀더 치환
```

### 상세 단계별 분석

#### Step 1: GitHub Actions에서 빌드

```yaml
# .github/workflows/deploy.yml
steps:
  - name: Checkout code
    uses: actions/checkout@v4
    with:
      submodules: true  # ⚠️ Submodule을 체크아웃하지만 이미지에 포함하지 않음
      token: ${{ secrets.SUBMODULE_TOKEN }}
  
  - name: Run tests
    run: ./gradlew test  # application-local.yml 사용
  
  - name: Build
    run: ./gradlew build -x test  # JAR 파일 생성
  
  - name: Build Docker image
    run: docker build -t kospot-backend:latest .
    # Dockerfile에서 JAR만 복사, .env는 포함하지 않음
```

#### Step 2: EC2에서 .env 로드

```bash
# scripts/start.sh
#!/bin/bash

# .env 파일 존재 확인
if [ ! -f .env ]; then
    echo "❌ ERROR: .env file not found!"
    exit 1
fi

# .env 파일 로드 (export로 환경변수 설정)
export $(cat .env | grep -v '^#' | xargs)

# docker-compose 실행
# docker-compose는 현재 셸의 환경변수를 자동으로 사용
docker-compose up -d
```

#### Step 3: docker-compose가 환경변수 전달

```yaml
# docker-compose.yml
services:
  app:
    environment:
      - DB_HOST=${DB_HOST}  # start.sh에서 export된 환경변수
      - DB_PORT=${DB_PORT}
      # ...
```

#### Step 4: Spring Boot가 환경변수 읽기

```yaml
# application.yml
spring:
  datasource:
    url: jdbc:mysql://${DB_HOST}:${DB_PORT}/${DB_NAME}
    # Docker 컨테이너의 환경변수 DB_HOST, DB_PORT, DB_NAME을 읽음
```

---

## 🏗️ 환경별 설정 관리

### 환경 구분

| 환경 | Profile | 설정 파일 | 환경변수 소스 | 용도 |
|------|---------|----------|---------------|------|
| Local | local | application-local.yml | 하드코딩 | 로컬 개발 |
| Test | test | application-test.yml | 하드코딩 | 테스트 실행 |
| Production | prod | application.yml + .env | EC2의 .env | 운영 서버 |

### Local 환경 (개발자 PC)

```yaml
# application-local.yml
spring:
  profiles:
    active: local
  datasource:
    url: jdbc:mysql://localhost:3306/kospot  # 하드코딩
    username: root
    password: 1234
  
cloud:
  aws:
    s3:
      bucket: kospot-bucket  # 로컬 테스트용 버킷
```

**실행**:
```bash
# IntelliJ에서 실행하거나
./gradlew bootRun

# 또는 명시적으로 프로파일 지정
./gradlew bootRun --args='--spring.profiles.active=local'
```

### Production 환경 (EC2)

```yaml
# application.yml
spring:
  profiles:
    active: prod  # ← Docker에서 SPRING_PROFILES_ACTIVE=prod로 오버라이드
  datasource:
    url: jdbc:mysql://${DB_HOST}:${DB_PORT}/${DB_NAME}  # 환경변수
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}
  
cloud:
  aws:
    s3:
      bucket: ${S3_BUCKET}  # 환경변수
```

**실행**:
```bash
# docker-compose가 자동으로 실행
docker-compose up -d

# 환경변수 확인
docker-compose exec app env | grep DB_HOST
```

---

## 📋 체크리스트

### 초기 설정 (최초 1회)

- [ ] Private 리포지토리 `KoSpot-backend-private` 생성
- [ ] Submodule로 메인 리포지토리에 추가
- [ ] `.env.prod` 파일 작성 (Submodule에)
- [ ] `.env.test` 파일 작성 (Submodule에)
- [ ] GitHub Secrets에 `SUBMODULE_TOKEN` 추가
- [ ] AWS S3 버킷 생성:
  - [ ] `kospot-deploy-bucket` (CodeDeploy용)
  - [ ] `kospot-images-prod` (이미지 저장용)
- [ ] IAM 역할 설정 (EC2가 S3에 접근할 수 있도록)
- [ ] EC2 인스턴스에 `.env` 파일 배치
  - [ ] `/home/ubuntu/kospot/.env` 생성
  - [ ] `chmod 600 .env` 권한 설정

### 배포 시 (매번)

- [ ] 코드 변경 후 테스트
- [ ] `main` 브랜치에 푸시
- [ ] GitHub Actions 빌드 확인
- [ ] CodeDeploy 배포 상태 확인
- [ ] EC2에서 헬스체크 확인

### 환경변수 변경 시

- [ ] Submodule의 `.env.prod` 수정
- [ ] Git 커밋 및 푸시
- [ ] EC2의 `/home/ubuntu/kospot/.env` 수동 업데이트
- [ ] 애플리케이션 재시작: `docker-compose restart app`

---

## 🛠️ 트러블슈팅

### 문제 1: 환경변수가 주입되지 않음

**증상**:
```
Caused by: java.lang.IllegalArgumentException: Could not resolve placeholder 'DB_HOST' in value "jdbc:mysql://${DB_HOST}:${DB_PORT}/${DB_NAME}"
```

**원인**:
- EC2에 `.env` 파일이 없음
- docker-compose.yml에서 환경변수를 전달하지 않음

**해결**:
```bash
# EC2에서 확인
cat /home/ubuntu/kospot/.env

# .env 파일이 없다면 생성
nano /home/ubuntu/kospot/.env
# (Submodule의 .env.prod 내용 복사)

# 권한 설정
chmod 600 /home/ubuntu/kospot/.env

# 재배포
docker-compose down
docker-compose up -d
```

### 문제 2: S3 업로드 실패 (403 Forbidden)

**원인**:
- IAM 역할에 S3 접근 권한이 없음

**해결**:
```bash
# EC2 인스턴스 프로파일 확인
aws sts get-caller-identity

# IAM 역할에 S3 접근 정책 추가 (AWS Console)
```

### 문제 3: Docker 이미지에 .env가 포함되어 보안 문제

**원인**:
- Dockerfile에서 .env를 COPY함

**해결**:
- `.dockerignore` 파일에 `.env` 추가
- Dockerfile에서 .env 복사 제거

```dockerfile
# .dockerignore
.env
.env.*
KoSpot-backend-private/
```

---

**작성일:** 2025-10-31  
**버전:** 1.0.0  
**프로젝트:** KoSpot Backend

