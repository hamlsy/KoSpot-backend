package com.kospot.auth.application.usecase;

import com.kospot.auth.application.service.PasswordResetEmailComposer;
import com.kospot.auth.application.service.PasswordResetRateLimitService;
import com.kospot.auth.application.service.PasswordResetTokenRedisService;
import com.kospot.common.annotation.usecase.UseCase;
import com.kospot.member.application.adaptor.MemberAdaptor;
import com.kospot.member.domain.entity.Member;
import com.kospot.member.domain.vo.AuthProvider;
import lombok.RequiredArgsConstructor;

import java.util.Optional;

@UseCase
@RequiredArgsConstructor
// @Transactional 없음 — Redis 작업과 DB 작업이 혼재하므로 클래스 레벨 트랜잭션 선언 금지
// DB 읽기는 MemberAdaptor의 @Transactional(readOnly = true)가 처리
public class RequestPasswordResetUseCase {

    private final MemberAdaptor memberAdaptor;
    private final PasswordResetTokenRedisService tokenRedisService;
    private final PasswordResetRateLimitService rateLimitService;
    private final PasswordResetEmailComposer emailComposer;

    public void execute(String email) {
        // 1. Rate Limit 체크 (Redis) — 트랜잭션 없음
        rateLimitService.checkAndIncrement(email);

        // 2. DB 조회 — MemberAdaptor 자체 @Transactional(readOnly = true) 적용
        Optional<Member> memberOpt = memberAdaptor.findByEmail(email);
        if (memberOpt.isEmpty()) return;

        Member member = memberOpt.get();
        if (member.getAuthProvider() != AuthProvider.LOCAL) return;

        // 3. 토큰 저장 (Redis) — 트랜잭션 없음
        String token = tokenRedisService.generateAndSave(member.getId());

        // 4. 이메일 발송 — 트랜잭션 없음
        emailComposer.send(email, token);
    }
}
