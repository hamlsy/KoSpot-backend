package com.kospot.application.chat.usecase;

import com.kospot.domain.member.entity.Member;
import com.kospot.infrastructure.annotation.usecase.UseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;

@Slf4j
@UseCase
@RequiredArgsConstructor
public class JoinGlobalLobbyUseCase {

    private static final String REDIS_LOBBY_MEMBERS = "lobby:members";
    private static final String GLOBAL_LOBBY_CHANNEL = "global_lobby";

    public void execute(Member member,
                        SimpMessageHeaderAccessor headerAccessor) {
        //todo
        // 1.Redis에 회원 세션 추가
        // 2 최근 채팅 히스토리 전송 (Redis 캐시에서)


    }

}
