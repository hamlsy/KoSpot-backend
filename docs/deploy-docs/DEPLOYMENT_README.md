# KoSpot Backend 배포 시스템 개요

## 🚀 배포 아키텍처

```
┌─────────────────────────────────────────────────────────────────┐
│                        GitHub Repository                         │
│  ┌──────────────┐  ┌────────────────┐  ┌──────────────────┐   │
│  │   Source     │  │  Submodule     │  │   Workflows      │   │
│  │    Code      │  │  (Private)     │  │  (.github/)      │   │
│  └──────────────┘  └────────────────┘  └──────────────────┘   │
└───────────────────────────┬─────────────────────────────────────┘
                            │ Push to main
                            ▼
┌─────────────────────────────────────────────────────────────────┐
│                      GitHub Actions                              │
│  ┌─────────┐  ┌─────────┐  ┌──────────┐  ┌───────────────┐   │
│  │  Build  │→ │  Test   │→ │  Docker  │→ │  Create Zip   │   │
│  └─────────┘  └─────────┘  └──────────┘  └───────────────┘   │
└───────────────────────────┬─────────────────────────────────────┘
                            │ Upload
                            ▼
┌─────────────────────────────────────────────────────────────────┐
│                        AWS S3 Bucket                             │
│              (deploy-{commit-sha}.zip 저장)                      │
└───────────────────────────┬─────────────────────────────────────┘
                            │ Deploy
                            ▼
┌─────────────────────────────────────────────────────────────────┐
│                      AWS CodeDeploy                              │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌─────────────┐   │
│  │   Stop   │→ │  Install │→ │  Start   │→ │  Validate   │   │
│  └──────────┘  └──────────┘  └──────────┘  └─────────────┘   │
└───────────────────────────┬─────────────────────────────────────┘
                            │ Deploy to
                            ▼
┌─────────────────────────────────────────────────────────────────┐
│                        EC2 Instance                              │
│  ┌────────────────────────────────────────────────────────┐    │
│  │                       Nginx                             │    │
│  │  ┌───────────────────────────────────────────────┐    │    │
│  │  │ Port 80/443 → Reverse Proxy                    │    │    │
│  │  └───────────────────────────────────────────────┘    │    │
│  └────────────────────────────────────────────────────────┘    │
│                            │                                     │
│  ┌────────────────────────┴────────────────────────────────┐   │
│  │              Docker Compose Network                      │   │
│  │  ┌─────────────────┐      ┌──────────────────┐         │   │
│  │  │  Spring Boot    │      │      Redis       │         │   │
│  │  │  (Port 8080)    │◄────►│   (Port 6379)    │         │   │
│  │  └─────────────────┘      └──────────────────┘         │   │
│  └──────────────────────────────────────────────────────────┘   │
└───────────────────────────┬─────────────────────────────────────┘
                            │ Connect to
                            ▼
┌─────────────────────────────────────────────────────────────────┐
│                        AWS RDS MySQL                             │
└─────────────────────────────────────────────────────────────────┘
```

---

## 📁 생성된 파일 목록

### 1. Docker 관련
- `Dockerfile` - Spring Boot 애플리케이션 컨테이너화
- `docker-compose.yml` - Redis + Spring Boot 멀티 컨테이너 구성

### 2. CI/CD 관련
- `.github/workflows/deploy.yml` - GitHub Actions 워크플로우
- `appspec.yml` - CodeDeploy 배포 설정

### 3. 배포 스크립트
- `scripts/stop.sh` - 기존 컨테이너 중지
- `scripts/before_install.sh` - 설치 전 준비
- `scripts/after_install.sh` - Docker 이미지 로드
- `scripts/start.sh` - 애플리케이션 시작
- `scripts/validate.sh` - 배포 검증

### 4. Nginx 설정
- `nginx/nginx.conf` - Nginx 메인 설정 (참고용)
- `nginx/kospot.conf` - KoSpot 프로젝트 전용 설정

### 5. 문서
- `docs/DEPLOYMENT_GUIDE.md` - 전체 배포 가이드
- `docs/EC2_SETUP_GUIDE.md` - EC2 서버 설정 상세 가이드
- `docs/NGINX_SETUP_GUIDE.md` - Nginx 설정 상세 가이드
- `docs/ENV_TEMPLATE.md` - 환경변수 설정 가이드
- `docs/GITHUB_ACTIONS_SETUP.md` - GitHub Actions 설정 가이드

---

## 🔑 환경변수 설정

### ⚠️ 중요: Submodule 설정

환경변수는 **Private Submodule** (`KoSpot-backend-private`)에서 관리됩니다.

**상세 가이드**: [ENV_MANAGEMENT_GUIDE.md](ENV_MANAGEMENT_GUIDE.md)

### Submodule에 추가할 파일

**`KoSpot-backend-private/.env`** 파일을 생성하고 다음 내용을 입력하세요:

> **왜 .env를 수동 배치하나요?**  
> 보안 때문입니다. Docker 이미지나 S3 배포 패키지에 민감정보가 포함되면 유출 위험이 있습니다.  
> EC2에만 .env를 두면 파이프라인 어디에도 비밀번호가 남지 않아 안전합니다.

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
REDIS_HOST=redis
REDIS_PORT=6379
REDIS_PASSWORD=your-strong-redis-password-here

# -------------------- JWT --------------------
JWT_SECRET=your-base64-encoded-256-bit-secret-key-here

# -------------------- OAuth2 --------------------
OAUTH_GOOGLE_CLIENT_ID=your-google-client-id.apps.googleusercontent.com
OAUTH_GOOGLE_CLIENT_SECRET=your-google-client-secret
OAUTH_NAVER_CLIENT_ID=your-naver-client-id
OAUTH_NAVER_CLIENT_SECRET=your-naver-client-secret
OAUTH_KAKAO_CLIENT_ID=your-kakao-client-id
OAUTH_KAKAO_CLIENT_SECRET=your-kakao-client-secret

# -------------------- AWS Configuration --------------------
AWS_ACCESS_KEY=your-aws-access-key-id
AWS_SECRET_KEY=your-aws-secret-access-key
AWS_REGION=ap-northeast-2

# -------------------- AWS S3 Buckets --------------------
# 이미지 저장용 버킷 (배너, 포토모드, 아이템 등)
S3_BUCKET=kospot-images-prod

# -------------------- Application --------------------
SERVER_PORT=8080
SPRING_PROFILES_ACTIVE=prod

# -------------------- CORS --------------------
CORS_FRONT_URL=https://kospot.example.com

# -------------------- WebSocket --------------------
WEBSOCKET_ALLOWED_ORIGINS=https://kospot.example.com

# -------------------- AES Encryption --------------------
AES_SECRET_KEY=your-aes-encryption-key-here
```

**이 파일을 EC2 서버의 `/home/ubuntu/kospot/.env` 경로에 수동으로 배치하세요.**

---

## 📝 배포 전 체크리스트

### AWS 인프라
- [ ] EC2 인스턴스 생성 (Ubuntu 22.04, t3.medium 이상)
- [ ] RDS MySQL 인스턴스 생성
- [ ] S3 Bucket 생성
  - [ ] `kospot-deploy-bucket` (CodeDeploy용)
  - [ ] `kospot-images-prod` (이미지 저장용) **[신규]**
- [ ] IAM 역할 생성
  - [ ] EC2용 역할: S3 읽기/쓰기 권한
  - [ ] CodeDeploy용 역할: AWSCodeDeployRole
  - [ ] GitHub Actions용 IAM 사용자
- [ ] 보안 그룹 설정
  - [ ] EC2: 22(SSH), 80(HTTP), 443(HTTPS)
  - [ ] RDS: 3306 (EC2 보안 그룹에서만)
- [ ] CodeDeploy 애플리케이션 및 배포 그룹 생성

### EC2 서버 설정
- [ ] Docker 설치
- [ ] Docker Compose 설치
- [ ] Nginx 설치 및 설정
- [ ] CodeDeploy Agent 설치
- [ ] 프로젝트 디렉토리 생성 (`/home/ubuntu/kospot`)
- [ ] 환경변수 파일 배치 (`.env`)
- [ ] SSL 인증서 발급 (Let's Encrypt)

### GitHub 설정
- [ ] GitHub Secrets 추가
  - [ ] `AWS_ACCESS_KEY_ID`
  - [ ] `AWS_SECRET_ACCESS_KEY`
  - [ ] `SUBMODULE_TOKEN`
- [ ] Workflow 파일 환경변수 수정
  - [ ] S3 버킷 이름
  - [ ] CodeDeploy 애플리케이션 이름
  - [ ] CodeDeploy 배포 그룹 이름

---

## 🚀 배포 순서

### 1단계: AWS 인프라 구축
```bash
# 1. EC2 인스턴스 생성
# 2. RDS 인스턴스 생성
# 3. S3 Bucket 생성
# 4. IAM 역할 생성
# 5. CodeDeploy 애플리케이션 생성
```

### 2단계: EC2 서버 초기 설정
```bash
# SSH 접속
ssh -i your-key.pem ubuntu@your-ec2-ip

# 시스템 업데이트
sudo apt update && sudo apt upgrade -y

# Docker 설치
curl -fsSL https://get.docker.com -o get-docker.sh
sudo sh get-docker.sh
sudo usermod -aG docker ubuntu

# Docker Compose 설치
sudo curl -L "https://github.com/docker/compose/releases/latest/download/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose
sudo chmod +x /usr/local/bin/docker-compose

# Nginx 설치
sudo apt install -y nginx

# CodeDeploy Agent 설치
wget https://aws-codedeploy-ap-northeast-2.s3.ap-northeast-2.amazonaws.com/latest/install
chmod +x ./install
sudo ./install auto
```

### 3단계: Nginx 설정
```bash
# Nginx 설정 파일 복사
sudo nano /etc/nginx/sites-available/kospot.conf
# (nginx/kospot.conf 내용 붙여넣기)

# 심볼릭 링크 생성
sudo ln -s /etc/nginx/sites-available/kospot.conf /etc/nginx/sites-enabled/

# 설정 테스트 및 재시작
sudo nginx -t
sudo systemctl restart nginx
```

### 4단계: 환경변수 설정
```bash
# 환경변수 파일 생성
cd /home/ubuntu/kospot
nano .env
# (환경변수 내용 붙여넣기)

# 권한 설정
chmod 600 .env
```

### 5단계: GitHub 설정
```bash
# 1. GitHub Secrets 추가 (웹 UI)
# 2. Workflow 파일 확인 및 수정 (필요 시)
```

### 6단계: 배포 실행
```bash
# main 브랜치에 푸시
git push origin main

# GitHub Actions에서 자동 배포 시작
# AWS CodeDeploy에서 EC2로 배포
```

---

## ✅ 배포 검증

### 1. EC2에서 확인
```bash
# 컨테이너 상태
docker-compose ps

# 애플리케이션 로그
docker-compose logs -f app

# 헬스체크
curl http://localhost:8080/actuator/health
```

### 2. 브라우저에서 확인
```bash
# HTTP 접속
http://your-ec2-public-ip/actuator/health

# 도메인 설정 후
https://your-domain.com/actuator/health
```

### 3. API 테스트
```bash
# API 엔드포인트 테스트
curl https://your-domain.com/api/v1/test

# WebSocket 연결 테스트
# (WebSocket 클라이언트 도구 사용)
```

---

## 🔄 배포 플로우

### 자동 배포 (CI/CD)
```
1. 코드 변경 → main 브랜치 푸시
2. GitHub Actions 트리거
3. 빌드 & 테스트
4. Docker 이미지 생성
5. S3에 배포 패키지 업로드
6. CodeDeploy 배포 시작
7. EC2에서 스크립트 실행:
   - stop.sh: 기존 컨테이너 중지
   - before_install.sh: 디렉토리 준비
   - after_install.sh: Docker 이미지 로드
   - start.sh: 애플리케이션 시작
   - validate.sh: 배포 검증
8. 배포 완료
```

### 수동 배포
```bash
# EC2 서버에서 직접 배포
cd /home/ubuntu/kospot
docker-compose down
docker-compose pull  # 새 이미지가 있다면
docker-compose up -d
```

---

## 🐛 트러블슈팅 빠른 참조

### 502 Bad Gateway
```bash
# 원인: 백엔드 서버 미실행
docker-compose ps
docker-compose logs app
docker-compose restart app
```

### CodeDeploy 배포 실패
```bash
# CodeDeploy Agent 확인
sudo systemctl status codedeploy-agent
sudo tail -f /var/log/aws/codedeploy-agent/codedeploy-agent.log
```

### Database 연결 실패
```bash
# RDS 보안 그룹 확인
# .env 파일 확인
cat /home/ubuntu/kospot/.env
```

### Redis 연결 실패
```bash
# Redis 컨테이너 확인
docker-compose ps redis
docker-compose exec redis redis-cli -a your-password ping
```

---

## 📚 상세 가이드 문서

각 주제별 상세 가이드:

1. **[DEPLOYMENT_GUIDE.md](DEPLOYMENT_GUIDE.md)** - 전체 배포 프로세스
2. **[EC2_SETUP_GUIDE.md](EC2_SETUP_GUIDE.md)** - EC2 서버 설정 상세
3. **[NGINX_SETUP_GUIDE.md](NGINX_SETUP_GUIDE.md)** - Nginx 설정 및 최적화
4. **[ENV_TEMPLATE.md](ENV_TEMPLATE.md)** - 환경변수 관리
5. **[GITHUB_ACTIONS_SETUP.md](GITHUB_ACTIONS_SETUP.md)** - CI/CD 설정

---

## 🔐 보안 권장사항

### 1. AWS
- [ ] IAM 최소 권한 원칙 적용
- [ ] EC2 보안 그룹: 필요한 포트만 개방
- [ ] RDS: Public 접근 비활성화
- [ ] S3 Bucket: 암호화 활성화

### 2. EC2 서버
- [ ] SSH 키 기반 인증만 허용
- [ ] Root 로그인 비활성화
- [ ] Fail2Ban 설치 (무차별 대입 공격 방어)
- [ ] UFW 방화벽 활성화
- [ ] 자동 보안 업데이트 설정

### 3. 애플리케이션
- [ ] 환경변수 파일 권한: 600
- [ ] SSL/TLS 인증서 적용
- [ ] CORS 설정: 허용된 도메인만
- [ ] Rate Limiting 적용

### 4. 모니터링
- [ ] CloudWatch 로그 수집
- [ ] 알람 설정 (CPU, 메모리, 디스크)
- [ ] 정기적인 백업 수행

---

## 💡 유용한 명령어 모음

### Docker
```bash
docker-compose ps                    # 컨테이너 상태
docker-compose logs -f app          # 로그 실시간 확인
docker-compose restart app          # 애플리케이션 재시작
docker-compose down && docker-compose up -d  # 전체 재시작
docker system prune -a              # 사용하지 않는 리소스 정리
```

### Nginx
```bash
sudo nginx -t                       # 설정 테스트
sudo systemctl reload nginx         # 설정 리로드
sudo systemctl restart nginx        # Nginx 재시작
sudo tail -f /var/log/nginx/kospot-access.log  # 액세스 로그
```

### CodeDeploy
```bash
sudo systemctl status codedeploy-agent  # Agent 상태
sudo systemctl restart codedeploy-agent # Agent 재시작
sudo tail -f /var/log/aws/codedeploy-agent/codedeploy-agent.log  # 로그
```

### 시스템 모니터링
```bash
htop                                # 시스템 리소스
docker stats                        # 컨테이너 리소스
df -h                               # 디스크 사용량
free -h                             # 메모리 사용량
```

---

## 📞 지원

배포 관련 문제 발생 시:
1. 해당 가이드 문서의 트러블슈팅 섹션 확인
2. GitHub Actions 로그 확인
3. EC2 인스턴스 로그 확인
4. AWS CodeDeploy 콘솔 확인

---

**작성일:** 2025-01-27  
**버전:** 1.0.0  
**프로젝트:** KoSpot Backend

