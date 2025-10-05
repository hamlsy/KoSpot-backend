# JoinGameRoomUseCase 테스트 가이드

## 📋 테스트 개요

현재 `JoinGameRoomUseCase`의 설계가 올바르게 "작동"하는지 검증하는 포괄적인 테스트 코드입니다.  
성능 최적화는 나중에 진행하되, **기본적인 비즈니스 로직과 현재 설계의 특성**을 명확히 파악합니다.

## 🧪 테스트 구성

### 1. JoinGameRoomUseCaseTest.java
**기본 기능 검증 및 통합 테스트**

#### 테스트 케이스:
- ✅ **정상적인 게임방 참가 플로우** - 전체 처리 과정 검증
- ✅ **이벤트 처리 후 Redis 상태 검증** - 이벤트 기반 아키텍처 동작 확인
- ✅ **게임방 최대 인원 초과 시 예외 발생** - 용량 제한 로직 검증
- ✅ **비밀번호 방 참가** - 비밀번호 검증 로직 확인
- ✅ **잘못된 비밀번호 예외 처리** - 보안 검증
- ✅ **중복 참가 방지** - 플레이어 상태 관리 검증
- ✅ **존재하지 않는 게임방 예외 처리** - 데이터 유효성 검증
- ✅ **게임 진행 중 참가 방지** - 게임 상태 검증
- ✅ **Redis-DB 간 데이터 일관성** - 데이터 저장소 간 동기화 확인

### 2. JoinGameRoomConcurrencyTest.java
**동시성 및 성능 특성 분석**

#### 테스트 케이스:
- 🔍 **Race Condition 발생 확인** - 현재 설계의 동시성 이슈 명확화
- 🔄 **순차적 참가 테스트** - 정상 케이스에서의 동작 검증
- 📈 **성능 기준 측정** - 향후 최적화 작업의 비교 기준점 제공
- 🔍 **데이터 일관성 검증** - DB와 Redis 간 동기화 상태 분석

## 🚀 테스트 실행 방법

### 전체 테스트 실행
```bash
# Gradle 사용
./gradlew test --tests "com.kospot.application.multi.room.usecase.*"

# 또는 IDE에서 패키지 단위 실행
```

### 개별 테스트 클래스 실행
```bash
# 기본 기능 테스트만
./gradlew test --tests "com.kospot.application.multi.room.usecase.JoinGameRoomUseCaseTest"

# 동시성 테스트만
./gradlew test --tests "com.kospot.application.multi.room.usecase.JoinGameRoomConcurrencyTest"
```

## 📊 예상 테스트 결과

### ✅ 통과해야 하는 테스트
- 모든 기본 기능 테스트 (JoinGameRoomUseCaseTest)
- 순차적 참가 테스트
- 성능 기준 테스트 (현재 성능 수준에서)

### ⚠️ 현재 설계의 한계로 인한 예상 이슈
1. **Race Condition 테스트**에서 최대 인원 초과 참가 발생 가능
2. **데이터 일관성 테스트**에서 DB-Redis 간 불일치 발생 가능
3. **동시성 테스트**에서 예상보다 많은 성공 케이스 발생 가능

> 📝 **중요**: 위 이슈들은 **현재 설계의 특성**이며, 테스트 실패가 아닙니다.  
> 이후 성능 최적화 작업에서 해결될 예정입니다.

## 🔧 테스트 환경 요구사항

### 필수 설정
- Spring Boot Test 환경
- Redis (테스트용 - TestContainers 또는 로컬 Redis)
- H2 Database (또는 테스트용 DB)

### 권장 설정
```properties
# application-test.properties
spring.datasource.url=jdbc:h2:mem:testdb
spring.jpa.hibernate.ddl-auto=create-drop
spring.data.redis.host=localhost
spring.data.redis.port=6379
logging.level.com.kospot=DEBUG
```

## 📈 성능 기준점

### 현재 측정 기준 (향후 최적화 비교용)
- **평균 처리 시간**: 4명 순차 참가 시 < 5초
- **최대 처리 시간**: < 10초
- **동시성 처리**: 8개 스레드 동시 요청 처리 가능

### 향후 최적화 목표 (참고용)
- **평균 처리 시간**: < 500ms
- **동시성 안정성**: Race Condition 완전 해결
- **데이터 일관성**: DB-Redis 100% 동기화

## 🎯 테스트 활용 방법

### 1. 개발 단계
- **기본 기능 테스트**로 비즈니스 로직 검증
- **통합 테스트**로 전체 플로우 확인

### 2. 코드 리뷰 단계
- 모든 테스트 통과 확인
- 동시성 테스트 결과 분석

### 3. 성능 최적화 전
- **현재 성능 기준점** 측정 및 기록
- **문제점 명확화** 및 개선 우선순위 설정

### 4. 성능 최적화 후
- **Before/After 비교** 분석
- **동시성 이슈 해결** 검증
- **데이터 일관성 개선** 확인

## 🔍 결과 분석 가이드

### 정상 동작 지표
```
✅ 기본 기능 테스트: 전체 통과
✅ 순차 참가: 정확히 최대 인원만 성공
✅ 예외 처리: 적절한 에러 메시지와 함께 실패
✅ 데이터 저장: DB에 올바른 상태 저장
```

### 현재 설계 한계 지표
```
⚠️ 동시성 테스트: 최대 인원 초과 참가 발생
⚠️ 데이터 일관성: DB와 Redis 간 불일치
⚠️ Race Condition: 예상보다 많은 성공 케이스
```

### 성능 분석 포인트
```
📊 응답 시간 분포
📊 처리량 (TPS)
📊 에러율
📊 데이터 정합성 비율
```

---

**💡 Tip**: 테스트 실행 시 로그 레벨을 DEBUG로 설정하면 더 자세한 처리 과정을 확인할 수 있습니다.
