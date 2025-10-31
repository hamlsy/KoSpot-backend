#!/bin/bash

echo "=========================================="
echo "Running before install script..."
echo "=========================================="

# 프로젝트 디렉토리 생성
if [ ! -d /home/ubuntu/kospot ]; then
    mkdir -p /home/ubuntu/kospot
    echo "✅ Created project directory"
fi

# 로그 디렉토리 생성
if [ ! -d /home/ubuntu/kospot/logs ]; then
    mkdir -p /home/ubuntu/kospot/logs
    echo "✅ Created logs directory"
fi

# 이전 배포 파일 백업 (선택사항)
if [ -d /home/ubuntu/kospot/backup ]; then
    rm -rf /home/ubuntu/kospot/backup
fi

if [ -f /home/ubuntu/kospot/docker-compose.yml ]; then
    mkdir -p /home/ubuntu/kospot/backup
    cp /home/ubuntu/kospot/docker-compose.yml /home/ubuntu/kospot/backup/ || true
    echo "✅ Backed up previous deployment files"
fi

echo "=========================================="
echo "Before install completed"
echo "=========================================="

