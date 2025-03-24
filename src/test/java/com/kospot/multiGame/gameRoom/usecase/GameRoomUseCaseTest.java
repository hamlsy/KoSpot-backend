package com.kospot.multiGame.gameRoom.usecase;

import com.kospot.application.multiGame.gameRoom.*;
import com.kospot.domain.member.entity.Member;
import com.kospot.domain.member.entity.Role;
import com.kospot.domain.member.repository.MemberRepository;
import com.kospot.domain.multiGame.gameRoom.entity.GameRoom;
import com.kospot.domain.multiGame.gameRoom.repository.GameRoomRepository;
import com.kospot.presentation.multiGame.gameRoom.dto.request.GameRoomRequest;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Slf4j
@SpringBootTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
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
    private Member adminMember;

    @BeforeEach
    void setUp() {
        member = Member.builder()
                        .username("member1")
                        .nickname("member1")
                        .role(Role.USER)
                        .build();


        adminMember = Member.builder()
                        .username("admin1")
                        .nickname("admin1")
                        .role(Role.ADMIN)
                        .build();

        memberRepository.save(member);
        memberRepository.save(adminMember);
    }

    @DisplayName("멀티게임 방 만들기를 테스트합니다.")
    @Test
    void createGameRoomUseCaseTest() {
        //given
        GameRoomRequest.Create request = GameRoomRequest.Create.builder()
                .title("title")
                .gameModeKey("roadview")
                .gameTypeKey("individual")
                .maxPlayers(4)
                .build();
        //when
        createGameRoomUseCase.execute(member, request);

        //then
        GameRoom gameRoom = gameRoomRepository.findById(1L).orElseThrow();
        assertEquals(request.getTitle(), gameRoom.getTitle());
//        assertEquals("member1", gameRoom.getHost().getUsername());
    }

}
