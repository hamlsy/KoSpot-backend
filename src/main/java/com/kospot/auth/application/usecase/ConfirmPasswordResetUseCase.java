package com.kospot.auth.application.usecase;

import com.kospot.auth.application.service.PasswordResetTokenRedisService;
import com.kospot.common.annotation.usecase.UseCase;
import com.kospot.member.application.service.MemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

@UseCase
@RequiredArgsConstructor
// @Transactional 없음 — Redis 삭제(getAndInvalidate)를 DB 트랜잭션 밖에서 먼저 수행해야 함
// DB 쓰기는 MemberService.updatePasswordById()의 @Transactional이 처리
public class ConfirmPasswordResetUseCase {

    private final PasswordResetTokenRedisService tokenRedisService;
    private final MemberService memberService;
    private final BCryptPasswordEncoder passwordEncoder;

    public void execute(String token, String newPassword) {
        // 1. Redis 토큰 조회 + 삭제 (GETDEL, 원자적) — 트랜잭션 없음
        //    @Transactional 안에서 호출 시 DB rollback과 무관하게 Redis 삭제가 커밋되어
        //    토큰 유실 버그 발생 → 반드시 트랜잭션 외부에서 실행
        Long memberId = tokenRedisService.getAndInvalidate(token);

        // 2. DB 조회 + 비밀번호 업데이트 — MemberService @Transactional 안에서 하나의 트랜잭션으로 처리
        memberService.updatePasswordById(memberId, passwordEncoder.encode(newPassword));
    }
}
