package com.kospot.member.usecase;

import com.kospot.application.member.GetMemberProfileUseCase;
import com.kospot.domain.game.vo.GameMode;
import com.kospot.domain.gamerank.entity.GameRank;
import com.kospot.domain.gamerank.repository.GameRankRepository;
import com.kospot.domain.gamerank.vo.RankLevel;
import com.kospot.domain.gamerank.vo.RankTier;
import com.kospot.domain.item.entity.Item;
import com.kospot.domain.item.repository.ItemRepository;
import com.kospot.domain.item.vo.ItemType;
import com.kospot.domain.member.entity.Member;
import com.kospot.domain.member.entity.MemberStatistic;
import com.kospot.domain.member.repository.MemberRepository;
import com.kospot.domain.member.repository.MemberStatisticRepository;
import com.kospot.domain.member.vo.Role;
import com.kospot.domain.memberitem.entity.MemberItem;
import com.kospot.domain.memberitem.repository.MemberItemRepository;
import com.kospot.presentation.member.dto.response.MemberProfileResponse;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

@Slf4j
@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class GetMemberProfileUseCaseTest {

    @Autowired
    private GetMemberProfileUseCase getMemberProfileUseCase;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private MemberStatisticRepository memberStatisticRepository;

    @Autowired
    private GameRankRepository gameRankRepository;

    @Autowired
    private ItemRepository itemRepository;

    @Autowired
    private MemberItemRepository memberItemRepository;

    private Member testMember;
    private MemberStatistic testStatistic;

    @BeforeEach
    void setUp() {
        testMember = createTestMember("testUser", "testNickname");
        testStatistic = createTestStatistic(testMember);
        createTestGameRank(testMember);
    }

    @DisplayName("회원 프로필을 정상적으로 조회한다")
    @Test
    void getMemberProfile_Success() {
        // when
        MemberProfileResponse response = getMemberProfileUseCase.execute(testMember);

        // then
        assertNotNull(response);
        assertEquals("testNickname", response.getNickname());
        assertEquals(testMember.getEmail(), response.getEmail());
        assertEquals(testMember.getPoint(), response.getCurrentPoint());
        assertEquals(testMember.getCreatedDate(), response.getJoinedAt());
    }

    @DisplayName("게임 통계가 올바르게 조회된다")
    @Test
    void getMemberProfile_GameStatistics() {
        // when
        MemberProfileResponse response = getMemberProfileUseCase.execute(testMember);
        MemberProfileResponse.GameStatistics statistics = response.getStatistics();

        // then
        assertNotNull(statistics);
        
        // 싱글 게임 - 연습 모드
        assertEquals(10L, statistics.getSingleGame().getPractice().getTotalGames());
        assertEquals(3500.5, statistics.getSingleGame().getPractice().getAverageScore());
        
        // 싱글 게임 - 랭크 모드
        assertEquals(20L, statistics.getSingleGame().getRank().getTotalGames());
        assertEquals(4200.8, statistics.getSingleGame().getRank().getAverageScore());
        
        // 멀티 게임
        assertEquals(15L, statistics.getMultiGame().getTotalGames());
        assertEquals(3800.0, statistics.getMultiGame().getAverageScore());
        assertEquals(5L, statistics.getMultiGame().getFirstPlaceCount());
        assertEquals(7L, statistics.getMultiGame().getSecondPlaceCount());
        assertEquals(3L, statistics.getMultiGame().getThirdPlaceCount());
        
        // 최고 점수
        assertEquals(4950.0, statistics.getBestScore());
    }

    @DisplayName("랭킹 정보가 올바르게 조회된다")
    @Test
    void getMemberProfile_RankInfo() {
        // when
        MemberProfileResponse response = getMemberProfileUseCase.execute(testMember);
        MemberProfileResponse.RankInfo rankInfo = response.getRankInfo();

        // then
        assertNotNull(rankInfo);
        assertNotNull(rankInfo.getRoadViewRank());
        assertEquals(RankTier.GOLD, rankInfo.getRoadViewRank().getTier());
        assertEquals(RankLevel.THREE, rankInfo.getRoadViewRank().getLevel());
        assertEquals(1850, rankInfo.getRoadViewRank().getRatingScore());
    }

    @DisplayName("아이템 정보가 올바르게 조회된다")
    @Test
    void getMemberProfile_ItemInfo() {
        // given
        Item item1 = createTestItem("marker1", ItemType.MARKER);
        Item item2 = createTestItem("marker2", ItemType.MARKER);
        Item item3 = createTestItem("marker3", ItemType.MARKER);
        
        createTestMemberItem(testMember, item1, true);
        createTestMemberItem(testMember, item2, true);
        createTestMemberItem(testMember, item3, false);

        // when
        MemberProfileResponse response = getMemberProfileUseCase.execute(testMember);
        MemberProfileResponse.ItemInfo itemInfo = response.getItemInfo();

        // then
        assertNotNull(itemInfo);
        assertEquals(3, itemInfo.getTotalItems());
        assertEquals(2, itemInfo.getEquippedItems());
    }

    @DisplayName("연속 플레이 기록이 올바르게 조회된다")
    @Test
    void getMemberProfile_CurrentStreak() {
        // when
        MemberProfileResponse response = getMemberProfileUseCase.execute(testMember);

        // then
        assertEquals(7, response.getCurrentStreak());
    }

    @DisplayName("최근 플레이 날짜가 올바르게 조회된다")
    @Test
    void getMemberProfile_LastPlayedAt() {
        // when
        MemberProfileResponse response = getMemberProfileUseCase.execute(testMember);

        // then
        assertNotNull(response.getLastPlayedAt());
        assertEquals(testStatistic.getLastPlayedAt(), response.getLastPlayedAt());
    }

    @DisplayName("통계가 0인 회원도 정상적으로 조회된다")
    @Test
    void getMemberProfile_EmptyStatistics() {
        // given
        Member newMember = createTestMember("newUser", "newNickname");
        MemberStatistic emptyStatistic = MemberStatistic.create(newMember);
        memberStatisticRepository.save(emptyStatistic);
        createTestGameRank(newMember);

        // when
        MemberProfileResponse response = getMemberProfileUseCase.execute(newMember);

        // then
        assertNotNull(response);
        assertEquals(0L, response.getStatistics().getSingleGame().getPractice().getTotalGames());
        assertEquals(0.0, response.getStatistics().getSingleGame().getPractice().getAverageScore());
        assertEquals(0, response.getCurrentStreak());
        assertNull(response.getLastPlayedAt());
    }

    @DisplayName("전체 프로필 조회 성능 테스트")
    @Test
    void getMemberProfile_Performance() {
        // given
        long startTime = System.currentTimeMillis();

        // when
        MemberProfileResponse response = getMemberProfileUseCase.execute(testMember);

        // then
        long endTime = System.currentTimeMillis();
        long executionTime = endTime - startTime;
        
        assertNotNull(response);
        log.info("프로필 조회 실행 시간: {}ms", executionTime);
        assertTrue(executionTime < 100, "프로필 조회는 100ms 이내에 완료되어야 합니다");
    }

    private Member createTestMember(String username, String nickname) {
        Member member = Member.builder()
                .username(username)
                .nickname(nickname)
                .email(username + "@kospot.com")
                .role(Role.USER)
                .point(15000)
                .build();
        return memberRepository.save(member);
    }

    private MemberStatistic createTestStatistic(Member member) {
        MemberStatistic statistic = MemberStatistic.builder()
                .member(member)
                .singlePracticeGames(10L)
                .singlePracticeAvgScore(3500.5)
                .singlePracticeTotalScore(35005.0)
                .singleRankGames(20L)
                .singleRankAvgScore(4200.8)
                .singleRankTotalScore(84016.0)
                .multiGames(15L)
                .multiAvgScore(3800.0)
                .multiTotalScore(57000.0)
                .multiFirstPlace(5L)
                .multiSecondPlace(7L)
                .multiThirdPlace(3L)
                .bestScore(4950.0)
                .currentStreak(7)
                .longestStreak(10)
                .lastPlayedDate(LocalDate.now())
                .lastPlayedAt(LocalDateTime.now().minusHours(2))
                .build();
        return memberStatisticRepository.save(statistic);
    }

    private GameRank createTestGameRank(Member member) {
        GameRank gameRank = GameRank.builder()
                .member(member)
                .gameMode(GameMode.ROADVIEW)
                .ratingScore(1850)
                .rankTier(RankTier.GOLD)
                .rankLevel(RankLevel.THREE)
                .build();
        return gameRankRepository.save(gameRank);
    }

    private Item createTestItem(String name, ItemType itemType) {
        Item item = Item.builder()
                .name(name)
                .itemType(itemType)
                .isAvailable(true)
                .build();
        return itemRepository.save(item);
    }

    private MemberItem createTestMemberItem(Member member, Item item, boolean isEquipped) {
        MemberItem memberItem = MemberItem.builder()
                .member(member)
                .item(item)
                .isEquipped(isEquipped)
                .build();
        return memberItemRepository.save(memberItem);
    }
}

