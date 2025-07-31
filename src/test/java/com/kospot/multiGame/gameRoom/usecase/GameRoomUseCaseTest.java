package com.kospot.multiGame.gameRoom.usecase;

import com.kospot.application.multiplayer.gameroom.usecase.*;
import com.kospot.domain.game.vo.GameMode;
import com.kospot.domain.member.adaptor.MemberAdaptor;
import com.kospot.domain.member.entity.Member;
import com.kospot.domain.member.vo.Role;
import com.kospot.domain.member.repository.MemberRepository;
import com.kospot.domain.multigame.game.vo.PlayerMatchType;
import com.kospot.domain.multigame.gameRoom.adaptor.GameRoomAdaptor;
import com.kospot.domain.multigame.gameRoom.entity.GameRoom;
import com.kospot.domain.multigame.gameRoom.vo.GameRoomStatus;
import com.kospot.domain.multigame.gameRoom.repository.GameRoomRepository;
import com.kospot.domain.multigame.gameRoom.service.GameRoomService;
import com.kospot.presentation.multigame.gameroom.dto.request.GameRoomRequest;
import com.kospot.presentation.multigame.gameroom.dto.response.GameRoomDetailResponse;
import jakarta.persistence.EntityManager;
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

    @Autowired
    private FindGameRoomDetailUseCase findGameRoomDetailUseCase;

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

    @Autowired
    private EntityManager entityManager;

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
                .playerMatchTypeKey("individual")
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
                        .playerMatchType(PlayerMatchType.TEAM)
                        .maxPlayers(4)
                        .build()
        );

        GameRoomRequest.Update request = GameRoomRequest.Update.builder()
                .title("title1")
                .gameModeKey("roadview")
                .playerMatchTypeKey("individual")
                .build();

        //when
        updateGameRoomUseCase.execute(member, request, gameRoom.getId());

        //then
        GameRoom updatedGameRoom = gameRoomRepository.findByIdFetchHost(gameRoom.getId()).orElseThrow();
        assertEquals(request.getTitle(), updatedGameRoom.getTitle());
        assertEquals(PlayerMatchType.INDIVIDUAL, updatedGameRoom.getPlayerMatchType());
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
                        .playerMatchType(PlayerMatchType.TEAM)
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
        players.forEach(
                p -> gameRoom.join(p, null)
        );

        //when
        leaveGameRoomUseCase.execute(member, gameRoom.getId());

        entityManager.clear();

        //then
//        assertEquals(true, gameRoomRepository.findById(gameRoom.getId()).orElseThrow().getDeleted());
        assertThrows(Exception.class, () -> gameRoomRepository.findById(gameRoom.getId()).orElseThrow());
        players.forEach(player -> assertNull(player.getGameRoomId()));
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
        GameRoom updatedGameRoom = gameRoomAdaptor.queryById(gameRoom.getId());
        assertEquals(2, updatedGameRoom.getCurrentPlayerCount());
        assertNull(player1.getGameRoomId());
    }


    private GameRoom getTestGameRoom() {
        return GameRoom.builder()
                .title("title")
                .host(member)
                .gameMode(GameMode.ROADVIEW)
                .playerMatchType(PlayerMatchType.TEAM)
                .privateRoom(false)
                .status(GameRoomStatus.WAITING)
                .maxPlayers(4)
                .currentPlayerCount(1)
                .build();
    }

    // 5. 이미 방에 참여한 플레이어가 다른 방에 다시 참여할 때 테스트

    @DisplayName("방 참여를 테스트합니다.")
    @Test
    @Transactional
    void joinGameRoomUseCaseTest() {
        //given
        GameRoom gameRoom = gameRoomRepository.save(getTestGameRoom());
        Member member1 = createMember("member3");
        Member member2 = createMember("member2");
        GameRoomRequest.Join request = GameRoomRequest.Join.builder()
                .password(null)
                .build();


        //when
        joinGameRoomUseCase.execute(member1, gameRoom.getId(), request);
        joinGameRoomUseCase.execute(member2, gameRoom.getId(), request);

        //then
        GameRoom updatedGameRoom = gameRoomAdaptor.queryById(gameRoom.getId());
        assertEquals(2, updatedGameRoom.getCurrentPlayerCount());

    }

    @DisplayName("방 인원 초과를 테스트합니다.")
    @Test
    @Transactional
    void joinGameRoomUseCase_WhenRoomIsFull_Test() {
        //given
        GameRoom gameRoom = gameRoomRepository.save(getTestGameRoom());
        Member member1 = createMember("member3");
        Member member2 = createMember("member2");
        Member member3 = createMember("member4");
        Member member5 = createMember("member6");
        GameRoomRequest.Join request = GameRoomRequest.Join.builder()
                .password(null)
                .build();

        //when
        joinGameRoomUseCase.execute(member1, gameRoom.getId(), request);
        joinGameRoomUseCase.execute(member2, gameRoom.getId(), request);
        joinGameRoomUseCase.execute(member3, gameRoom.getId(), request);

        //then
        log.info("" + gameRoom.getCurrentPlayerCount());
        assertThrows(Exception.class, () -> joinGameRoomUseCase.execute(member5, gameRoom.getId(), request));
        assertNull(member5.getGameRoomId());
    }

    @DisplayName("비밀번호 방 테스트를 진행합니다.")
    @Test
    @Transactional
    void joinGameRoomUseCase_WhenPrivateRoom_Test() {
        //given
        String password = "111";
        GameRoom gameRoom = gameRoomRepository.save(getTestPriviateGameRoom(password));
        Member member1 = createMember("member3");
        Member member2 = createMember("member2");
        GameRoomRequest.Join request = GameRoomRequest.Join.builder()
                .password("111")
                .build();
        GameRoomRequest.Join request1 = GameRoomRequest.Join.builder()
                .password("112")
                .build();


        //when
        joinGameRoomUseCase.execute(member1, gameRoom.getId(), request);
        assertThrows(Exception.class, () -> joinGameRoomUseCase.execute(member2, gameRoom.getId(), request1));

        //then
        GameRoom updatedGameRoom = gameRoomAdaptor.queryById(gameRoom.getId());
        assertEquals(2, updatedGameRoom.getCurrentPlayerCount());
    }

    @DisplayName("비밀번호 없는 방에 비밀번호를 입력했을 경우를 테스트합니다.")
    @Test
    @Transactional
    void joinGameRoomUseCase_WhenNoPasswordRoom_Test() {
        //given
        GameRoom gameRoom = gameRoomRepository.save(getTestGameRoom());
        Member member1 = createMember("member3");
        GameRoomRequest.Join request = GameRoomRequest.Join.builder()
                .password("111")
                .build();

        //when
        assertDoesNotThrow(() -> joinGameRoomUseCase.execute(member1, gameRoom.getId(), request));

        //then
        assertNotNull(member1.getGameRoomId());
        assertEquals(2, gameRoom.getCurrentPlayerCount());
    }

    @DisplayName("이미 방에 참여한 플레이어가 다른 방에 참여하는 경우를 테스트합니다.")
    @Test
    @Transactional
    void joinGameRoomUseCase_WhenPlayerAlreadyInRoom_Test() {
        //given
        GameRoom gameRoom = gameRoomRepository.save(getTestGameRoom());
        GameRoom gameRoom1 = gameRoomRepository.save(getTestPriviateGameRoom("111"));
        Member member1 = createMember("member3");
        GameRoomRequest.Join request = GameRoomRequest.Join.builder()
                .password(null)
                .build();
        GameRoomRequest.Join request1 = GameRoomRequest.Join.builder()
                .password("111")
                .build();

        //when
        joinGameRoomUseCase.execute(member1, gameRoom.getId(), request);
//        assertThrows(Exception.class, () -> joinGameRoomUseCase.execute(member1, gameRoom1.getId(), request));
        joinGameRoomUseCase.execute(member1, gameRoom1.getId(), request1);

        //then
        GameRoom updatedGameRoom = gameRoomAdaptor.queryById(gameRoom.getId());
        assertEquals(2, updatedGameRoom.getCurrentPlayerCount());
    }

    @DisplayName("게임 방 내부 조회 테스트")
    @Test
    void findGameRoomDetailUseCaseTest() {
        //given
        GameRoom gameRoom = gameRoomRepository.save(getTestGameRoom());

        //when
        GameRoomRequest.Join request = GameRoomRequest.Join.builder()
                .password(null)
                .build();
        joinGameRoomUseCase.execute(member, gameRoom.getId(), request);

        //then
        GameRoomDetailResponse response =  findGameRoomDetailUseCase.execute(gameRoom.getId());
        assertEquals("title", response.getTitle());
    }

    private Member createMember(String username) {
        return memberRepository.save(
                Member.builder()
                        .username(username)
                        .nickname(username)
                        .role(Role.USER)
                        .build()
        );
    }

    private GameRoom getTestPriviateGameRoom(String password) {
        return GameRoom.builder()
                .title("title")
                .host(adminMember)
                .gameMode(GameMode.ROADVIEW)
                .playerMatchType(PlayerMatchType.TEAM)
                .privateRoom(true)
                .password(password)
                .status(GameRoomStatus.WAITING)
                .maxPlayers(4)
                .currentPlayerCount(1)
                .build();
    }


}
