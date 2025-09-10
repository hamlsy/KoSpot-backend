# 팀 설정 변경 테스트 가이드

이 문서는 `UpdateGameRoomSettingsUseCase`의 팀 설정 변경 로직을 테스트하는 방법을 설명합니다.

## 테스트 개요

### 테스트 파일
1. **UpdateGameRoomSettingsUseCaseTest.java** - 통합 테스트 (UseCase 레벨)
2. **GameRoomRedisServiceTeamTest.java** - 단위 테스트 (Service 레벨)

### 테스트 시나리오
- 개인전 → 팀전 변경 시 모든 플레이어에게 팀 할당
- 팀전 → 개인전 변경 시 모든 플레이어의 팀 해제
- 팀전 → 팀전 변경 시 팀 재할당
- 개인전 → 개인전 변경 시 팀 상태 유지
- 빈 방에서의 팀 설정 변경
- Redis 연결 상태 확인

## 사전 요구사항

### 1. Redis 서버 실행
```bash
# Windows (Redis 설치 후)
redis-server

# 또는 Docker 사용
docker run -d -p 6379:6379 redis:latest
```

### 2. MySQL 서버 실행
```bash
# MySQL 서버가 localhost:3306에서 실행 중이어야 함
# 데이터베이스: kospot-test
# 사용자: root
# 비밀번호: 1234
```

## 테스트 실행 방법

### 1. 전체 테스트 실행
```bash
# 프로젝트 루트에서
./gradlew test --tests "*TeamSettingTest*" --info
```

### 2. 개별 테스트 실행
```bash
# 통합 테스트만 실행
./gradlew test --tests "UpdateGameRoomSettingsUseCaseTest" --info

# 단위 테스트만 실행
./gradlew test --tests "GameRoomRedisServiceTeamTest" --info
```

### 3. IDE에서 실행
- IntelliJ IDEA: 테스트 클래스 우클릭 → "Run 'TestClassName'"
- Eclipse: 테스트 클래스 우클릭 → "Run As" → "JUnit Test"

## 테스트 결과 확인

### 성공적인 테스트 실행 시 출력 예시
```
[INFO] 개인전 → 팀전 변경 테스트 완료 - RED: 3, BLUE: 2
[INFO] 팀전 → 개인전 변경 테스트 완료
[INFO] 팀전 → 팀전 재할당 테스트 완료
[INFO] 개인전 → 개인전 유지 테스트 완료
[INFO] 빈 방 팀 설정 변경 테스트 완료
[INFO] Redis 연결 테스트 완료
```

### 실패 시 확인사항
1. **Redis 연결 실패**: Redis 서버가 실행 중인지 확인
2. **MySQL 연결 실패**: MySQL 서버 및 데이터베이스 설정 확인
3. **테스트 데이터 충돌**: 이전 테스트 데이터가 남아있을 수 있음

## 테스트 데이터 관리

### 자동 정리
- 각 테스트는 `@AfterEach`에서 자동으로 Redis 데이터를 정리합니다
- 테스트용 게임방과 플레이어 데이터는 테스트 완료 후 삭제됩니다

### 수동 정리 (필요시)
```bash
# Redis 데이터 전체 삭제
redis-cli FLUSHALL

# 특정 패턴의 키 삭제
redis-cli --scan --pattern "game:room:*" | xargs redis-cli DEL
```

## 문제 해결

### 1. Redis 연결 오류
```
Could not connect to Redis at localhost:6379
```
**해결방법**: Redis 서버가 실행 중인지 확인하고 포트 6379가 사용 가능한지 확인

### 2. MySQL 연결 오류
```
Access denied for user 'root'@'localhost'
```
**해결방법**: `application-test.yml`의 데이터베이스 설정 확인

### 3. 테스트 실패
```
Assertion failed: Expected 4 but was 0
```
**해결방법**: 
- Redis 데이터가 정상적으로 저장되었는지 확인
- 테스트 실행 순서가 올바른지 확인
- 이전 테스트 데이터가 남아있지 않은지 확인

## 성능 고려사항

### 테스트 실행 시간
- 통합 테스트: 약 10-15초
- 단위 테스트: 약 5-8초
- 전체 테스트: 약 15-25초

### Redis 메모리 사용량
- 테스트당 약 1-2MB 사용
- 테스트 완료 후 자동 정리로 메모리 해제

## 추가 테스트 케이스

### 확장 가능한 테스트 시나리오
1. **동시성 테스트**: 여러 스레드에서 동시에 팀 설정 변경
2. **대용량 데이터 테스트**: 많은 플레이어가 있는 방에서 팀 설정 변경
3. **네트워크 장애 테스트**: Redis 연결이 불안정한 상황에서의 동작
4. **메모리 누수 테스트**: 장시간 반복 실행 시 메모리 사용량 모니터링

### 커스텀 테스트 추가
```java
@DisplayName("커스텀 테스트 케이스")
@Test
void testCustomScenario() {
    // Given
    // When
    // Then
}
```

## 참고사항

- 테스트는 `@ActiveProfiles("test")`로 test 프로파일을 사용합니다
- Redis 데이터는 테스트용으로만 사용되며 프로덕션 데이터에 영향을 주지 않습니다
- 각 테스트는 독립적으로 실행되며 서로 의존하지 않습니다
