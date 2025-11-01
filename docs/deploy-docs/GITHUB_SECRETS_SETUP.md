# GitHub Secrets 설정 가이드

## 📋 필요한 GitHub Secrets 목록

GitHub Actions에서 사용하는 Secrets는 **정확히 3개**입니다:

| Secret 이름 | 용도 | 사용 위치 |
|------------|------|-----------|
| `AWS_ACCESS_KEY_ID` | AWS 서비스 접근 (S3 업로드, CodeDeploy) | deploy.yml |
| `AWS_SECRET_ACCESS_KEY` | AWS 서비스 접근 (S3 업로드, CodeDeploy) | deploy.yml |
| `SUBMODULE_TOKEN` | Private Submodule 체크아웃 | deploy.yml |

---

## 🔑 1. SUBMODULE_TOKEN

### 용도
Private Submodule (`KoSpot-backend-private`)를 체크아웃하기 위한 GitHub Personal Access Token입니다.

### 생성 방법

1. **GitHub → Settings → Developer settings**
   - 웹사이트: https://github.com/settings/tokens

2. **Personal access tokens → Tokens (classic) → Generate new token**

3. **설정**:
   ```
   Note: KoSpot Submodule Access
   Expiration: No expiration (또는 1년)
   Scopes:
     ✅ repo (모든 체크박스)
       ✅ repo:status
       ✅ repo_deployment
       ✅ public_repo
       ✅ repo:invite
       ✅ security_events
   ```

4. **Generate token** 클릭

5. **토큰 복사** (한 번만 보여줌!)

### GitHub Secrets에 등록

```
Repository → Settings → Secrets and variables → Actions → New repository secret

Name: SUBMODULE_TOKEN
Secret: (복사한 토큰 붙여넣기)
```

### 사용 위치

```yaml
# .github/workflows/deploy.yml (27번째 줄)
- name: Checkout code
  uses: actions/checkout@v4
  with:
    submodules: true
    token: ${{ secrets.SUBMODULE_TOKEN }}  # ← 여기서 사용
```

---

## 🔑 2. AWS_ACCESS_KEY_ID

### 용도
GitHub Actions에서 AWS 서비스(S3, CodeDeploy)에 접근하기 위한 IAM 사용자의 Access Key ID입니다.

### 생성 방법

#### IAM 사용자 생성

```bash
# AWS CLI 사용
aws iam create-user --user-name kospot-github-actions

# 또는 AWS Console
# IAM → Users → Add users
# User name: kospot-github-actions
# Access type: Programmatic access
```

#### 정책 생성 및 연결

**정책 이름**: `KoSpotGitHubActionsPolicy`

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Sid": "S3DeployBucketAccess",
      "Effect": "Allow",
      "Action": [
        "s3:PutObject",
        "s3:GetObject",
        "s3:ListBucket"
      ],
      "Resource": [
        "arn:aws:s3:::kospot-deploy-bucket",
        "arn:aws:s3:::kospot-deploy-bucket/*"
      ]
    },
    {
      "Sid": "CodeDeployAccess",
      "Effect": "Allow",
      "Action": [
        "codedeploy:CreateDeployment",
        "codedeploy:GetDeployment",
        "codedeploy:GetDeploymentConfig",
        "codedeploy:GetApplicationRevision",
        "codedeploy:RegisterApplicationRevision"
      ],
      "Resource": "*"
    }
  ]
}
```

**정책 생성 및 연결**:

```bash
# 정책 생성
aws iam create-policy \
  --policy-name KoSpotGitHubActionsPolicy \
  --policy-document file://github-actions-policy.json

# 사용자에 정책 연결
aws iam attach-user-policy \
  --user-name kospot-github-actions \
  --policy-arn arn:aws:iam::YOUR_ACCOUNT_ID:policy/KoSpotGitHubActionsPolicy
```

#### Access Key 생성

```bash
# Access Key 생성
aws iam create-access-key --user-name kospot-github-actions

# 출력:
# {
#   "AccessKey": {
#     "UserName": "kospot-github-actions",
#     "AccessKeyId": "AKIAIOSFODNN7EXAMPLE",
#     "Status": "Active",
#     "SecretAccessKey": "wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY",
#     "CreateDate": "2025-10-31T12:00:00Z"
#   }
# }
```

**또는 AWS Console**:
1. IAM → Users → `kospot-github-actions`
2. Security credentials 탭
3. Create access key
4. Use case: Application running outside AWS
5. Create access key
6. **Access Key ID**와 **Secret Access Key** 복사

### GitHub Secrets에 등록

```
Repository → Settings → Secrets and variables → Actions → New repository secret

Name: AWS_ACCESS_KEY_ID
Secret: (Access Key ID 붙여넣기, 예: AKIAIOSFODNN7EXAMPLE)
```

### 사용 위치

```yaml
# .github/workflows/deploy.yml (63번째 줄)
- name: Configure AWS credentials
  uses: aws-actions/configure-aws-credentials@v4
  with:
    aws-access-key-id: ${{ secrets.AWS_ACCESS_KEY_ID }}  # ← 여기서 사용
    aws-secret-access-key: ${{ secrets.AWS_SECRET_ACCESS_KEY }}
```

---

## 🔑 3. AWS_SECRET_ACCESS_KEY

### 용도
`AWS_ACCESS_KEY_ID`와 함께 사용하는 Secret Key입니다.

### 생성 방법
위의 "AWS_ACCESS_KEY_ID" 섹션에서 함께 생성됩니다.

### GitHub Secrets에 등록

```
Repository → Settings → Secrets and variables → Actions → New repository secret

Name: AWS_SECRET_ACCESS_KEY
Secret: (Secret Access Key 붙여넣기, 예: wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY)
```

### 사용 위치

```yaml
# .github/workflows/deploy.yml (64번째 줄)
- name: Configure AWS credentials
  uses: aws-actions/configure-aws-credentials@v4
  with:
    aws-access-key-id: ${{ secrets.AWS_ACCESS_KEY_ID }}
    aws-secret-access-key: ${{ secrets.AWS_SECRET_ACCESS_KEY }}  # ← 여기서 사용
```

---

## ⚠️ 중요: AWS 키의 역할 분리

### GitHub Actions용 AWS 키 (GitHub Secrets)

```
용도: GitHub Actions → S3 업로드, CodeDeploy 배포
설정: GitHub Secrets
사용: deploy.yml

권한:
- S3: kospot-deploy-bucket 읽기/쓰기
- CodeDeploy: 배포 생성
```

### EC2용 AWS 키 (.env 파일)

```
용도: Spring Boot → S3 이미지 업로드
설정: EC2의 .env 파일
사용: application.yml

권한:
- S3: kospot-images-prod 읽기/쓰기
- (CodeDeploy 접근 불필요)
```

**왜 분리?**
- GitHub Actions는 배포만 담당
- Spring Boot 애플리케이션은 이미지 업로드만 담당
- 최소 권한 원칙 (각각 필요한 권한만)

---

## ✅ 최종 체크리스트

### GitHub Secrets 등록 확인

```
Repository → Settings → Secrets and variables → Actions

✅ AWS_ACCESS_KEY_ID         (IAM 사용자: kospot-github-actions)
✅ AWS_SECRET_ACCESS_KEY     (IAM 사용자: kospot-github-actions)
✅ SUBMODULE_TOKEN           (GitHub Personal Access Token)
```

### IAM 사용자 권한 확인

```bash
# 정책 연결 확인
aws iam list-attached-user-policies --user-name kospot-github-actions

# Access Key 확인
aws iam list-access-keys --user-name kospot-github-actions
```

### 배포 테스트

```bash
# GitHub에서 main 브랜치에 푸시
git push origin main

# GitHub Actions 탭에서 워크플로우 확인
# - ✅ 빌드 성공
# - ✅ S3 업로드 성공
# - ✅ CodeDeploy 배포 성공
```

---

## 🔒 보안 주의사항

### ⚠️ 절대 하지 말아야 할 것

1. ❌ **GitHub Secrets를 코드에 커밋**
2. ❌ **Slack, Discord 등에 Secrets 공유**
3. ❌ **스크린샷에 Secrets 노출**
4. ❌ **개인 계정의 AWS 키 사용** (IAM 사용자 사용)

### ✅ 해야 할 것

1. ✅ **IAM 사용자별로 별도 키 생성**
2. ✅ **최소 권한 원칙** (필요한 권한만)
3. ✅ **정기적인 키 로테이션** (3-6개월마다)
4. ✅ **액세스 키 사용량 모니터링** (CloudTrail)

---

## 🆘 문제 해결

### 문제 1: Submodule 체크아웃 실패

```
Error: fatal: could not read Username
```

**원인**: `SUBMODULE_TOKEN`이 없거나 만료됨

**해결**:
1. GitHub Secrets에 `SUBMODULE_TOKEN` 등록 확인
2. Personal Access Token 권한 확인 (`repo` 스코프 필요)
3. 토큰 재생성 후 업데이트

### 문제 2: S3 업로드 실패 (403 Forbidden)

```
Error: upload failed: deploy/deploy.zip to s3://... An error occurred (403)
```

**원인**: IAM 사용자 권한 부족

**해결**:
```bash
# IAM 정책 확인
aws iam list-attached-user-policies --user-name kospot-github-actions

# 정책 내용 확인
aws iam get-policy-version \
  --policy-arn arn:aws:iam::ACCOUNT_ID:policy/KoSpotGitHubActionsPolicy \
  --version-id v1

# S3 버킷 이름 확인 (kospot-deploy-bucket)
```

### 문제 3: CodeDeploy 배포 실패 (AccessDenied)

```
Error: User: arn:aws:iam::... is not authorized to perform: codedeploy:CreateDeployment
```

**원인**: CodeDeploy 권한 없음

**해결**:
```bash
# 정책에 CodeDeploy 권한 추가
# 위의 "AWS_ACCESS_KEY_ID" 섹션의 정책 참조
```

---

## 📝 요약

### GitHub Secrets 등록 항목 (3개)

```
✅ AWS_ACCESS_KEY_ID         (GitHub Actions → AWS)
✅ AWS_SECRET_ACCESS_KEY     (GitHub Actions → AWS)
✅ SUBMODULE_TOKEN           (GitHub Actions → Private Submodule)
```

### EC2 .env 파일 항목 (별도)

```
# EC2의 .env 파일에만 들어가는 것
AWS_ACCESS_KEY=...           # Spring Boot 애플리케이션용 (다른 IAM 사용자)
AWS_SECRET_KEY=...
S3_BUCKET=kospot-images-prod

# ⚠️ GitHub Secrets와는 별개!
```

---

**작성일**: 2025-10-31  
**프로젝트**: KoSpot Backend

