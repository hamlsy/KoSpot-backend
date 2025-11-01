# S3 버킷 분리 및 역할 명확화 가이드

## 📋 개요

KoSpot 프로젝트는 **2개의 S3 버킷**을 사용하며, 각각 다른 용도와 설정 위치를 가집니다.

---

## 🪣 두 개의 S3 버킷

### 1️⃣ 배포 전용 버킷 (`kospot-bucket-deploy`)

| 항목 | 내용 |
|------|------|
| **버킷명** | `kospot-bucket-deploy` |
| **용도** | CI/CD 배포 패키지 임시 저장 |
| **사용 주체** | GitHub Actions, CodeDeploy |
| **설정 위치** | `.github/workflows/deploy.yml` |
| **.env 필요 여부** | ❌ **불필요** (GitHub Actions에서만 사용) |

#### 사용 흐름

```
GitHub Actions (deploy.yml)
  ↓ aws s3 cp deploy.zip
S3: kospot-bucket-deploy
  ↓ CodeDeploy 다운로드
EC2 배포
```

#### 설정 위치

```yaml
# .github/workflows/deploy.yml
env:
  S3_BUCKET_NAME: kospot-bucket-deploy  # ← 여기만 있으면 됨!
  
steps:
  - name: Upload to S3
    run: |
      aws s3 cp deploy/deploy.zip \
        s3://${{ env.S3_BUCKET_NAME }}/deploy-${{ github.sha }}.zip
```

#### 특징
- ✅ GitHub Actions 전용
- ✅ .env 파일에 **설정 불필요**
- ✅ 애플리케이션 코드에서 사용 안 함
- ✅ 임시 파일 저장 (30일 후 자동 삭제)

---

### 2️⃣ 이미지 저장용 버킷 (`kospot-images-prod`)

| 항목 | 내용 |
|------|------|
| **버킷명** | `kospot-images-prod` |
| **용도** | 배너, 아이템, 포토모드 이미지 저장 |
| **사용 주체** | Spring Boot 애플리케이션 (EC2) |
| **설정 위치** | `.env` → `docker-compose.yml` → `application.yml` |
| **deploy.yml 필요 여부** | ❌ **불필요** (애플리케이션에서만 사용) |

#### 사용 흐름

```
사용자 업로드 요청
  ↓
Spring Boot Application
  ↓ AWS SDK
S3: kospot-images-prod
```

#### 설정 위치

```bash
# 1. .env 파일 (EC2에만 존재)
S3_BUCKET=kospot-images-prod

# 2. docker-compose.yml
environment:
  - S3_BUCKET=${S3_BUCKET}

# 3. application.yml
cloud:
  aws:
    s3:
      bucket: ${S3_BUCKET}  # ← 여기서 사용
```

#### 특징
- ✅ Spring Boot 애플리케이션 전용
- ✅ deploy.yml에 **설정 불필요**
- ✅ .env 파일에만 설정
- ✅ 영구 저장 (사용자 데이터)

---

## 📊 역할 분리 다이어그램

```
┌──────────────────────────────────────────────────────────────┐
│                    배포 전용 버킷                              │
│              kospot-bucket-deploy                             │
├──────────────────────────────────────────────────────────────┤
│  사용 주체: GitHub Actions                                   │
│  설정 위치: .github/workflows/deploy.yml                      │
│  변수명:    S3_BUCKET_NAME                                   │
│  .env:      ❌ 불필요                                         │
│  용도:      deploy-{sha}.zip 임시 저장                       │
│  수명:      30일 후 자동 삭제                                 │
└──────────────────────────────────────────────────────────────┘
                              │
                              │ 배포 패키지 업로드
                              ▼
                    ┌────────────────────┐
                    │   GitHub Actions   │
                    │   deploy.yml       │
                    └────────┬───────────┘
                             │
                             │ CodeDeploy
                             ▼
                    ┌────────────────────┐
                    │   EC2 Instance     │
                    └────────────────────┘


┌──────────────────────────────────────────────────────────────┐
│                  이미지 저장용 버킷                            │
│              kospot-images-prod                               │
├──────────────────────────────────────────────────────────────┤
│  사용 주체: Spring Boot Application                          │
│  설정 위치: .env → docker-compose.yml → application.yml     │
│  변수명:    S3_BUCKET                                        │
│  deploy.yml: ❌ 불필요                                        │
│  용도:      items/, banners/, photomode/ 이미지 저장         │
│  수명:      영구 저장                                         │
└──────────────────────────────────────────────────────────────┘
                              ▲
                              │ 이미지 업로드
                              │
                    ┌─────────┴──────────┐
                    │  Spring Boot App   │
                    │  application.yml   │
                    │  ${S3_BUCKET}      │
                    └────────────────────┘
                              ▲
                              │ .env에서 읽음
                              │
                    ┌─────────┴──────────┐
                    │  EC2: .env         │
                    │  S3_BUCKET=...    │
                    └────────────────────┘
```

---

## ✅ 현재 설정 확인

### deploy.yml (배포 전용)

```yaml
# .github/workflows/deploy.yml
env:
  S3_BUCKET_NAME: kospot-bucket-deploy  # ✅ 배포 전용

# 사용:
aws s3 cp deploy/deploy.zip s3://${{ env.S3_BUCKET_NAME }}/...
#                      ↑
#                GitHub Actions에서만 사용
```

### .env (이미지 저장용)

```bash
# EC2: /home/ubuntu/kospot/.env

# ❌ 배포용 버킷은 .env에 없어도 됨
# (GitHub Actions에서만 사용하므로)

# ✅ 이미지 저장용 버킷만 .env에 설정
S3_BUCKET=kospot-images-prod  # ← Spring Boot 애플리케이션에서 사용
```

### application.yml (이미지 저장용)

```yaml
# src/main/resources/application.yml
cloud:
  aws:
    s3:
      bucket: ${S3_BUCKET}  # ← .env의 S3_BUCKET 사용
                            # ← 배포용 버킷은 사용 안 함
```

---

## 🔍 변수명 구분

### 배포 전용 (deploy.yml)

```yaml
# 변수명: S3_BUCKET_NAME
# 사용 위치: GitHub Actions 워크플로우
# .env: 불필요

env:
  S3_BUCKET_NAME: kospot-bucket-deploy
```

### 이미지 저장용 (.env)

```bash
# 변수명: S3_BUCKET
# 사용 위치: Spring Boot 애플리케이션
# deploy.yml: 불필요

S3_BUCKET=kospot-images-prod
```

**주의**: 변수명이 다릅니다!
- `S3_BUCKET_NAME` → 배포 전용 (deploy.yml)
- `S3_BUCKET` → 이미지 저장용 (.env)

---

## ❓ 자주 묻는 질문

### Q1: 배포용 버킷을 .env에 넣어야 하나요?

**A**: ❌ **불필요합니다.**

```bash
# .env 파일
# ❌ 이렇게 하지 마세요!
# S3_BUCKET_DEPLOY=kospot-bucket-deploy

# 이유:
# - GitHub Actions에서만 사용
# - EC2/애플리케이션에서는 사용 안 함
# - deploy.yml에 이미 설정되어 있음
```

### Q2: 이미지 저장용 버킷을 deploy.yml에 넣어야 하나요?

**A**: ❌ **불필요합니다.**

```yaml
# .github/workflows/deploy.yml
env:
  # ❌ 이렇게 하지 마세요!
  # S3_BUCKET_IMAGES: kospot-images-prod

# 이유:
# - GitHub Actions는 배포만 담당
# - 이미지 업로드는 애플리케이션이 처리
# - .env에서 읽으면 됨
```

### Q3: 두 버킷이 섞이면 안 되나요?

**A**: ❌ **섞이면 안 됩니다!**

```yaml
# ❌ 잘못된 예시
# deploy.yml
env:
  S3_BUCKET_NAME: kospot-images-prod  # ← 배포용인데 이미지 버킷?

# .env
S3_BUCKET: kospot-bucket-deploy  # ← 이미지용인데 배포 버킷?
```

**문제점**:
- 배포 패키지가 이미지 버킷에 저장됨 (혼란)
- 이미지가 배포 버킷에 저장됨 (30일 후 삭제됨)
- 버킷 역할이 혼란스러워짐

**올바른 설정**:
```yaml
# ✅ deploy.yml
S3_BUCKET_NAME: kospot-bucket-deploy  # 배포 전용
```

```bash
# ✅ .env
S3_BUCKET=kospot-images-prod  # 이미지 전용
```

---

## 📝 체크리스트

### 배포 전용 버킷 (`kospot-bucket-deploy`)

- [ ] `.github/workflows/deploy.yml`에 `S3_BUCKET_NAME` 설정됨
- [ ] `.env` 파일에 **없음** (정상)
- [ ] `application.yml`에서 **사용 안 함** (정상)
- [ ] GitHub Actions에서만 접근
- [ ] CodeDeploy가 배포 패키지 다운로드

### 이미지 저장용 버킷 (`kospot-images-prod`)

- [ ] `.env` 파일에 `S3_BUCKET` 설정됨
- [ ] `docker-compose.yml`에서 환경변수로 전달됨
- [ ] `application.yml`에서 `${S3_BUCKET}` 사용됨
- [ ] `deploy.yml`에 **없음** (정상)
- [ ] Spring Boot 애플리케이션에서만 접근

---

## 🎯 핵심 정리

### 두 버킷은 완전히 분리됩니다

| 항목 | 배포 전용 | 이미지 저장용 |
|------|-----------|---------------|
| **버킷명** | `kospot-bucket-deploy` | `kospot-images-prod` |
| **변수명** | `S3_BUCKET_NAME` | `S3_BUCKET` |
| **설정 위치** | `deploy.yml` | `.env` |
| **사용 주체** | GitHub Actions | Spring Boot |
| **deploy.yml 필요** | ✅ **필요** | ❌ 불필요 |
| **.env 필요** | ❌ 불필요 | ✅ **필요** |

### 현재 설정이 올바릅니다! ✅

- ✅ `deploy.yml`: `S3_BUCKET_NAME: kospot-bucket-deploy` (배포 전용)
- ✅ `.env`: `S3_BUCKET=kospot-images-prod` (이미지 저장용)
- ✅ 두 버킷이 명확히 분리됨

---

**작성일**: 2025-10-31  
**프로젝트**: KoSpot Backend

