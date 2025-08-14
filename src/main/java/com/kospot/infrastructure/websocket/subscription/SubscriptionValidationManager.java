package com.kospot.infrastructure.websocket.subscription;

import com.kospot.infrastructure.websocket.auth.WebSocketMemberPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;

/**
 * 구독 검증 매니저
 * - 여러 검증자를 통합 관리
 * - 우선순위에 따른 검증 실행
 * - 확장성과 유지보수성 제공
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SubscriptionValidationManager {
    
    private final List<SubscriptionValidator> validators;
    
    /**
     * 구독 권한 검증 실행
     * @param principal 사용자 정보
     * @param destination 구독 대상
     * @return 검증 통과 여부
     */
    public boolean validateSubscription(WebSocketMemberPrincipal principal, String destination) {
        if (destination == null || destination.trim().isEmpty()) {
            log.warn("Invalid destination for subscription validation: {}", destination);
            return false;
        }
        
        // 지원하는 검증자 찾기 및 우선순위 정렬
        List<SubscriptionValidator> applicableValidators = validators.stream()
                .filter(validator -> validator.supports(destination))
                .sorted(Comparator.comparingInt(SubscriptionValidator::getPriority).reversed())
                .toList();
        
        if (applicableValidators.isEmpty()) {
            log.warn("No validators found for destination: {} - AllowedPrefixes: {}", 
                    destination, getSupportedPrefixes());
            return false;
        }
        
        // 모든 적용 가능한 검증자를 통과해야 함
        for (SubscriptionValidator validator : applicableValidators) {
            try {
                boolean canSubscribe = validator.canSubscribe(principal, destination);
                
                if (!canSubscribe) {
                    log.warn("Subscription validation failed - Validator: {}, MemberId: {}, Destination: {}", 
                            validator.getValidatorName(), 
                            principal != null ? principal.getMemberId() : "null", 
                            destination);
                    return false;
                }
                
                log.debug("Subscription validation passed - Validator: {}, MemberId: {}, Destination: {}", 
                        validator.getValidatorName(), 
                        principal != null ? principal.getMemberId() : "null", 
                        destination);
                
            } catch (Exception e) {
                log.error("Subscription validation error - Validator: {}, MemberId: {}, Destination: {}, Error: {}", 
                        validator.getValidatorName(), 
                        principal != null ? principal.getMemberId() : "null", 
                        destination, 
                        e.getMessage(), e);
                return false;
            }
        }
        
        log.debug("All subscription validations passed - MemberId: {}, Destination: {}, ValidatorCount: {}", 
                principal != null ? principal.getMemberId() : "null", 
                destination, 
                applicableValidators.size());
        
        return true;
    }
    
    /**
     * 특정 destination에 적용 가능한 검증자 목록 조회
     * @param destination 구독 대상
     * @return 적용 가능한 검증자 목록
     */
    public List<SubscriptionValidator> getApplicableValidators(String destination) {
        return validators.stream()
                .filter(validator -> validator.supports(destination))
                .sorted(Comparator.comparingInt(SubscriptionValidator::getPriority).reversed())
                .toList();
    }
    
    /**
     * 등록된 모든 검증자 조회
     * @return 전체 검증자 목록
     */
    public List<SubscriptionValidator> getAllValidators() {
        return validators.stream()
                .sorted(Comparator.comparingInt(SubscriptionValidator::getPriority).reversed())
                .toList();
    }
    
    /**
     * 지원하는 destination 패턴 목록 조회 (디버깅용)
     * @return 지원 패턴 목록
     */
    public String getSupportedPrefixes() {
        return validators.stream()
                .map(validator -> validator.getValidatorName() + ": " + getValidatorSupportInfo(validator))
                .reduce((a, b) -> a + ", " + b)
                .orElse("None");
    }
    
    /**
     * 검증자별 지원 정보 조회 (디버깅용)
     */
    private String getValidatorSupportInfo(SubscriptionValidator validator) {
        // 각 검증자의 supports 메서드를 통해 지원 패턴을 확인
        String[] commonPrefixes = {"/topic/chat/", "/topic/lobby", "/topic/room/", "/topic/game/", "/user/", "/topic/notification/"};
        
        StringBuilder supportInfo = new StringBuilder();
        for (String prefix : commonPrefixes) {
            if (validator.supports(prefix)) {
                if (supportInfo.length() > 0) supportInfo.append("|");
                supportInfo.append(prefix);
            }
        }
        
        return supportInfo.length() > 0 ? supportInfo.toString() : "unknown";
    }
    
    /**
     * 검증 통계 정보 조회
     * @return 검증자별 통계 정보
     */
    public String getValidationStatistics() {
        return String.format("Total validators: %d, Active validators: %d", 
                validators.size(), 
                validators.size()); // 모든 검증자가 활성 상태라고 가정
    }
}
