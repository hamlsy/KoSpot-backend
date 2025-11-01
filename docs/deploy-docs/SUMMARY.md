# 배포 시스템 및 환경변수 관리 완전 가이드 - 요약

> **KoSpot Backend 배포 시스템**  
> 작성일: 2025-10-31

---

## 📚 문서 구조

이번 작업으로 생성/업데이트된 문서들:

```
KoSpot-backend/
├── .dockerignore                              # [신규] Docker 이미지 보안
├── docker-compose.yml                         # [수정] 환경변수 일관성
├── docs/deploy/
│   ├── ACTION_PLAN.md                         # [신규] 실행 계획서
│   ├── DEPLOYMENT_README.md                   # [수정] S3 버킷 정보 추가
│   ├── ENV_MANAGEMENT_GUIDE.md                # [신규] 환경변수 완전 가이드
│   ├── S3_BUCKET_SETUP_GUIDE.md               # [신규] S3 설정 가이드
│   └── SUMMARY.md                             # [신규] 이 파일
└── KoSpot-backend-private/                    # [신규] Submodule
    ├── .env                                   # [생성 필요] 운영 환경변수
    └── README.md                              # [신규] Submodule 가이드
```

---

## 🎯 핵심 변경사항

### 1. 환경변수 관리 체계 확립

**변경 전**:
- ❌ `.env` 파일이 메인 리포지토리에 없음
- ❌ 환경변수가 하드코딩 또는 수동 관리
- ❌ 민감한 정보 노출 위험

**변경 후**:
- ✅ Private Submodule로 관리
- ✅ `.env` 파일로 운영 환경 관리
- ✅ Git으로 버전 관리 (Private 리포지토리)
- ✅ EC2에 수동 배치 (배포 패키지에 미포함)

### 2-1. 왜 .env를 수동 배치하나?

**핵심 이유: 보안**

```
❌ 자동 배치 (위험):
Docker 이미지에 환경변수 포함 → 이미지 유출 시 DB 비밀번호 노출

✅ 수동 배치 (안전):
Docker 이미지는 깨끗 → EC2에만 .env 존재 → 이미지 유출되어도 안전
```

**실무적 장점**:
- 최초 1회만 설정 (5분)
- 이후 배포는 완전 자동
- 같은 이미지로 여러 환경 배포 가능
- 환경변수 변경 시 이미지 재빌드 불필요

### 2. S3 버킷 구성

**기존**:
- `kospot-deploy-bucket` (CodeDeploy용)

**추가**:
- `kospot-images-prod` (이미지 저장용)
  - `banners/` - 배너 이미지
  - `photomode/` - 포토모드 이미지
  - `items/` - 아이템 이미지
  - `profiles/` - 프로필 이미지
  - `thumbnails/` - 썸네일 이미지

### 3. docker-compose.yml 개선

**변경 전**:
```yaml
environment:
  - SPRING_DATASOURCE_URL=${DB_URL}  # application.yml과 불일치
```

**변경 후**:
```yaml
environment:
  - DB_HOST=${DB_HOST}                # application.yml과 일치
  - DB_PORT=${DB_PORT}
  - DB_NAME=${DB_NAME}
  - S3_BUCKET=${S3_BUCKET}           # 이미지 버킷 추가
```

### 4. 보안 강화

- `.dockerignore` 추가 → 민감한 정보가 Docker 이미지에 포함되지 않음
- IAM 정책 세분화 → 최소 권한 원칙
- Presigned URL 사용 → 퍼블릭 액세스 차단

---

## 🔄 환경변수 흐름

```
┌──────────────────────────────────────────────────┐
│  개발자 PC                                        │
│  ├── application-local.yml (Git 포함)            │
│  └── KoSpot-backend-private/                     │
│      ├── .env.prod (Git - Private)               │
│      └── .env.test (Git - Private)               │
└────────────────┬─────────────────────────────────┘
                 │ Git Push
                 ▼
┌──────────────────────────────────────────────────┐
│  GitHub                                           │
│  ├── 메인 리포지토리 (Public)                    │
│  └── KoSpot-backend-private (Submodule Private)  │
└────────────────┬─────────────────────────────────┘
                 │ GitHub Actions
                 │ (Submodule 체크아웃하지만 이미지에 미포함)
                 ▼
┌──────────────────────────────────────────────────┐
│  S3: kospot-deploy-bucket                        │
│  └── deploy-{sha}.zip                            │
│      ├── docker-compose.yml                      │
│      ├── kospot-backend.tar                      │
│      └── scripts/                                │
└────────────────┬─────────────────────────────────┘
                 │ CodeDeploy
                 ▼
┌──────────────────────────────────────────────────┐
│  EC2: /home/ubuntu/kospot/                       │
│  ├── .env ← Submodule의 .env.prod 수동 배치     │
│  ├── docker-compose.yml                          │
│  └── scripts/start.sh → .env 로드               │
└────────────────┬─────────────────────────────────┘
                 │ docker-compose up
                 ▼
┌──────────────────────────────────────────────────┐
│  Docker Container                                 │
│  └── Spring Boot Application                     │
│      └── application.yml ← 환경변수 주입         │
└──────────────────────────────────────────────────┘
```

---

## 📋 당신이 해야 할 일 (우선순위 순)

### 🚨 최우선 (지금 바로)

1. **Private 리포지토리 생성**
   - 리포지토리명: `KoSpot-backend-private`
   - Visibility: Private

2. **Submodule 추가**
   ```bash
   git submodule add https://github.com/your-org/KoSpot-backend-private.git
   ```

3. **`.env.prod` 파일 작성**
   - 위치: `KoSpot-backend-private/.env.prod`
   - 템플릿: `KoSpot-backend-private/README.md` 참조
   - **실제 값**으로 모두 채우기

4. **GitHub Secrets 설정**
   - Name: `SUBMODULE_TOKEN`
   - Secret: Personal Access Token (repo 권한)

### ⚡ 높음 (오늘 내로)

5. **S3 버킷 생성**
   ```bash
   aws s3 mb s3://kospot-images-prod --region ap-northeast-2
   ```
   상세: `docs/deploy/S3_BUCKET_SETUP_GUIDE.md`

6. **IAM 정책 업데이트**
   - EC2 역할에 S3 접근 권한 추가
   - 파일: `docs/deploy/S3_BUCKET_SETUP_GUIDE.md` 2-2절

7. **EC2에 `.env` 파일 배치**
   ```bash
   # EC2에서
   cd /home/ubuntu/kospot
   nano .env
   # .env.prod 내용 붙여넣기
   chmod 600 .env
   ```

### 🔄 중간 (이번 주 내)

8. **배포 테스트**
   - 코드 푸시 → GitHub Actions 확인
   - EC2 배포 확인
   - S3 연동 테스트

9. **팀원에게 공유**
   - Slack 공지
   - Wiki 업데이트

### 📝 낮음 (여유 있을 때)

10. **CloudFront CDN 설정** (선택)
11. **Lambda 썸네일 자동 생성** (선택)
12. **모니터링 대시보드** (선택)

---

## 🔑 핵심 개념

### 1. 왜 Submodule을 사용하나?

- ✅ 민감한 정보를 Private 리포지토리에 격리
- ✅ Git으로 환경변수 버전 관리 가능
- ✅ 팀원 간 환경변수 공유 용이
- ✅ 메인 리포지토리를 Public으로 유지 가능

### 2. 왜 .env 파일을 EC2에 수동 배치하나?

- ✅ Docker 이미지에 민감한 정보 미포함
- ✅ S3 배포 패키지에 환경변수 미포함
- ✅ 보안 강화 (이미지가 유출되어도 안전)

### 3. application.yml과 docker-compose.yml의 관계

```yaml
# application.yml
spring:
  datasource:
    url: jdbc:mysql://${DB_HOST}:${DB_PORT}/${DB_NAME}
                        ↑         ↑         ↑
                        │         │         │
# docker-compose.yml    │         │         │
environment:            │         │         │
  - DB_HOST=${DB_HOST}──┘         │         │
  - DB_PORT=${DB_PORT}────────────┘         │
  - DB_NAME=${DB_NAME}──────────────────────┘
            ↑
            │
# .env 파일 │
DB_HOST=rds-endpoint
DB_PORT=3306
DB_NAME=kospot_prod
```

환경변수가 **3단계**로 전달됩니다:
1. `.env` → `docker-compose.yml`
2. `docker-compose.yml` → Docker Container 환경변수
3. Container 환경변수 → Spring Boot `application.yml`

---

## 🎓 유용한 명령어

### Submodule 관리

```bash
# Submodule 초기화
git submodule init

# Submodule 업데이트
git submodule update --remote

# Submodule 포함 클론
git clone --recurse-submodules <repo-url>
```

### S3 관리

```bash
# 버킷 목록
aws s3 ls

# 버킷 내용 확인
aws s3 ls s3://kospot-images-prod/

# 파일 업로드
aws s3 cp file.jpg s3://kospot-images-prod/items/

# 파일 다운로드
aws s3 cp s3://kospot-images-prod/items/file.jpg ./
```

### EC2 디버깅

```bash
# 환경변수 확인
docker-compose exec app env | grep DB_HOST

# 애플리케이션 로그
docker-compose logs -f app

# 헬스체크
curl http://localhost:8080/actuator/health

# 컨테이너 재시작
docker-compose restart app
```

---

## 📞 문제 해결

### Q1: "Could not resolve placeholder 'DB_HOST'"

**원인**: 환경변수가 주입되지 않음

**해결**:
```bash
# EC2에서
cat /home/ubuntu/kospot/.env  # 파일 확인
docker-compose exec app env | grep DB_HOST  # 컨테이너 환경변수 확인
```

### Q2: S3 접근 거부 (403 Forbidden)

**원인**: IAM 역할 권한 부족

**해결**:
```bash
# IAM 정책 확인
aws iam list-attached-role-policies --role-name KoSpotEC2CodeDeployRole

# 정책 내용 확인
aws iam get-policy-version --policy-arn <arn> --version-id v1
```

### Q3: GitHub Actions에서 Submodule 클론 실패

**원인**: `SUBMODULE_TOKEN` 없음 또는 만료

**해결**:
1. Personal Access Token 재생성
2. GitHub Secrets 업데이트
3. 워크플로우 재실행

### Q4: docker-compose가 .env 파일을 읽지 못함

**원인**: `start.sh`에서 환경변수를 export하지 않음

**해결**:
```bash
# start.sh 확인
cat scripts/start.sh

# export 명령이 있는지 확인
# export $(cat .env | grep -v '^#' | xargs)
```

---

## ✅ 최종 점검

배포 전 마지막 확인:

### Submodule
- [ ] Private 리포지토리 생성됨
- [ ] Submodule 추가 완료
- [ ] `.env` 실제 값 작성 완료
- [ ] GitHub Secrets 추가 완료

### AWS
- [ ] `kospot-images-prod` 버킷 생성
- [ ] IAM 정책 업데이트
- [ ] EC2에서 S3 접근 테스트 성공

### EC2
- [ ] `.env` 파일 배치
- [ ] 권한 600 설정
- [ ] docker-compose.yml 업데이트

### 배포
- [ ] GitHub Actions 빌드 성공
- [ ] CodeDeploy 배포 성공
- [ ] 헬스체크 통과

---

## 📚 참조 문서

| 문서 | 용도 |
|------|------|
| [ACTION_PLAN.md](ACTION_PLAN.md) | 단계별 실행 계획 |
| [ENV_MANAGEMENT_GUIDE.md](ENV_MANAGEMENT_GUIDE.md) | 환경변수 완전 가이드 |
| [S3_BUCKET_SETUP_GUIDE.md](S3_BUCKET_SETUP_GUIDE.md) | S3 설정 상세 가이드 |
| [DEPLOYMENT_README.md](DEPLOYMENT_README.md) | 배포 시스템 개요 |
| [EC2_SETUP_GUIDE.md](EC2_SETUP_GUIDE.md) | EC2 서버 설정 |
| [KoSpot-backend-private/README.md](../../KoSpot-backend-private/README.md) | Submodule 가이드 |

---

## 🎯 다음 단계

1. **지금 바로**: [ACTION_PLAN.md](ACTION_PLAN.md)를 열고 Phase 1부터 시작
2. **궁금한 점**: [ENV_MANAGEMENT_GUIDE.md](ENV_MANAGEMENT_GUIDE.md) 참조
3. **문제 발생**: 각 문서의 트러블슈팅 섹션 확인

---

**성공적인 배포를 기원합니다! 🚀**

---

**작성일:** 2025-10-31  
**버전:** 1.0.0  
**작성자:** Backend 팀

