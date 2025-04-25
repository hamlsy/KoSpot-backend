package com.kospot.multiGame.game.usecase;

import com.kospot.application.coordinate.ImportCoordinateUseCase;
import com.kospot.application.multiGame.game.StartMultiRoadViewPlayerGameUseCase;
import com.kospot.domain.game.entity.GameMode;
import com.kospot.domain.image.entity.Image;
import com.kospot.domain.image.repository.ImageRepository;
import com.kospot.domain.member.entity.Member;
import com.kospot.domain.member.entity.Role;
import com.kospot.domain.member.repository.MemberRepository;
import com.kospot.domain.multiGame.game.entity.MultiRoadViewGame;
import com.kospot.domain.multiGame.game.entity.PlayerMatchType;
import com.kospot.domain.multiGame.game.repository.MultiRoadViewGameRepository;
import com.kospot.domain.multiGame.gamePlayer.repository.GamePlayerRepository;
import com.kospot.domain.multiGame.gameRoom.entity.GameRoom;
import com.kospot.domain.multiGame.gameRoom.entity.GameRoomStatus;
import com.kospot.domain.multiGame.gameRoom.repository.GameRoomRepository;
import com.kospot.domain.multiGame.gameRound.entity.RoadViewGameRound;
import com.kospot.domain.multiGame.gameRound.repository.RoadViewGameRoundRepository;

import com.kospot.presentation.multiGame.game.dto.request.MultiGameRequest;
import com.kospot.presentation.multiGame.game.dto.response.MultiRoadViewGameResponse;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

@Slf4j
@SpringBootTest
@ActiveProfiles("test")
public class StartMultiRoadViewPlayerGameUseCaseTest {

    @Autowired
    private StartMultiRoadViewPlayerGameUseCase startMultiRoadViewPlayerGameUseCase;
    
    @Autowired
    private GameRoomRepository gameRoomRepository;
    
    @Autowired
    private MemberRepository memberRepository;
    
    @Autowired
    private MultiRoadViewGameRepository multiRoadViewGameRepository;
    
    @Autowired
    private RoadViewGameRoundRepository roadViewGameRoundRepository;
    
    @Autowired
    private GamePlayerRepository gamePlayerRepository;

    @Autowired
    private ImportCoordinateUseCase importCoordinateUseCase;

    @Autowired
    private ImageRepository imageRepository;

    private Member hostMember;
    private Member hostMember1;
    private List<Member> players;
    private GameRoom gameRoom;
    private Image image;
    
    @BeforeEach
    void setUp() {
        memberRepository.deleteAll();
        // 마커 이미지 생성
        image = Image.builder()
                .imageUrl("http://example.com/image.jpg")
                .build();
        imageRepository.save(image);

        // 멤버 생성
        hostMember = createAndSaveMember("host", "host123", Role.USER, image);
        hostMember1 = createAndSaveMember("temp host1", "host1234", Role.USER, image);
        players = new ArrayList<>();
        players.add(hostMember);
        log.info("{}", hostMember);

        // 추가 플레이어 생성
        for (int i = 1; i <= 3; i++) {
            Member player = createAndSaveMember("player" + i, "nick" + i, Role.USER, image);
            log.info("{}", player);
            players.add(player);
        }
        
        // 게임룸 생성
        gameRoom = createAndSaveGameRoom(hostMember, players);

        importCoordinateUseCase.execute("test_coordinates_excel.xlsx");
    }
    
    @Test
    @DisplayName("정상 케이스: 로드뷰 멀티게임 시작 성공")
    @Transactional
    void startGameSuccess() {
        // given
        MultiGameRequest.Start request = createStartRequest(gameRoom.getId());
        
        // when
        MultiRoadViewGameResponse.StartPlayerGame response = startMultiRoadViewPlayerGameUseCase.execute(hostMember, request);
        
        // then
        assertNotNull(response);
        assertNotNull(response.getGameId());
        
        // 게임 생성 확인
        MultiRoadViewGame game = multiRoadViewGameRepository.findById(response.getGameId()).orElse(null);
        assertNotNull(game);
        
        // 라운드 생성 확인
        List<RoadViewGameRound> rounds = roadViewGameRoundRepository.findAllByMultiRoadViewGameId(game.getId());
        assertFalse(rounds.isEmpty());
        assertEquals(1, rounds.get(0).getCurrentRound());
        
        // 게임 플레이어 생성 확인
        assertEquals(players.size(), gamePlayerRepository.countByGameRoomId(game.getId()));
    }
    
    @Test
    @DisplayName("예외 케이스: 호스트가 아닌 사용자가 게임 시작 시도")
    @Transactional
    void startGameNotHost() {
        // given
        MultiGameRequest.Start request = createStartRequest(gameRoom.getId());
        Member notHost = players.get(1); // 호스트가 아닌 멤버
        
        // when & then
        assertThrows(Exception.class, () -> {
            startMultiRoadViewPlayerGameUseCase.execute(notHost, request);
        });
    }
    
    @Test
    @DisplayName("예외 케이스: 존재하지 않는 게임룸으로 게임 시작 시도")
    @Transactional
    void startGameWithNonExistentRoom() {
        // given
        MultiGameRequest.Start request = createStartRequest(9999L); // 존재하지 않는 ID
        
        // when & then
        assertThrows(Exception.class, () -> {
            startMultiRoadViewPlayerGameUseCase.execute(hostMember, request);
        });
    }
    
    @Test
    @DisplayName("예외 케이스: 이미 게임이 시작된 방에서 게임 시작 시도")
    @Transactional
    void startGameWithAlreadyStartedRoom() {
        // given
        MultiGameRequest.Start request = createStartRequest(gameRoom.getId());
        
        // 첫 번째 게임 시작
        startMultiRoadViewPlayerGameUseCase.execute(hostMember, request);
        
        // when & then - 두 번째 게임 시작 시도
        assertThrows(Exception.class, () -> {
            startMultiRoadViewPlayerGameUseCase.execute(hostMember, request);
        });
    }
    
    @Test
    @DisplayName("예외 케이스: 최소 인원 미달 시 게임 시작 시도")
    @Transactional
    void startGameWithInsufficientPlayers() {
        // given
        // 1명만 있는 게임룸 생성
        GameRoom smallRoom = createGameRoomWithOnePlayer(hostMember1);
        MultiGameRequest.Start request = createStartRequest(smallRoom.getId());
        
        // when & then
        assertThrows(Exception.class, () -> {
            startMultiRoadViewPlayerGameUseCase.execute(hostMember1, request);
        });
    }
    
    @Test
    @DisplayName("동시성 테스트: 여러 클라이언트가 동시에 게임 시작 요청")
    void concurrentGameStart() throws InterruptedException {
        // given
        int threadCount = 5;
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger exceptionCount = new AtomicInteger(0);
        MultiGameRequest.Start request = createStartRequest(gameRoom.getId());
        
        // when
        for (int i = 0; i < threadCount; i++) {
            executorService.execute(() -> {
                try {
                    startMultiRoadViewPlayerGameUseCase.execute(hostMember, request);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    exceptionCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }
        latch.await(); // 모든 스레드가 완료될 때까지 대기
        
        // then
        // 정확히 한 번만 성공해야 함
        assertEquals(1, successCount.get());
        assertEquals(threadCount - 1, exceptionCount.get());
        
        // 게임이 정확히 한 개만 생성되었는지 확인
        List<MultiRoadViewGame> games = multiRoadViewGameRepository.findAllByGameRoomId(gameRoom.getId());
        assertEquals(1, games.size());
    }
    
    @Test
    @DisplayName("부하 테스트: 다수의 게임룸에서 동시에 게임 시작")
    void multipleGameRoomsStart() throws InterruptedException {
        // given
        int roomCount = 5;
        List<GameRoom> rooms = new ArrayList<>();
        List<Member> hosts = new ArrayList<>();
        
        // 여러 개의 게임룸과 호스트 생성
        for (int i = 0; i < roomCount; i++) {
            Member host = createAndSaveMember("host" + i, "host" + i, Role.USER, image);
            hosts.add(host);
            
            List<Member> roomPlayers = new ArrayList<>();
            roomPlayers.add(host);
            for (int j = 0; j < 3; j++) {
                Member player = createAndSaveMember("player" + i + j, "player" + i + j, Role.USER, image);
                roomPlayers.add(player);
            }
            
            GameRoom room = createAndSaveGameRoom(host, roomPlayers);
            rooms.add(room);
        }
        
        ExecutorService executorService = Executors.newFixedThreadPool(roomCount);
        CountDownLatch latch = new CountDownLatch(roomCount);
        AtomicInteger successCount = new AtomicInteger(0);
        
        // when
        for (int i = 0; i < roomCount; i++) {
            final int index = i;
            executorService.execute(() -> {
                try {
                    MultiGameRequest.Start request = createStartRequest(rooms.get(index).getId());
                    startMultiRoadViewPlayerGameUseCase.execute(hosts.get(index), request);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    System.err.println("Error in room " + index + ": " + e.getMessage());
                } finally {
                    latch.countDown();
                }
            });
        }
        latch.await();
        
        // then
        assertEquals(roomCount, successCount.get());
        assertEquals(roomCount, multiRoadViewGameRepository.count());
    }
    
    // 헬퍼 메소드
    private Member createAndSaveMember(String username, String nickname, Role role, Image image) {
        Member member = Member.builder()
                .username(username)
                .nickname(nickname)
                .equippedMarkerImage(image)
                .role(role)
                .build();
        return memberRepository.save(member);
    }
    
    private GameRoom createAndSaveGameRoom(Member host, List<Member> players) {
        GameRoom gameRoom = GameRoom.builder()
                .host(host)
                .gameMode(GameMode.ROADVIEW)
                .maxPlayers(6)
                .playerMatchType(PlayerMatchType.INDIVIDUAL)
                .status(GameRoomStatus.WAITING)
                .title("Test Game Room")
                .build();
        
        // 플레이어들을 게임룸에 등록하는 로직
        // 실제 구현에 맞게 이 부분은 수정이 필요할 수 있습니다
        GameRoom savedRoom = gameRoomRepository.save(gameRoom);
        
        // 플레이어 추가 로직 (실제 구현에 맞게 수정 필요)
        for (Member player : players) {
            gameRoom.join(player);
        }
        
        return gameRoomRepository.save(gameRoom);
    }
    
    private GameRoom createGameRoomWithOnePlayer(Member host) {
        GameRoom gameRoom = GameRoom.builder()
                .host(host)
                .gameMode(GameMode.ROADVIEW)
                .maxPlayers(6)
                .title("Test Game Room1")
                .build();
        
        GameRoom savedRoom = gameRoomRepository.save(gameRoom);
        savedRoom.join(host);
        
        return gameRoomRepository.save(savedRoom);
    }
    
    private MultiGameRequest.Start createStartRequest(Long gameRoomId) {
        MultiGameRequest.Start request = new MultiGameRequest.Start();
        request.setGameRoomId(gameRoomId);
        request.setTotalRounds(10);
        request.setPlayerMatchTypeKey("individual");
        return request;
    }
}
