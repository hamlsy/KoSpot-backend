# WebSocket 구독 검증 확장 가이드

## 🎯 개요

기존 `WebSocketChannelInterceptor`의 구독 검증 부분을 확장 가능하게 리팩토링했습니다. 이제 새로운 도메인을 추가할 때 기존 코드 수정 없이 새로운 검증자만 추가하면 됩니다.

## 🏗️ 아키텍처

### 변경 전 (기존)
```java
// WebSocketChannelInterceptor.java
private void validateBasicSubscriptionAccess(WebSocketMemberPrincipal principal, String destination) {
    // 하드코딩된 검증 로직
    // 새 도메인 추가 시 이 메서드를 수정해야 함
}
```

### 변경 후 (확장 가능)
```java
// WebSocketChannelInterceptor.java
private void validateSubscriptionAccess(WebSocketMemberPrincipal principal, String destination) {
    boolean canSubscribe = subscriptionValidationManager.validateSubscription(principal, destination);
    // 검증 매니저가 모든 등록된 검증자를 자동으로 실행
}
```

## 📋 핵심 컴포넌트

### 1. SubscriptionValidator (인터페이스)
```java
public interface SubscriptionValidator {
    boolean canSubscribe(WebSocketMemberPrincipal principal, String destination);
    boolean supports(String destination);
    int getPriority();
    String getValidatorName();
}
```

### 2. SubscriptionValidationManager (매니저)
- 모든 검증자를 자동으로 발견하고 관리
- 우선순위에 따른 검증 실행
- 통계 및 디버깅 정보 제공

### 3. 도메인별 검증자 구현체
- `GlobalLobbySubscriptionValidator`: 글로벌 로비/채팅
- `GameRoomSubscriptionValidator`: 게임방
- `GameSessionSubscriptionValidator`: 게임 세션
- `PersonalMessageSubscriptionValidator`: 개인 메시지
- `RankingSubscriptionValidator`: 실시간 랭킹 (확장 예시)

## 🚀 새로운 도메인 추가 방법

### 1단계: 검증자 구현
```java
@Component
public class NewDomainSubscriptionValidator implements SubscriptionValidator {
    
    @Override
    public boolean canSubscribe(WebSocketMemberPrincipal principal, String destination) {
        // 도메인별 검증 로직 구현
        return true;
    }
    
    @Override
    public boolean supports(String destination) {
        return destination.startsWith("/topic/newdomain/");
    }
    
    @Override
    public int getPriority() {
        return 150; // 우선순위 설정
    }
}
```

### 2단계: Spring에서 자동 등록
- `@Component` 어노테이션으로 자동 등록
- 기존 코드 수정 불필요

### 3단계: 테스트
```java
// 클라이언트에서 구독 시도
stompClient.subscribe("/topic/newdomain/data", handler);
// 새로운 검증자가 자동으로 실행됨
```

## ✅ 현재 지원하는 도메인

| 도메인 | Destination 패턴 | 우선순위 | 검증 로직 |
|--------|------------------|----------|-----------|
| 글로벌 로비 | `/topic/chat/`, `/topic/lobby` | 100 | 기본 인증만 확인 |
| 게임방 | `/topic/room/` | 200 | 게임방 참여 + 강퇴 여부 |
| 개인 메시지 | `/user/` | 250 | 본인 채널만 접근 |
| 게임 세션 | `/topic/game/` | 300 | 게임 참여 + 진행 상태 |
| 실시간 랭킹 | `/topic/ranking/` | 150 | 레벨 제한 (확장 예시) |

## 🔧 설정 및 사용법

### 검증 통계 확인
```java
String stats = subscriptionValidationManager.getValidationStatistics();
log.info("Validation Stats: {}", stats);
```

### 지원 패턴 확인
```java
String patterns = subscriptionValidationManager.getSupportedPrefixes();
log.info("Supported Patterns: {}", patterns);
```

### 특정 destination의 검증자 확인
```java
List<SubscriptionValidator> validators = 
    subscriptionValidationManager.getApplicableValidators("/topic/room/123/chat");
```

## 🐛 트러블슈팅

### 구독이 거부되는 경우
1. 로그 확인: `Subscription access denied - MemberId: {}, Destination: {}`
2. 지원 패턴 확인: `SupportedPrefixes: {}`
3. 해당 도메인의 검증자가 등록되어 있는지 확인

### 새 검증자가 동작하지 않는 경우
1. `@Component` 어노테이션 확인
2. `supports()` 메서드가 올바른 패턴을 반환하는지 확인
3. Spring 컨텍스트에 빈이 등록되었는지 확인

## 🎯 장점

✅ **확장성**: 새 도메인 추가 시 기존 코드 수정 불필요  
✅ **유지보수성**: 도메인별 검증 로직이 분리되어 관리 용이  
✅ **테스트 용이성**: 각 검증자를 독립적으로 단위 테스트 가능  
✅ **설정 가능성**: 우선순위와 조건을 쉽게 변경 가능  
✅ **모니터링**: 실시간 통계와 디버깅 정보 제공  

이제 새로운 도메인을 추가할 때마다 간단한 검증자 클래스만 만들면 됩니다!
