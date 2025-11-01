# EC2 서버 설정 가이드

## 📋 목차
1. [EC2 인스턴스 생성](#ec2-인스턴스-생성)
2. [초기 서버 설정](#초기-서버-설정)
3. [Docker 설치](#docker-설치)
4. [CodeDeploy Agent 설치](#codedeploy-agent-설치)
5. [모니터링 도구 설치](#모니터링-도구-설치)
6. [보안 설정](#보안-설정)

---

## 🖥️ EC2 인스턴스 생성

### 1. AWS Console에서 EC2 인스턴스 생성

#### 인스턴스 사양 (권장)

| 환경 | 인스턴스 타입 | vCPU | 메모리 | 스토리지 |
|------|--------------|------|--------|----------|
| 개발/테스트 | t3.small | 2 | 2GB | 20GB |
| 운영 (소규모) | t3.medium | 2 | 4GB | 30GB |
| 운영 (중규모) | t3.large | 2 | 8GB | 50GB |

#### AMI 선택
- **OS**: Ubuntu Server 22.04 LTS (HVM), SSD Volume Type
- **Architecture**: 64-bit (x86)

#### 네트워크 설정
- **VPC**: 기본 VPC 또는 커스텀 VPC
- **Subnet**: Public Subnet (외부 접근 필요)
- **Auto-assign Public IP**: Enable

#### 보안 그룹 설정

```
인바운드 규칙:
┌─────────────┬──────────┬─────────────┬─────────────────────────┐
│ Type        │ Protocol │ Port Range  │ Source                  │
├─────────────┼──────────┼─────────────┼─────────────────────────┤
│ SSH         │ TCP      │ 22          │ My IP (보안을 위해)     │
│ HTTP        │ TCP      │ 80          │ 0.0.0.0/0              │
│ HTTPS       │ TCP      │ 443         │ 0.0.0.0/0              │
│ Custom TCP  │ TCP      │ 8080        │ Security Group (Nginx)  │
└─────────────┴──────────┴─────────────┴─────────────────────────┘

아웃바운드 규칙:
All traffic to 0.0.0.0/0 (기본값)
```

#### 스토리지 설정
- **Root Volume**: gp3 (General Purpose SSD)
- **Size**: 최소 20GB (로그 및 Docker 이미지 고려)
- **Delete on Termination**: Yes (개발), No (운영)

#### IAM 역할 연결
- **Role Name**: `KoSpotEC2CodeDeployRole`
- **Policies**:
  - `AmazonS3ReadOnlyAccess` (CodeDeploy용)
  - `CloudWatchAgentServerPolicy` (모니터링용)

#### 태그 설정
```
Key: Name
Value: KoSpot-Backend

Key: Environment
Value: Production

Key: Project
Value: KoSpot
```

---

## 🔧 초기 서버 설정

### 1. SSH 접속

```bash
# SSH 키 권한 설정 (로컬 PC에서)
chmod 400 your-key.pem

# EC2 연결
ssh -i your-key.pem ubuntu@your-ec2-public-ip

# 또는 SSH 설정 파일 사용
nano ~/.ssh/config

# 다음 내용 추가:
Host kospot-server
    HostName your-ec2-public-ip
    User ubuntu
    IdentityFile ~/path/to/your-key.pem

# 간편하게 연결
ssh kospot-server
```

### 2. 시스템 업데이트

```bash
# 패키지 목록 업데이트
sudo apt update

# 전체 패키지 업그레이드
sudo apt upgrade -y

# 재부팅이 필요한 경우
sudo reboot

# 다시 접속 후 확인
ssh kospot-server
uname -a
```

### 3. 기본 패키지 설치

```bash
# 필수 도구 설치
sudo apt install -y \
  curl \
  wget \
  git \
  vim \
  unzip \
  zip \
  htop \
  net-tools \
  build-essential \
  software-properties-common

# 설치 확인
curl --version
git --version
vim --version
```

### 4. 타임존 설정

```bash
# 현재 타임존 확인
timedatectl

# 타임존을 한국으로 변경
sudo timedatectl set-timezone Asia/Seoul

# 확인
date
```

### 5. 호스트네임 설정

```bash
# 호스트네임 변경
sudo hostnamectl set-hostname kospot-backend

# 확인
hostname

# /etc/hosts 파일 업데이트
sudo nano /etc/hosts

# 다음 줄 추가:
127.0.0.1 kospot-backend
```

### 6. Swap 메모리 설정 (메모리 부족 시)

```bash
# 2GB Swap 파일 생성
sudo fallocate -l 2G /swapfile

# 권한 설정
sudo chmod 600 /swapfile

# Swap 영역으로 설정
sudo mkswap /swapfile

# Swap 활성화
sudo swapon /swapfile

# 영구 설정
echo '/swapfile none swap sw 0 0' | sudo tee -a /etc/fstab

# 확인
free -h
swapon --show
```

---

## 🐳 Docker 설치

### 1. Docker 설치

```bash
# 기존 Docker 패키지 제거 (있다면)
sudo apt remove docker docker-engine docker.io containerd runc

# Docker 공식 GPG 키 추가
sudo apt update
sudo apt install -y ca-certificates curl gnupg lsb-release

sudo mkdir -p /etc/apt/keyrings
curl -fsSL https://download.docker.com/linux/ubuntu/gpg | sudo gpg --dearmor -o /etc/apt/keyrings/docker.gpg

# Docker 리포지토리 추가
echo \
  "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.gpg] https://download.docker.com/linux/ubuntu \
  $(lsb_release -cs) stable" | sudo tee /etc/apt/sources.list.d/docker.list > /dev/null

# Docker 설치
sudo apt update
sudo apt install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin

# Docker 서비스 시작
sudo systemctl start docker
sudo systemctl enable docker

# 버전 확인
docker --version
```

### 2. Docker 권한 설정

```bash
# 현재 사용자를 docker 그룹에 추가
sudo usermod -aG docker ubuntu

# 그룹 변경 적용 (재로그인 필요)
newgrp docker

# 또는 다시 SSH 접속

# 권한 확인
docker ps
```

### 3. Docker Compose 설치

```bash
# Docker Compose 최신 버전 설치
sudo curl -L "https://github.com/docker/compose/releases/latest/download/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose

# 실행 권한 부여
sudo chmod +x /usr/local/bin/docker-compose

# 심볼릭 링크 생성
sudo ln -s /usr/local/bin/docker-compose /usr/bin/docker-compose

# 버전 확인
docker-compose --version
```

### 4. Docker 설정 최적화

```bash
# Docker 데몬 설정
sudo nano /etc/docker/daemon.json

# 다음 내용 추가:
{
  "log-driver": "json-file",
  "log-opts": {
    "max-size": "10m",
    "max-file": "3"
  },
  "storage-driver": "overlay2",
  "dns": ["8.8.8.8", "8.8.4.4"]
}

# Docker 재시작
sudo systemctl restart docker

# 상태 확인
sudo systemctl status docker
```

---

## 📦 CodeDeploy Agent 설치

### 1. Ruby 설치 (CodeDeploy 의존성)

```bash
# Ruby 설치
sudo apt install -y ruby-full

# 버전 확인
ruby --version
```

### 2. CodeDeploy Agent 설치

```bash
# 작업 디렉토리로 이동
cd /home/ubuntu

# CodeDeploy Agent 설치 스크립트 다운로드
wget https://aws-codedeploy-ap-northeast-2.s3.ap-northeast-2.amazonaws.com/latest/install

# 실행 권한 부여
chmod +x ./install

# CodeDeploy Agent 설치
sudo ./install auto

# 설치 확인
sudo systemctl status codedeploy-agent
```

### 3. CodeDeploy Agent 자동 시작 설정

```bash
# 자동 시작 활성화
sudo systemctl enable codedeploy-agent

# 시작
sudo systemctl start codedeploy-agent

# 상태 확인
sudo systemctl status codedeploy-agent

# 로그 확인
sudo tail -f /var/log/aws/codedeploy-agent/codedeploy-agent.log
```

---

## 📊 모니터링 도구 설치

### 1. CloudWatch Agent 설치 (선택사항)

```bash
# CloudWatch Agent 다운로드
wget https://s3.amazonaws.com/amazoncloudwatch-agent/ubuntu/amd64/latest/amazon-cloudwatch-agent.deb

# 설치
sudo dpkg -i -E ./amazon-cloudwatch-agent.deb

# 설정 (Wizard 사용)
sudo /opt/aws/amazon-cloudwatch-agent/bin/amazon-cloudwatch-agent-config-wizard

# 또는 설정 파일 직접 생성
sudo nano /opt/aws/amazon-cloudwatch-agent/etc/config.json
```

### 2. htop 및 모니터링 도구

```bash
# htop 설치 (이미 설치되어 있을 수 있음)
sudo apt install -y htop

# 추가 모니터링 도구
sudo apt install -y \
  iotop \
  iftop \
  sysstat \
  nethogs

# 사용법:
# htop      - 시스템 리소스 모니터링
# iotop     - 디스크 I/O 모니터링
# iftop     - 네트워크 대역폭 모니터링
# nethogs   - 프로세스별 네트워크 사용량
```

---

## 🔒 보안 설정

### 1. UFW 방화벽 설정

```bash
# UFW 설치 (이미 설치되어 있을 수 있음)
sudo apt install -y ufw

# 기본 정책 설정
sudo ufw default deny incoming
sudo ufw default allow outgoing

# SSH 허용 (중요!)
sudo ufw allow ssh
sudo ufw allow 22/tcp

# HTTP/HTTPS 허용
sudo ufw allow 80/tcp
sudo ufw allow 443/tcp

# 방화벽 활성화
sudo ufw enable

# 상태 확인
sudo ufw status verbose
```

### 2. Fail2Ban 설치 (SSH 무차별 대입 공격 방어)

```bash
# Fail2Ban 설치
sudo apt install -y fail2ban

# 설정 파일 복사
sudo cp /etc/fail2ban/jail.conf /etc/fail2ban/jail.local

# 설정 편집
sudo nano /etc/fail2ban/jail.local

# [sshd] 섹션 수정:
[sshd]
enabled = true
port = ssh
logpath = %(sshd_log)s
maxretry = 5
bantime = 3600

# Fail2Ban 시작
sudo systemctl start fail2ban
sudo systemctl enable fail2ban

# 상태 확인
sudo fail2ban-client status
sudo fail2ban-client status sshd
```

### 3. SSH 보안 강화

```bash
# SSH 설정 백업
sudo cp /etc/ssh/sshd_config /etc/ssh/sshd_config.backup

# SSH 설정 편집
sudo nano /etc/ssh/sshd_config

# 다음 설정 변경/추가:
PermitRootLogin no                    # Root 로그인 비활성화
PasswordAuthentication no             # 비밀번호 인증 비활성화 (키 인증만 허용)
PubkeyAuthentication yes              # 공개키 인증 활성화
X11Forwarding no                      # X11 포워딩 비활성화
MaxAuthTries 3                        # 최대 인증 시도 횟수
ClientAliveInterval 300               # 클라이언트 활성 체크 (5분)
ClientAliveCountMax 2                 # 최대 무응답 횟수

# SSH 서비스 재시작
sudo systemctl restart sshd

# 현재 연결을 유지한 채 새 터미널로 접속 테스트!
```

### 4. 자동 보안 업데이트 설정

```bash
# Unattended-upgrades 설치
sudo apt install -y unattended-upgrades

# 설정
sudo dpkg-reconfigure -plow unattended-upgrades

# 또는 수동 설정
sudo nano /etc/apt/apt.conf.d/50unattended-upgrades

# 다음 내용 확인/추가:
Unattended-Upgrade::Allowed-Origins {
    "${distro_id}:${distro_codename}-security";
    "${distro_id}ESMApps:${distro_codename}-apps-security";
};
Unattended-Upgrade::AutoFixInterruptedDpkg "true";
Unattended-Upgrade::Remove-Unused-Dependencies "true";
Unattended-Upgrade::Automatic-Reboot "false";
```

---

## 📂 프로젝트 디렉토리 구조

```bash
# 프로젝트 디렉토리 생성
mkdir -p /home/ubuntu/kospot
mkdir -p /home/ubuntu/kospot/logs
mkdir -p /home/ubuntu/kospot/backup
mkdir -p /home/ubuntu/kospot/scripts

# 권한 설정
sudo chown -R ubuntu:ubuntu /home/ubuntu/kospot
chmod 755 /home/ubuntu/kospot

# 디렉토리 구조 확인
tree /home/ubuntu/kospot
```

**예상 디렉토리 구조:**
```
/home/ubuntu/kospot/
├── .env                    # 환경변수 파일
├── docker-compose.yml      # Docker Compose 설정
├── kospot-backend.tar      # Docker 이미지 (배포 시 생성)
├── appspec.yml            # CodeDeploy 설정
├── scripts/               # 배포 스크립트
│   ├── stop.sh
│   ├── before_install.sh
│   ├── after_install.sh
│   ├── start.sh
│   └── validate.sh
├── logs/                  # 애플리케이션 로그
└── backup/                # 백업 파일
```

---

## ✅ 설정 검증

### 1. 시스템 정보 확인

```bash
# OS 버전
lsb_release -a

# 커널 버전
uname -a

# CPU 정보
lscpu

# 메모리 정보
free -h

# 디스크 정보
df -h

# 네트워크 인터페이스
ip addr show
```

### 2. 설치된 소프트웨어 확인

```bash
# Docker
docker --version
docker-compose --version

# CodeDeploy Agent
sudo systemctl status codedeploy-agent

# 방화벽
sudo ufw status

# Fail2Ban
sudo fail2ban-client status
```

### 3. 네트워크 연결 테스트

```bash
# 인터넷 연결 확인
ping -c 4 google.com

# RDS 연결 확인 (RDS 엔드포인트로 변경)
nc -zv your-rds-endpoint.ap-northeast-2.rds.amazonaws.com 3306

# S3 연결 확인
aws s3 ls s3://kospot-deploy-bucket
```

---

## 🔄 유지보수 및 관리

### 1. 정기 업데이트

```bash
# 주간 업데이트 (Cron 작업 추가)
sudo crontab -e

# 다음 줄 추가 (매주 일요일 오전 3시):
0 3 * * 0 apt update && apt upgrade -y
```

### 2. 로그 모니터링

```bash
# 시스템 로그
sudo tail -f /var/log/syslog

# Docker 로그
docker-compose logs -f

# CodeDeploy 로그
sudo tail -f /var/log/aws/codedeploy-agent/codedeploy-agent.log

# Nginx 로그
sudo tail -f /var/log/nginx/access.log
sudo tail -f /var/log/nginx/error.log
```

### 3. 백업 스크립트 (선택사항)

```bash
# 백업 스크립트 생성
nano /home/ubuntu/kospot/backup.sh

#!/bin/bash
# 백업 스크립트
BACKUP_DIR="/home/ubuntu/kospot/backup"
DATE=$(date +%Y%m%d_%H%M%S)

# Docker 볼륨 백업
docker-compose down
tar -czf $BACKUP_DIR/volumes_$DATE.tar.gz /var/lib/docker/volumes
docker-compose up -d

# .env 파일 백업
cp /home/ubuntu/kospot/.env $BACKUP_DIR/.env_$DATE

# 오래된 백업 삭제 (30일 이상)
find $BACKUP_DIR -name "*.tar.gz" -mtime +30 -delete

# 실행 권한 부여
chmod +x /home/ubuntu/kospot/backup.sh

# Cron 작업 추가 (매일 오전 2시)
0 2 * * * /home/ubuntu/kospot/backup.sh
```

---

## 📞 추가 리소스

- [AWS EC2 User Guide](https://docs.aws.amazon.com/ec2/index.html)
- [Docker Documentation](https://docs.docker.com/)
- [Ubuntu Server Guide](https://ubuntu.com/server/docs)
- [AWS CodeDeploy Guide](https://docs.aws.amazon.com/codedeploy/)

---

**작성일:** 2025-01-27  
**버전:** 1.0.0

