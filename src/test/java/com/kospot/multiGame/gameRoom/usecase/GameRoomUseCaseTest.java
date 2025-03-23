package com.kospot.multiGame.gameRoom.usecase;

import com.kospot.application.multiGame.gameRoom.*;
import com.kospot.domain.member.entity.Member;
import com.kospot.domain.member.entity.Role;
import com.kospot.domain.member.repository.MemberRepository;
import com.kospot.domain.multiGame.gameRoom.repository.GameRoomRepository;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@Slf4j
@SpringBootTest
@ActiveProfiles("test")
public class GameRoomUseCaseTest {

    //usecase
    @Autowired
    private FindAllGameRoomUseCase findAllGameRoomUseCase;

    @Autowired
    private CreateGameRoomUseCase createGameRoomUseCase;

    @Autowired
    private UpdateGameRoomUseCase updateGameRoomUseCase;

    @Autowired
    private JoinGameRoomUseCase joinGameRoomUseCase;

    @Autowired
    private KickPlayerUseCase kickPlayerUseCase;

    @Autowired
    private LeaveGameRoomUseCase leaveGameRoomUseCase;

    //repository
    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private GameRoomRepository gameRoomRepository;


    private Member member;
    private Member admin;

    @BeforeEach
    void setUp() {
        member = memberRepository.save(
                Member.builder()
                        .username("member")
                        .nickname("member")
                        .role(Role.USER)
                        .build()
        );

        admin = memberRepository.save(
                Member.builder()
                        .username("admin")
                        .nickname("admin")
                        .role(Role.ADMIN)
                        .build()
        );
    }



}
