package com.kospot.multiGame.gameRoom.usecase;

import com.kospot.application.multiGame.gameRoom.*;
import com.kospot.domain.game.entity.GameMode;
import com.kospot.domain.game.entity.GameType;
import com.kospot.domain.member.adaptor.MemberAdaptor;
import com.kospot.domain.member.entity.Member;
import com.kospot.domain.member.entity.Role;
import com.kospot.domain.member.repository.MemberRepository;
import com.kospot.domain.multiGame.gameRoom.adaptor.GameRoomAdaptor;
import com.kospot.domain.multiGame.gameRoom.entity.GameRoom;
import com.kospot.domain.multiGame.gameRoom.entity.GameRoomStatus;
import com.kospot.domain.multiGame.gameRoom.repository.GameRoomRepository;
import com.kospot.domain.multiGame.gameRoom.service.GameRoomService;
import com.kospot.presentation.multiGame.gameRoom.dto.request.GameRoomRequest;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

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

    //repository, adaptor
    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private GameRoomRepository gameRoomRepository;

    @Autowired
    private MemberAdaptor memberAdaptor;

    @Autowired
    private GameRoomAdaptor gameRoomAdaptor;

    //service
    @Autowired
    private GameRoomService gameRoomService;


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


    @DisplayName("멀티게임 방 수정을 테스트합니다.")
    @Test
    void updateGameRoomUseCaseTest() {
        //given
        GameRoom gameRoom = gameRoomRepository.save(
                GameRoom.builder()
                        .title("title")
                        .host(member)
                        .gameMode(GameMode.ROADVIEW)
                        .gameType(GameType.COOPERATIVE)
                        .maxPlayers(4)
                        .build()
        );

        GameRoomRequest.Update request = GameRoomRequest.Update.builder()
                .title("title1")
                .gameModeKey("roadview")
                .gameTypeKey("individual")
                .maxPlayers(4)
                .build();

        //when
        updateGameRoomUseCase.execute(member, request, gameRoom.getId());

        //then
        GameRoom updatedGameRoom = gameRoomRepository.findByIdFetchHost(gameRoom.getId()).orElseThrow();
        assertEquals(request.getTitle(), updatedGameRoom.getTitle());
        assertEquals(GameType.INDIVIDUAL, updatedGameRoom.getGameType());
        assertEquals(member.getUsername(), updatedGameRoom.getHost().getUsername());

    }

    @DisplayName("방에 들어와 있지 않은 플레이어가 나가려는 경우를 테스트합니다.")
    @Test
    void leaveGameRoom_WhenPlayerNotInRoom_Test() {
        //given
        //gameRoom, host
        GameRoom gameRoom = gameRoomRepository.save(
                GameRoom.builder()
                        .title("title")
                        .host(member)
                        .gameMode(GameMode.ROADVIEW)
                        .gameType(GameType.COOPERATIVE)
                        .maxPlayers(4)
                        .build()
        );

        //when
        Member anotherPlayer = memberRepository.save(
                Member.builder()
                        .username("another")
                        .nickname("another")
                        .role(Role.USER)
                        .build()
        );

        //then
        assertDoesNotThrow(() -> leaveGameRoomUseCase.execute(anotherPlayer, gameRoom.getId()));
    }

    @DisplayName("호스트가 방을 나가는 경우를 테스트합니다.")
    @Test
    @Transactional
    void leaveGameRoom_WhenHostLeaves_Test() {
        //given
        //player 3, host 1
        List<Member> players = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            Member player = Member.builder()
                    .username("player" + (i + 1))
                    .nickname("player" + (i + 1))
                    .role(Role.USER)
                    .build();
            players.add(player);
        }
        memberRepository.saveAll(players);

        //gameRoom, host
        GameRoom gameRoom = gameRoomRepository.save(getTestGameRoom());
        players.forEach(gameRoom::join);

        //when
        leaveGameRoomUseCase.execute(member, gameRoom.getId());

        //then
        assertThrows(Exception.class, ()-> gameRoomRepository.findById(gameRoom.getId()).orElseThrow());
        players.forEach(player -> assertNull(player.getGameRoom()));
        players.forEach(Assertions::assertNotNull);
        assertNotNull(member);

    }

    @DisplayName("일반 플레이어가 방을 나가는 경우 남은 인원을 조회하는 테스트입니다.")
    @Test
    @Transactional
    void leaveGameRoom_WhenRegularPlayerLeaves_CheckRemainingPlayers_Test() {
        //given
        //player 3, host 1
        List<Member> players = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            Member player = Member.builder()
                    .username("player" + (i + 1))
                    .nickname("player" + (i + 1))
                    .role(Role.USER)
                    .build();
            players.add(player);
        }
        memberRepository.saveAll(players);

        //gameRoom, host
        GameRoom gameRoom = gameRoomRepository.save(getTestGameRoom());
        GameRoomRequest.Join request = GameRoomRequest.Join.builder()
                .password(null)
                .build();

        gameRoomService.joinGameRoom(players.get(0), gameRoom, request);
        gameRoomService.joinGameRoom(players.get(1), gameRoom, request);
        gameRoomService.joinGameRoom(players.get(2), gameRoom, request);

        //when
        Member player1 = memberRepository.findById(3L).orElseThrow();
        leaveGameRoomUseCase.execute(player1, gameRoom.getId());

        //then
        GameRoom updatedGameRoom = gameRoomAdaptor.queryByIdFetchPlayers(gameRoom.getId());
        assertEquals(2, updatedGameRoom.getWaitingPlayers().size());
        assertNull(player1.getGameRoom());
    }


    private GameRoom getTestGameRoom() {
        return GameRoom.builder()
                .title("title")
                .host(member)
                .gameMode(GameMode.ROADVIEW)
                .gameType(GameType.COOPERATIVE)
                .privateRoom(false)
                .status(GameRoomStatus.WAITING)
                .maxPlayers(4)
                .build();
    }


    //todo 비밀번호 체크 테스트

}
