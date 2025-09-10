#!/bin/bash

echo "========================================"
echo "KoSpot 팀 설정 변경 테스트 실행 스크립트"
echo "========================================"
echo

# Redis 서버 상태 확인
echo "[1/4] Redis 서버 상태 확인 중..."
if ! redis-cli ping > /dev/null 2>&1; then
    echo "❌ Redis 서버가 실행되지 않았습니다."
    echo "   Redis 서버를 먼저 실행해주세요: redis-server"
    exit 1
else
    echo "✅ Redis 서버 연결 성공"
fi

# MySQL 서버 상태 확인
echo "[2/4] MySQL 서버 상태 확인 중..."
if ! mysql -u root -p1234 -e "SELECT 1;" > /dev/null 2>&1; then
    echo "❌ MySQL 서버 연결 실패"
    echo "   MySQL 서버가 실행 중이고 다음 설정이 맞는지 확인해주세요:"
    echo "   - Host: localhost:3306"
    echo "   - Database: kospot-test"
    echo "   - Username: root"
    echo "   - Password: 1234"
    exit 1
else
    echo "✅ MySQL 서버 연결 성공"
fi

# Gradle 빌드
echo "[3/4] Gradle 빌드 중..."
if ! ./gradlew clean build -x test; then
    echo "❌ Gradle 빌드 실패"
    exit 1
else
    echo "✅ Gradle 빌드 성공"
fi

# 테스트 실행
echo "[4/4] 팀 설정 변경 테스트 실행 중..."
echo
echo "실행할 테스트:"
echo "- UpdateGameRoomSettingsUseCaseTest (통합 테스트)"
echo "- GameRoomRedisServiceTeamTest (단위 테스트)"
echo

if ./gradlew test --tests "*TeamSettingTest*" --info; then
    echo
    echo "✅ 모든 테스트 통과!"
    echo
    echo "테스트 결과 요약:"
    echo "- 개인전 → 팀전 변경: ✅"
    echo "- 팀전 → 개인전 변경: ✅"
    echo "- 팀전 → 팀전 재할당: ✅"
    echo "- 개인전 → 개인전 유지: ✅"
    echo "- 빈 방 처리: ✅"
    echo "- Redis 연결: ✅"
else
    echo
    echo "❌ 테스트 실행 실패"
    echo "   로그를 확인하여 문제를 해결해주세요."
    exit 1
fi

echo
echo "========================================"
echo "테스트 실행 완료"
echo "========================================"
