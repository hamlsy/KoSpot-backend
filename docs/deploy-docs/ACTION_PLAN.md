# KoSpot Backend 배포 및 S3 설정 실행 계획

> **작성일**: 2025-10-31  
> **담당**: Backend 팀  
> **목적**: S3 이미지 버킷 추가 및 환경변수 관리 체계 확립

---

## 📋 실행 순서

이 문서는 **당신이 해야 할 작업**을 순서대로 정리한 실행 계획서입니다.

---

## 🎯 Phase 1: Submodule 설정 (최우선)

### 1-1. Private 리포지토리 생성

GitHub에서 **Private 리포지토리**를 생성합니다.

```bash
# GitHub 웹사이트에서:
# 1. New repository 클릭
# 2. Repository name: KoSpot-backend-private
# 3. Visibility: Private ⚠️ 중요!
# 4. Create repository
```

### 1-2. Submodule로 추가

메인 리포지토리에서 Submodule을 추가합니다.

```bash
# 메인 리포지토리 디렉토리에서
cd C:\KoSpot-backend

# Submodule 추가
git submodule add https://github.com/your-org/KoSpot-backend-private.git KoSpot-backend-private

# 확인
ls KoSpot-backend-private

# 커밋
git add .gitmodules KoSpot-backend-private
git commit -m "Add private configuration submodule"
git push
```

### 1-3. Submodule에 환경변수 파일 생성

```bash
cd KoSpot-backend-private

# .env.prod 파일 생성
# (이미 생성된 README.md의 템플릿 참조)
notepad .env.prod

# 내용 작성 후 저장
# Git에 커밋 (Private 리포지토리이므로 안전)
git add .env.prod
git commit -m "Add production environment variables"
git push
```

**`.env.prod` 필수 작성 항목**:
- ✅ RDS 엔드포인트 (실제 값)
- ✅ DB 비밀번호 (강력한 비밀번호)
- ✅ Redis 비밀번호
- ✅ JWT Secret (Base64, 32자 이상)
- ✅ OAuth2 클라이언트 ID/Secret (실제 값)
- ✅ AWS 액세스 키 (IAM 사용자)
- ✅ S3 버킷명: `kospot-images-prod`

### 1-4. GitHub Secrets 설정

GitHub Actions가 Submodule에 접근할 수 있도록 토큰을 설정합니다.

```bash
# 1. GitHub Personal Access Token 생성
#    GitHub → Settings → Developer settings → Personal access tokens → Tokens (classic)
#    - Note: "KoSpot Submodule Access"
#    - Expiration: No expiration (또는 1년)
#    - Scopes: repo (전체 체크)

# 2. GitHub Secrets에 추가
#    메인 리포지토리 → Settings → Secrets and variables → Actions
#    - Name: SUBMODULE_TOKEN
#    - Secret: (생성한 토큰 붙여넣기)
```

✅ **Phase 1 완료 확인**:
- [ ] Private 리포지토리 생성됨
- [ ] Submodule 추가 완료
- [ ] `.env.prod` 파일 작성 완료
- [ ] `SUBMODULE_TOKEN` GitHub Secrets 추가 완료

---

## 🪣 Phase 2: AWS S3 버킷 생성

### 2-1. 이미지 저장용 S3 버킷 생성

#### AWS Console 사용

1. AWS Console → S3 → **Create bucket**
2. 설정:
   - **Bucket name**: `kospot-images-prod`
   - **Region**: `ap-northeast-2` (Asia Pacific - Seoul)
   - **Block Public Access**: 모두 체크 ✅
   - **Bucket Versioning**: Enabled
   - **Default encryption**: SSE-S3
3. **Create bucket** 클릭

#### AWS CLI 사용 (선호)

```bash
# 1. 버킷 생성
aws s3 mb s3://kospot-images-prod --region ap-northeast-2

# 2. 버저닝 활성화
aws s3api put-bucket-versioning \
  --bucket kospot-images-prod \
  --versioning-configuration Status=Enabled

# 3. 퍼블릭 액세스 차단
aws s3api put-public-access-block \
  --bucket kospot-images-prod \
  --public-access-block-configuration \
    "BlockPublicAcls=true,IgnorePublicAcls=true,BlockPublicPolicy=true,RestrictPublicBuckets=true"

# 4. 암호화 활성화
aws s3api put-bucket-encryption \
  --bucket kospot-images-prod \
  --server-side-encryption-configuration \
    '{"Rules": [{"ApplyServerSideEncryptionByDefault": {"SSEAlgorithm": "AES256"},"BucketKeyEnabled": true}]}'

# 5. 폴더 구조 생성
aws s3api put-object --bucket kospot-images-prod --key banners/
aws s3api put-object --bucket kospot-images-prod --key photomode/
aws s3api put-object --bucket kospot-images-prod --key items/
aws s3api put-object --bucket kospot-images-prod --key profiles/
aws s3api put-object --bucket kospot-images-prod --key thumbnails/

# 6. 확인
aws s3 ls s3://kospot-images-prod/
```

### 2-2. IAM 정책 업데이트

EC2 인스턴스가 새 S3 버킷에 접근할 수 있도록 IAM 정책을 수정합니다.

#### 프로젝트 루트에 s3-access-policy.json 파일 생성

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Sid": "CodeDeployBucketReadOnly",
      "Effect": "Allow",
      "Action": [
        "s3:GetObject",
        "s3:GetObjectVersion",
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
        "s3:PutObjectAcl",
        "s3:GetObjectVersion"
      ],
      "Resource": [
        "arn:aws:s3:::kospot-images-prod",
        "arn:aws:s3:::kospot-images-prod/*"
      ]
    }
  ]
}
```

#### 정책 생성 및 연결

```bash
# 1. IAM 정책 생성
aws iam create-policy \
  --policy-name KoSpotS3AccessPolicy \
  --policy-document file://s3-access-policy.json

# 2. EC2 역할에 정책 연결
# (YOUR_ACCOUNT_ID를 실제 AWS 계정 ID로 변경)
aws iam attach-role-policy \
  --role-name KoSpotEC2CodeDeployRole \
  --policy-arn arn:aws:iam::YOUR_ACCOUNT_ID:policy/KoSpotS3AccessPolicy

# 3. 확인
aws iam list-attached-role-policies --role-name KoSpotEC2CodeDeployRole
```

✅ **Phase 2 완료 확인**:
- [ ] `kospot-images-prod` 버킷 생성 완료
- [ ] 버킷 설정 완료 (버저닝, 암호화, 퍼블릭 액세스 차단)
- [ ] 폴더 구조 생성 완료
- [ ] IAM 정책 생성 및 연결 완료

---

## 🖥️ Phase 3: EC2 서버 설정

### 3-1. EC2에 환경변수 파일 배치

```bash
# 1. EC2 SSH 접속
ssh -i your-key.pem ubuntu@your-ec2-public-ip

# 2. 프로젝트 디렉토리 이동
cd /home/ubuntu/kospot

# 3. .env 파일 생성
nano .env

# 4. Submodule의 .env 내용을 복사해서 붙여넣기
# (로컬 PC에서 KoSpot-backend-private/.env 내용 복사)

# 5. 실제 값으로 수정
# - DB_HOST: 실제 RDS 엔드포인트
# - DB_PASSWORD: 실제 DB 비밀번호
# - AWS_ACCESS_KEY: 실제 IAM 액세스 키
# - S3_BUCKET: kospot-images-prod
# - 기타 모든 값

# 6. 저장
# Ctrl+X → Y → Enter

# 7. 권한 설정 (중요!)
chmod 600 .env

# 8. 확인
cat .env
```

### 3-2. S3 접근 테스트

```bash
# EC2에서 S3 버킷 접근 테스트
aws s3 ls s3://kospot-images-prod/

# 테스트 파일 업로드
echo "test" > test.txt
aws s3 cp test.txt s3://kospot-images-prod/test.txt

# 테스트 파일 다운로드
aws s3 cp s3://kospot-images-prod/test.txt ./downloaded.txt
cat downloaded.txt

# 테스트 파일 삭제
aws s3 rm s3://kospot-images-prod/test.txt
rm test.txt downloaded.txt

# 성공하면 ✅  
```

✅ **Phase 3 완료 확인**:
- [ ] EC2에 `.env` 파일 배치 완료
- [ ] `.env` 파일 권한 600 설정 완료
- [ ] S3 접근 테스트 성공

---

## 🚀 Phase 4: 배포 테스트

### 4-1. 로컬에서 변경사항 푸시

```bash
# 로컬 PC에서 메인 리포지토리로 이동
cd C:\KoSpot-backend

# 현재 브랜치 확인
git branch

# main/master 브랜치로 이동 (배포 브랜치)
git checkout main  # 또는 master

# Submodule 업데이트 반영
git add .
git commit -m "feat: Add S3 image bucket support and environment management"
git push origin main
```

### 4-2. GitHub Actions 모니터링

```bash
# 1. GitHub 웹사이트에서 Actions 탭 확인
#    https://github.com/your-org/KoSpot-backend/actions

# 2. 워크플로우 진행 상황 확인
#    - ✅ Build & Test
#    - ✅ Docker 이미지 빌드
#    - ✅ S3 업로드
#    - ✅ CodeDeploy 배포

# 3. 실패 시 로그 확인
```

### 4-3. EC2에서 배포 확인

```bash
# EC2 SSH 접속
ssh -i your-key.pem ubuntu@your-ec2-ip

# 1. CodeDeploy 로그 확인
sudo tail -f /var/log/aws/codedeploy-agent/codedeploy-agent.log

# 2. 컨테이너 상태 확인
cd /home/ubuntu/kospot
docker-compose ps

# 3. 애플리케이션 로그 확인
docker-compose logs -f app

# 4. 헬스체크
curl http://localhost:8080/actuator/health

# 5. 환경변수 확인
docker-compose exec app env | grep S3_BUCKET
# 출력: S3_BUCKET=kospot-images-prod
```

### 4-4. S3 연동 확인

애플리케이션에서 S3에 이미지를 업로드하는 기능이 있다면 테스트합니다.

```bash
# Postman이나 curl로 이미지 업로드 API 테스트
curl -X POST http://your-ec2-ip/api/images/upload \
  -H "Content-Type: multipart/form-data" \
  -F "file=@test-image.jpg" \
  -F "category=items"

# S3 버킷에서 확인
aws s3 ls s3://kospot-images-prod/items/
```

✅ **Phase 4 완료 확인**:
- [ ] GitHub Actions 빌드 성공
- [ ] CodeDeploy 배포 성공
- [ ] EC2 컨테이너 정상 실행
- [ ] 헬스체크 통과
- [ ] S3 버킷 환경변수 확인
- [ ] S3 이미지 업로드 테스트 성공

---

## 📝 Phase 5: 문서화 및 팀 공유

### 5-1. 팀원에게 공유할 정보

Slack 또는 팀 채널에 다음 내용을 공유하세요:

```
📢 **배포 환경 업데이트 공지**

1. **새로운 S3 버킷 추가**
   - 버킷명: `kospot-images-prod`
   - 용도: 배너, 포토모드, 아이템 이미지 저장
   - 폴더: banners/, photomode/, items/, profiles/, thumbnails/

2. **환경변수 관리 변경**
   - Private Submodule로 관리: `KoSpot-backend-private`
   - 환경변수 수정 시 Submodule 리포지토리에서 수정 후 EC2 .env 파일 업데이트 필요

3. **관련 문서**
   - docs/deploy/ENV_MANAGEMENT_GUIDE.md
   - docs/deploy/S3_BUCKET_SETUP_GUIDE.md
   - KoSpot-backend-private/README.md

4. **주의사항**
   - `.env` 파일을 절대 Public 리포지토리에 커밋하지 마세요
   - AWS 자격 증명을 Slack에 공유하지 마세요
   - 환경변수 변경 시 팀에 공지해주세요
```

### 5-2. Wiki 또는 Confluence 업데이트

프로젝트 Wiki에 다음 페이지를 추가/업데이트하세요:

- **환경변수 관리 가이드**
- **S3 버킷 사용 가이드**
- **배포 프로세스**

✅ **Phase 5 완료 확인**:
- [ ] 팀원에게 Slack 공지 완료
- [ ] Wiki/Confluence 업데이트 완료

---

## 🎓 추가 작업 (선택사항)

### CloudFront CDN 설정 (이미지 전송 가속화)

```bash
# CloudFront Distribution 생성
# AWS Console → CloudFront → Create Distribution
# - Origin: kospot-images-prod.s3.ap-northeast-2.amazonaws.com
# - Origin access: Origin access control (OAC)
# - Viewer protocol policy: Redirect HTTP to HTTPS
# - Price class: Use North America, Europe, and Asia

# 생성 후 도메인:
# https://d1234567890abc.cloudfront.net/
```

### Lambda로 썸네일 자동 생성

S3에 이미지 업로드 시 자동으로 썸네일을 생성하는 Lambda 함수 설정.

### 이미지 최적화 파이프라인

업로드된 이미지를 자동으로 최적화하는 Lambda 함수 설정.

---

## 🆘 문제 해결

### 문제 1: Submodule 클론 실패

```bash
# 해결: Personal Access Token 재생성 및 재설정
git submodule update --init --recursive
```

### 문제 2: EC2에서 S3 접근 거부 (403 Forbidden)

```bash
# 원인: IAM 역할 권한 부족
# 해결: IAM 정책 다시 확인 및 연결
aws iam list-attached-role-policies --role-name KoSpotEC2CodeDeployRole
```

### 문제 3: 환경변수 주입 실패

```bash
# 원인: docker-compose.yml과 application.yml 불일치
# 해결: docker-compose.yml 환경변수 이름 확인
# - DB_HOST (O)
# - SPRING_DATASOURCE_URL (X)
```

### 문제 4: 배포 후 헬스체크 실패

```bash
# EC2에서 로그 확인
docker-compose logs app

# 일반적인 원인:
# 1. DB 연결 실패 → RDS 보안 그룹 확인
# 2. Redis 연결 실패 → Redis 컨테이너 상태 확인
# 3. S3 접근 실패 → IAM 권한 확인
```

---

## ✅ 최종 체크리스트

배포가 완전히 완료되었는지 확인하세요:

### Submodule 설정
- [ ] Private 리포지토리 생성
- [ ] Submodule 추가
- [ ] `.env.prod` 파일 작성
- [ ] GitHub Secrets `SUBMODULE_TOKEN` 추가

### AWS 리소스
- [ ] S3 버킷 `kospot-images-prod` 생성
- [ ] 버킷 설정 (버저닝, 암호화, 퍼블릭 액세스 차단)
- [ ] IAM 정책 업데이트
- [ ] EC2에서 S3 접근 테스트 성공

### EC2 서버
- [ ] `.env` 파일 배치
- [ ] `.env` 권한 600 설정
- [ ] S3 접근 테스트 성공

### 배포 및 테스트
- [ ] GitHub Actions 빌드 성공
- [ ] CodeDeploy 배포 성공
- [ ] 컨테이너 정상 실행
- [ ] 헬스체크 통과
- [ ] S3 연동 확인

### 문서 및 공유
- [ ] 팀원에게 공지
- [ ] Wiki 업데이트

---

## 📞 지원

문제 발생 시:
1. [ENV_MANAGEMENT_GUIDE.md](ENV_MANAGEMENT_GUIDE.md) 트러블슈팅 섹션 참조
2. [S3_BUCKET_SETUP_GUIDE.md](S3_BUCKET_SETUP_GUIDE.md) 참조
3. GitHub Actions 로그 확인
4. EC2 로그 확인: `docker-compose logs -f app`
5. 팀 Slack 채널에 질문

---

**작성일:** 2025-10-31  
**버전:** 1.0.0  
**담당:** Backend 팀  
**다음 리뷰일:** 2025-11-30

