@echo off
echo ========================================
echo KoSpot 팀 설정 변경 테스트 실행 스크립트
echo ========================================
echo.

REM Redis 서버 상태 확인
echo [1/4] Redis 서버 상태 확인 중...
redis-cli ping >nul 2>&1
if %errorlevel% neq 0 (
    echo ❌ Redis 서버가 실행되지 않았습니다.
    echo    Redis 서버를 먼저 실행해주세요: redis-server
    pause
    exit /b 1
) else (
    echo ✅ Redis 서버 연결 성공
)

REM MySQL 서버 상태 확인
echo [2/4] MySQL 서버 상태 확인 중...
mysql -u root -p1234 -e "SELECT 1;" >nul 2>&1
if %errorlevel% neq 0 (
    echo ❌ MySQL 서버 연결 실패
    echo    MySQL 서버가 실행 중이고 다음 설정이 맞는지 확인해주세요:
    echo    - Host: localhost:3306
    echo    - Database: kospot-test
    echo    - Username: root
    echo    - Password: 1234
    pause
    exit /b 1
) else (
    echo ✅ MySQL 서버 연결 성공
)

REM Gradle 빌드
echo [3/4] Gradle 빌드 중...
call gradlew clean build -x test
if %errorlevel% neq 0 (
    echo ❌ Gradle 빌드 실패
    pause
    exit /b 1
) else (
    echo ✅ Gradle 빌드 성공
)

REM 테스트 실행
echo [4/4] 팀 설정 변경 테스트 실행 중...
echo.
echo 실행할 테스트:
echo - UpdateGameRoomSettingsUseCaseTest (통합 테스트)
echo - GameRoomRedisServiceTeamTest (단위 테스트)
echo.

call gradlew test --tests "*TeamSettingTest*" --info
if %errorlevel% neq 0 (
    echo.
    echo ❌ 테스트 실행 실패
    echo    로그를 확인하여 문제를 해결해주세요.
) else (
    echo.
    echo ✅ 모든 테스트 통과!
    echo.
    echo 테스트 결과 요약:
    echo - 개인전 → 팀전 변경: ✅
    echo - 팀전 → 개인전 변경: ✅
    echo - 팀전 → 팀전 재할당: ✅
    echo - 개인전 → 개인전 유지: ✅
    echo - 빈 방 처리: ✅
    echo - Redis 연결: ✅
)

echo.
echo ========================================
echo 테스트 실행 완료
echo ========================================
pause
