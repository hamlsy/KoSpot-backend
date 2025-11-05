# Nginx 설정 가이드

## 📋 목차
1. [Nginx 설치](#nginx-설치)
2. [기본 설정](#기본-설정)
3. [리버스 프록시 설정](#리버스-프록시-설정)
4. [SSL/TLS 인증서 설정](#ssltls-인증서-설정)
5. [성능 최적화](#성능-최적화)
6. [보안 강화](#보안-강화)
7. [로그 관리](#로그-관리)
8. [트러블슈팅](#트러블슈팅)

---

## 🚀 Nginx 설치

### 1. Nginx 설치

```bash
# 패키지 업데이트
sudo apt update

# Nginx 설치
sudo apt install -y nginx

# 버전 확인
nginx -v

# 서비스 시작
sudo systemctl start nginx
sudo systemctl enable nginx

# 상태 확인
sudo systemctl status nginx
```

### 2. 방화벽 설정

```bash
# UFW를 사용하는 경우
sudo ufw allow 'Nginx Full'
sudo ufw status

# 개별 포트 허용
sudo ufw allow 80/tcp
sudo ufw allow 443/tcp
```

### 3. 기본 테스트

```bash
# 로컬에서 접속 테스트
curl http://localhost

# 브라우저에서 EC2 Public IP로 접속
# http://your-ec2-public-ip
# "Welcome to nginx!" 페이지가 표시되어야 함
```

---

## ⚙️ 기본 설정

### 1. Nginx 디렉토리 구조

```
/etc/nginx/
├── nginx.conf                 # 메인 설정 파일
├── sites-available/           # 사용 가능한 사이트 설정
│   └── kospot.conf
├── sites-enabled/             # 활성화된 사이트 설정 (심볼릭 링크)
│   └── kospot.conf -> ../sites-available/kospot.conf
├── conf.d/                    # 추가 설정 파일
├── snippets/                  # 재사용 가능한 설정 조각
└── modules-enabled/           # 활성화된 모듈

/var/log/nginx/
├── access.log                 # 접근 로그
├── error.log                  # 에러 로그
└── kospot-access.log         # 프로젝트별 로그
```

### 2. 메인 설정 파일 백업

```bash
# 기존 설정 백업
sudo cp /etc/nginx/nginx.conf /etc/nginx/nginx.conf.backup
sudo cp /etc/nginx/sites-available/default /etc/nginx/sites-available/default.backup
```

### 3. 프로젝트 설정 파일 생성

```bash
# 프로젝트 설정 파일 생성
sudo nano /etc/nginx/sites-available/kospot.conf
```

**kospot.conf 내용:**
```nginx
upstream kospot_backend {
    least_conn;
    server localhost:8080 max_fails=3 fail_timeout=30s;
    keepalive 32;
}

server {
    listen 80;
    server_name _;  # 실제 도메인으로 변경

    client_max_body_size 20M;
    
    access_log /var/log/nginx/kospot-access.log;
    error_log /var/log/nginx/kospot-error.log;

    # 기본 경로
    location / {
        return 200 '{"status":"ok","service":"KoSpot Backend"}';
        add_header Content-Type application/json;
    }

    # API 프록시
    location /api/ {
        proxy_pass http://kospot_backend;
        proxy_http_version 1.1;
        
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        
        proxy_connect_timeout 60s;
        proxy_send_timeout 60s;
        proxy_read_timeout 60s;
        
        proxy_set_header Connection "";
    }

    # WebSocket 프록시
    location /ws/ {
        proxy_pass http://kospot_backend/api/ws/;
        proxy_http_version 1.1;
        
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection "upgrade";
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        
        proxy_connect_timeout 7d;
        proxy_send_timeout 7d;
        proxy_read_timeout 7d;
        
        proxy_buffering off;
    }

    # Health Check
    location /actuator/health {
        proxy_pass http://kospot_backend;
        access_log off;
    }
}
```

### 4. 설정 활성화

```bash
# 심볼릭 링크 생성
sudo ln -s /etc/nginx/sites-available/kospot.conf /etc/nginx/sites-enabled/kospot.conf

# 기본 설정 비활성화 (선택사항)
sudo rm /etc/nginx/sites-enabled/default

# 설정 파일 문법 검사
sudo nginx -t

# Nginx 재시작
sudo systemctl restart nginx
```

---

## 🔄 리버스 프록시 설정

### 1. Upstream 설정 (로드 밸런싱)

```nginx
# 여러 백엔드 서버가 있는 경우
upstream kospot_backend {
    # 로드 밸런싱 방법 선택
    # least_conn;        # 연결 수가 가장 적은 서버
    # ip_hash;           # 클라이언트 IP 기반
    # random;            # 랜덤
    
    least_conn;
    
    # 백엔드 서버 목록
    server localhost:8080 weight=1 max_fails=3 fail_timeout=30s;
    # server localhost:8081 weight=1 max_fails=3 fail_timeout=30s;
    
    # Keep-Alive 연결 유지
    keepalive 32;
    keepalive_timeout 60s;
    keepalive_requests 100;
}
```

### 2. 프록시 헤더 설정

```nginx
location /api/ {
    proxy_pass http://kospot_backend;
    
    # HTTP 버전
    proxy_http_version 1.1;
    
    # 필수 헤더
    proxy_set_header Host $host;
    proxy_set_header X-Real-IP $remote_addr;
    proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    proxy_set_header X-Forwarded-Proto $scheme;
    proxy_set_header X-Forwarded-Host $server_name;
    
    # Keep-Alive
    proxy_set_header Connection "";
    
    # 타임아웃 설정
    proxy_connect_timeout 60s;
    proxy_send_timeout 60s;
    proxy_read_timeout 60s;
    
    # 버퍼 설정
    proxy_buffering on;
    proxy_buffer_size 4k;
    proxy_buffers 8 4k;
    proxy_busy_buffers_size 8k;
}
```

### 3. WebSocket 프록시 설정

```nginx
location /ws/ {
    proxy_pass http://kospot_backend;
    proxy_http_version 1.1;
    
    # WebSocket 필수 헤더
    proxy_set_header Upgrade $http_upgrade;
    proxy_set_header Connection "upgrade";
    
    # 기본 헤더
    proxy_set_header Host $host;
    proxy_set_header X-Real-IP $remote_addr;
    proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    proxy_set_header X-Forwarded-Proto $scheme;
    
    # WebSocket 타임아웃 (긴 시간 설정)
    proxy_connect_timeout 7d;
    proxy_send_timeout 7d;
    proxy_read_timeout 7d;
    
    # 버퍼링 비활성화
    proxy_buffering off;
}
```

---

## 🔐 SSL/TLS 인증서 설정

### 1. Let's Encrypt 인증서 발급

```bash
# Certbot 설치
sudo apt install -y certbot python3-certbot-nginx

# 인증서 발급 (도메인 소유 확인 필요)
sudo certbot --nginx -d your-domain.com -d www.your-domain.com

# 대화형 프롬프트에서:
# - 이메일 입력
# - 약관 동의
# - HTTP를 HTTPS로 리다이렉트 선택 (권장)
```

### 2. 인증서 자동 갱신

```bash
# 자동 갱신 테스트
sudo certbot renew --dry-run

# Cron 작업 확인 (자동으로 설정됨)
sudo systemctl status certbot.timer

# 수동으로 Cron 작업 추가 (필요 시)
sudo crontab -e

# 매일 오전 3시 갱신 시도
0 3 * * * certbot renew --quiet --post-hook "systemctl reload nginx"
```

### 3. SSL 설정 강화

```nginx
server {
    listen 80;
    server_name your-domain.com www.your-domain.com;
    
    # HTTP를 HTTPS로 리다이렉트
    return 301 https://$server_name$request_uri;
}

server {
    listen 443 ssl http2;
    server_name your-domain.com www.your-domain.com;
    
    # SSL 인증서
    ssl_certificate /etc/letsencrypt/live/your-domain.com/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/your-domain.com/privkey.pem;
    
    # SSL 프로토콜 및 암호화 스위트
    ssl_protocols TLSv1.2 TLSv1.3;
    ssl_prefer_server_ciphers on;
    ssl_ciphers 'ECDHE-ECDSA-AES128-GCM-SHA256:ECDHE-RSA-AES128-GCM-SHA256:ECDHE-ECDSA-AES256-GCM-SHA384:ECDHE-RSA-AES256-GCM-SHA384:ECDHE-ECDSA-CHACHA20-POLY1305:ECDHE-RSA-CHACHA20-POLY1305:DHE-RSA-AES128-GCM-SHA256:DHE-RSA-AES256-GCM-SHA384';
    
    # SSL 세션 캐시
    ssl_session_cache shared:SSL:10m;
    ssl_session_timeout 10m;
    ssl_session_tickets off;
    
    # OCSP Stapling
    ssl_stapling on;
    ssl_stapling_verify on;
    ssl_trusted_certificate /etc/letsencrypt/live/your-domain.com/chain.pem;
    
    # DNS Resolver
    resolver 8.8.8.8 8.8.4.4 valid=300s;
    resolver_timeout 5s;
    
    # 보안 헤더
    add_header Strict-Transport-Security "max-age=31536000; includeSubDomains" always;
    
    # 나머지 location 블록...
}
```

---

## 🚀 성능 최적화

### 1. Worker 프로세스 설정

```nginx
# /etc/nginx/nginx.conf
user nginx;
worker_processes auto;  # CPU 코어 수에 맞게 자동 설정
worker_rlimit_nofile 65535;

events {
    worker_connections 2048;  # 동시 연결 수
    use epoll;                # Linux에서 효율적인 이벤트 모델
    multi_accept on;
}
```

### 2. 버퍼 및 타임아웃 설정

```nginx
http {
    # 버퍼 크기
    client_body_buffer_size 128k;
    client_max_body_size 20M;
    client_header_buffer_size 1k;
    large_client_header_buffers 4 4k;
    output_buffers 1 32k;
    postpone_output 1460;
    
    # 타임아웃
    client_body_timeout 12;
    client_header_timeout 12;
    keepalive_timeout 65;
    send_timeout 10;
    
    # TCP 최적화
    sendfile on;
    tcp_nopush on;
    tcp_nodelay on;
}
```

### 3. Gzip 압축

```nginx
http {
    # Gzip 압축 활성화
    gzip on;
    gzip_vary on;
    gzip_proxied any;
    gzip_comp_level 6;
    gzip_types
        text/plain
        text/css
        text/xml
        text/javascript
        application/json
        application/javascript
        application/xml+rss
        application/rss+xml
        font/truetype
        font/opentype
        application/vnd.ms-fontobject
        image/svg+xml;
    gzip_disable "msie6";
    gzip_min_length 256;
}
```

### 4. 캐싱 설정

```nginx
http {
    # 프록시 캐시 경로 설정
    proxy_cache_path /var/cache/nginx levels=1:2 keys_zone=api_cache:10m max_size=1g inactive=60m use_temp_path=off;
    
    server {
        # 정적 리소스 캐싱 (필요 시)
        location ~* \.(jpg|jpeg|png|gif|ico|css|js|woff|woff2|ttf|svg)$ {
            expires 30d;
            add_header Cache-Control "public, immutable";
        }
        
        # API 응답 캐싱 (신중하게 사용)
        location /api/public/ {
            proxy_pass http://kospot_backend;
            proxy_cache api_cache;
            proxy_cache_valid 200 5m;
            proxy_cache_key "$scheme$request_method$host$request_uri";
            add_header X-Cache-Status $upstream_cache_status;
        }
    }
}
```

---

## 🛡️ 보안 강화

### 1. Rate Limiting (요청 제한)

```nginx
http {
    # Rate Limiting Zone 정의
    limit_req_zone $binary_remote_addr zone=api_limit:10m rate=100r/s;
    limit_req_zone $binary_remote_addr zone=ws_limit:10m rate=50r/s;
    limit_conn_zone $binary_remote_addr zone=addr:10m;
    
    server {
        # API 엔드포인트에 Rate Limiting 적용
        location /api/ {
            limit_req zone=api_limit burst=50 nodelay;
            limit_conn addr 10;
            
            proxy_pass http://kospot_backend;
        }
        
        # WebSocket에 Rate Limiting 적용
        location /ws/ {
            limit_req zone=ws_limit burst=20 nodelay;
            
            proxy_pass http://kospot_backend;
        }
    }
}
```

### 2. 보안 헤더

```nginx
server {
    # XSS 보호
    add_header X-XSS-Protection "1; mode=block" always;
    
    # MIME 타입 스니핑 방지
    add_header X-Content-Type-Options "nosniff" always;
    
    # Clickjacking 방지
    add_header X-Frame-Options "SAMEORIGIN" always;
    
    # Referrer 정책
    add_header Referrer-Policy "no-referrer-when-downgrade" always;
    
    # Content Security Policy (필요에 따라 조정)
    add_header Content-Security-Policy "default-src 'self'; script-src 'self' 'unsafe-inline'; style-src 'self' 'unsafe-inline';" always;
    
    # HSTS (HTTPS 강제)
    add_header Strict-Transport-Security "max-age=31536000; includeSubDomains" always;
}
```

### 3. 숨겨진 파일 및 디렉토리 보호

```nginx
server {
    # . 으로 시작하는 파일 접근 차단
    location ~ /\. {
        deny all;
        access_log off;
        log_not_found off;
    }
    
    # 백업 파일 접근 차단
    location ~ ~$ {
        deny all;
        access_log off;
        log_not_found off;
    }
}
```

### 4. 서버 정보 숨기기

```nginx
http {
    # 서버 버전 숨기기
    server_tokens off;
    
    # 추가로 헤더 수정 (nginx-extras 패키지 필요)
    # more_clear_headers Server;
    # more_set_headers 'Server: WebServer';
}
```

---

## 📊 로그 관리

### 1. 로그 포맷 커스터마이징

```nginx
http {
    # 상세 로그 포맷
    log_format main '$remote_addr - $remote_user [$time_local] "$request" '
                    '$status $body_bytes_sent "$http_referer" '
                    '"$http_user_agent" "$http_x_forwarded_for" '
                    'rt=$request_time uct="$upstream_connect_time" '
                    'uht="$upstream_header_time" urt="$upstream_response_time"';
    
    # JSON 로그 포맷 (분석 도구용)
    log_format json_combined escape=json
    '{'
        '"time_local":"$time_local",'
        '"remote_addr":"$remote_addr",'
        '"request":"$request",'
        '"status": "$status",'
        '"body_bytes_sent":"$body_bytes_sent",'
        '"request_time":"$request_time",'
        '"http_referrer":"$http_referer",'
        '"http_user_agent":"$http_user_agent"'
    '}';
    
    access_log /var/log/nginx/access.log main;
}
```

### 2. 로그 로테이션

```bash
# Logrotate 설정
sudo nano /etc/logrotate.d/nginx

# 다음 내용으로 수정:
/var/log/nginx/*.log {
    daily                    # 매일 로테이션
    missingok               # 로그 파일이 없어도 에러 없음
    rotate 14               # 14일치 보관
    compress                # 압축
    delaycompress           # 다음 로테이션 시 압축
    notifempty              # 빈 파일은 로테이션 안 함
    create 0640 www-data adm
    sharedscripts
    postrotate
        [ -f /var/run/nginx.pid ] && kill -USR1 `cat /var/run/nginx.pid`
    endscript
}
```

### 3. 로그 분석

```bash
# 실시간 로그 모니터링
sudo tail -f /var/log/nginx/kospot-access.log

# 상태 코드별 집계
sudo awk '{print $9}' /var/log/nginx/kospot-access.log | sort | uniq -c | sort -rn

# 가장 많이 접근한 IP
sudo awk '{print $1}' /var/log/nginx/kospot-access.log | sort | uniq -c | sort -rn | head -10

# 응답 시간이 긴 요청
sudo grep -oP '(?<=rt=)[0-9.]+' /var/log/nginx/kospot-access.log | sort -rn | head -10
```

---

## 🔧 트러블슈팅

### 1. 502 Bad Gateway

**원인:**
- 백엔드 서버가 실행되지 않음
- 백엔드 서버가 8080 포트를 사용하지 않음
- 방화벽 문제

**해결 방법:**
```bash
# 백엔드 서버 확인
docker-compose ps

# 포트 사용 확인
sudo netstat -tulpn | grep 8080

# Nginx 에러 로그 확인
sudo tail -f /var/log/nginx/error.log

# 백엔드 서버 재시작
docker-compose restart app
```

### 2. 413 Request Entity Too Large

**원인:**
- 업로드 파일 크기 제한 초과

**해결 방법:**
```nginx
server {
    client_max_body_size 20M;  # 필요한 크기로 조정
}
```

### 3. WebSocket 연결 실패

**원인:**
- Upgrade 헤더 누락
- 타임아웃 설정이 짧음

**해결 방법:**
```nginx
location /ws/ {
    proxy_pass http://kospot_backend;
    proxy_http_version 1.1;
    
    # 필수 헤더
    proxy_set_header Upgrade $http_upgrade;
    proxy_set_header Connection "upgrade";
    
    # 긴 타임아웃
    proxy_connect_timeout 7d;
    proxy_send_timeout 7d;
    proxy_read_timeout 7d;
}
```

### 4. Nginx 설정 테스트 실패

```bash
# 문법 검사
sudo nginx -t

# 상세 로그 확인
sudo nginx -t -c /etc/nginx/nginx.conf

# 특정 설정 파일 테스트
sudo nginx -t -c /etc/nginx/sites-available/kospot.conf
```

---

## 📝 유용한 명령어

```bash
# Nginx 재시작
sudo systemctl restart nginx

# Nginx 리로드 (연결 유지)
sudo systemctl reload nginx

# Nginx 중지
sudo systemctl stop nginx

# Nginx 시작
sudo systemctl start nginx

# 설정 테스트
sudo nginx -t

# Nginx 프로세스 확인
ps aux | grep nginx

# 실시간 상태 확인 (nginx-module-vts 설치 필요)
# curl http://localhost/status
```

---

## 📚 참고 자료

- [Nginx Official Documentation](https://nginx.org/en/docs/)
- [Nginx Configuration Generator](https://www.digitalocean.com/community/tools/nginx)
- [Mozilla SSL Configuration Generator](https://ssl-config.mozilla.org/)
- [Let's Encrypt Documentation](https://letsencrypt.org/docs/)

---

**작성일:** 2025-01-27  
**버전:** 1.0.0

