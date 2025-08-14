package com.kospot.infrastructure.websocket.subscription;

import com.kospot.infrastructure.websocket.auth.WebSocketMemberPrincipal;

/**
 * 구독 권한 검증 인터페이스
 * - SRP: 구독 검증만 담당
 * - OCP: 새로운 도메인 추가 시 확장 가능
 */
public interface SubscriptionValidator {
    
    /**
     * 구독 권한 검증
     * @param principal 사용자 정보
     * @param destination 구독 대상
     * @return 검증 통과 여부
     */
    boolean canSubscribe(WebSocketMemberPrincipal principal, String destination);
    
    /**
     * 지원하는 destination 패턴인지 확인
     * @param destination 구독 대상
     * @return 지원 여부
     */
    boolean supports(String destination);
    
    /**
     * 검증 우선순위 (높을수록 먼저 실행)
     * @return 우선순위
     */
    int getPriority();
    
    /**
     * 검증자 이름 (로깅/디버깅용)
     * @return 검증자 이름
     */
    default String getValidatorName() {
        return this.getClass().getSimpleName();
    }
}
