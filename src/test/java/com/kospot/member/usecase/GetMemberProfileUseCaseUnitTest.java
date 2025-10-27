package com.kospot.member.usecase;

import com.kospot.application.member.GetMemberProfileUseCase;
import com.kospot.domain.game.vo.GameMode;
import com.kospot.domain.gamerank.adaptor.GameRankAdaptor;
import com.kospot.domain.gamerank.entity.GameRank;
import com.kospot.domain.gamerank.vo.RankLevel;
import com.kospot.domain.gamerank.vo.RankTier;
import com.kospot.domain.member.adaptor.MemberStatisticAdaptor;
import com.kospot.domain.member.entity.Member;
import com.kospot.domain.member.entity.MemberStatistic;
import com.kospot.domain.member.vo.Role;
import com.kospot.domain.memberitem.repository.MemberItemRepository;
import com.kospot.presentation.member.dto.response.MemberProfileResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("GetMemberProfileUseCase 단위 테스트")
public class GetMemberProfileUseCaseUnitTest {

    @Mock
    private MemberStatisticAdaptor memberStatisticAdaptor;

    @Mock
    private MemberItemRepository memberItemRepository;

    @Mock
    private GameRankAdaptor gameRankAdaptor;

    @InjectMocks
    private GetMemberProfileUseCase getMemberProfileUseCase;

    private Member testMember;
    private MemberStatistic testStatistic;
    private GameRank testGameRank;

    @BeforeEach
    void setUp() {
        testMember = createTestMember();
        testStatistic = createTestStatistic();
        testGameRank = createTestGameRank();
    }

    @DisplayName("프로필 조회 시 모든 정보가 올바르게 매핑된다")
    @Test
    void execute_Success() {
        // given
        when(memberStatisticAdaptor.queryByMember(testMember)).thenReturn(testStatistic);
        when(gameRankAdaptor.queryByMemberAndGameMode(testMember, GameMode.ROADVIEW)).thenReturn(testGameRank);
        when(memberItemRepository.countByMember(testMember)).thenReturn(5L);
        when(memberItemRepository.countEquippedByMember(testMember)).thenReturn(2L);

        // when
        MemberProfileResponse response = getMemberProfileUseCase.execute(testMember);

        // then
        assertNotNull(response);
        assertEquals("testNickname", response.getNickname());
        assertEquals("test@kospot.com", response.getEmail());
        assertEquals(10000, response.getCurrentPoint());
        assertNotNull(response.getJoinedAt());
    }

    @DisplayName("게임 통계가 MemberStatistic에서 올바르게 조회된다")
    @Test
    void execute_GameStatistics() {
        // given
        when(memberStatisticAdaptor.queryByMember(testMember)).thenReturn(testStatistic);
        when(gameRankAdaptor.queryByMemberAndGameMode(any(), any())).thenReturn(testGameRank);
        when(memberItemRepository.countByMember(any())).thenReturn(0L);
        when(memberItemRepository.countEquippedByMember(any())).thenReturn(0L);

        // when
        MemberProfileResponse response = getMemberProfileUseCase.execute(testMember);
        MemberProfileResponse.GameStatistics stats = response.getStatistics();

        // then
        assertNotNull(stats);
        assertEquals(50L, stats.getSingleGame().getPractice().getTotalGames());
        assertEquals(3200.5, stats.getSingleGame().getPractice().getAverageScore());
        assertEquals(30L, stats.getSingleGame().getRank().getTotalGames());
        assertEquals(4100.0, stats.getSingleGame().getRank().getAverageScore());
        assertEquals(25L, stats.getMultiGame().getTotalGames());
        assertEquals(3900.0, stats.getMultiGame().getAverageScore());
        assertEquals(8L, stats.getMultiGame().getFirstPlaceCount());
        assertEquals(10L, stats.getMultiGame().getSecondPlaceCount());
        assertEquals(5L, stats.getMultiGame().getThirdPlaceCount());
        assertEquals(4800.0, stats.getBestScore());
    }

    @DisplayName("랭킹 정보가 GameRank에서 올바르게 조회된다")
    @Test
    void execute_RankInfo() {
        // given
        when(memberStatisticAdaptor.queryByMember(any())).thenReturn(testStatistic);
        when(gameRankAdaptor.queryByMemberAndGameMode(testMember, GameMode.ROADVIEW)).thenReturn(testGameRank);
        when(memberItemRepository.countByMember(any())).thenReturn(0L);
        when(memberItemRepository.countEquippedByMember(any())).thenReturn(0L);

        // when
        MemberProfileResponse response = getMemberProfileUseCase.execute(testMember);
        MemberProfileResponse.RankInfo rankInfo = response.getRankInfo();

        // then
        assertNotNull(rankInfo);
        assertEquals(RankTier.PLATINUM, rankInfo.getRoadViewRank().getTier());
        assertEquals(RankLevel.TWO, rankInfo.getRoadViewRank().getLevel());
        assertEquals(2100, rankInfo.getRoadViewRank().getRatingScore());
    }

    @DisplayName("아이템 정보가 Repository에서 올바르게 조회된다")
    @Test
    void execute_ItemInfo() {
        // given
        when(memberStatisticAdaptor.queryByMember(any())).thenReturn(testStatistic);
        when(gameRankAdaptor.queryByMemberAndGameMode(any(), any())).thenReturn(testGameRank);
        when(memberItemRepository.countByMember(testMember)).thenReturn(12L);
        when(memberItemRepository.countEquippedByMember(testMember)).thenReturn(4L);

        // when
        MemberProfileResponse response = getMemberProfileUseCase.execute(testMember);
        MemberProfileResponse.ItemInfo itemInfo = response.getItemInfo();

        // then
        assertNotNull(itemInfo);
        assertEquals(12, itemInfo.getTotalItems());
        assertEquals(4, itemInfo.getEquippedItems());
    }

    @DisplayName("연속 플레이 기록이 올바르게 반환된다")
    @Test
    void execute_CurrentStreak() {
        // given
        when(memberStatisticAdaptor.queryByMember(testMember)).thenReturn(testStatistic);
        when(gameRankAdaptor.queryByMemberAndGameMode(any(), any())).thenReturn(testGameRank);
        when(memberItemRepository.countByMember(any())).thenReturn(0L);
        when(memberItemRepository.countEquippedByMember(any())).thenReturn(0L);

        // when
        MemberProfileResponse response = getMemberProfileUseCase.execute(testMember);

        // then
        assertEquals(15, response.getCurrentStreak());
        assertNotNull(response.getLastPlayedAt());
    }

    @DisplayName("통계가 0인 경우에도 정상 동작한다")
    @Test
    void execute_EmptyStatistics() {
        // given
        MemberStatistic emptyStatistic = MemberStatistic.create(testMember);
        when(memberStatisticAdaptor.queryByMember(testMember)).thenReturn(emptyStatistic);
        when(gameRankAdaptor.queryByMemberAndGameMode(any(), any())).thenReturn(testGameRank);
        when(memberItemRepository.countByMember(any())).thenReturn(0L);
        when(memberItemRepository.countEquippedByMember(any())).thenReturn(0L);

        // when
        MemberProfileResponse response = getMemberProfileUseCase.execute(testMember);

        // then
        assertNotNull(response);
        assertEquals(0L, response.getStatistics().getSingleGame().getPractice().getTotalGames());
        assertEquals(0.0, response.getStatistics().getSingleGame().getPractice().getAverageScore());
        assertEquals(0, response.getCurrentStreak());
        assertNull(response.getLastPlayedAt());
    }

    @DisplayName("read-only 트랜잭션이므로 여러 번 조회해도 동일한 결과를 반환한다")
    @Test
    void execute_ReadOnlyConsistency() {
        // given
        when(memberStatisticAdaptor.queryByMember(testMember)).thenReturn(testStatistic);
        when(gameRankAdaptor.queryByMemberAndGameMode(any(), any())).thenReturn(testGameRank);
        when(memberItemRepository.countByMember(any())).thenReturn(5L);
        when(memberItemRepository.countEquippedByMember(any())).thenReturn(2L);

        // when
        MemberProfileResponse response1 = getMemberProfileUseCase.execute(testMember);
        MemberProfileResponse response2 = getMemberProfileUseCase.execute(testMember);

        // then
        assertEquals(response1.getNickname(), response2.getNickname());
        assertEquals(response1.getCurrentPoint(), response2.getCurrentPoint());
        assertEquals(response1.getCurrentStreak(), response2.getCurrentStreak());
        assertEquals(response1.getStatistics().getBestScore(), response2.getStatistics().getBestScore());
    }

    private Member createTestMember() {
        return Member.builder()
                .id(1L)
                .username("testUser")
                .nickname("testNickname")
                .email("test@kospot.com")
                .role(Role.USER)
                .createdDate(LocalDateTime.now())
                .point(10000)
                .build();
    }

    private MemberStatistic createTestStatistic() {
        return MemberStatistic.builder()
                .id(1L)
                .member(testMember)
                .singlePracticeGames(50L)
                .singlePracticeAvgScore(3200.5)
                .singlePracticeTotalScore(160025.0)
                .singleRankGames(30L)
                .singleRankAvgScore(4100.0)
                .singleRankTotalScore(123000.0)
                .multiGames(25L)
                .multiAvgScore(3900.0)
                .multiTotalScore(97500.0)
                .multiFirstPlace(8L)
                .multiSecondPlace(10L)
                .multiThirdPlace(5L)
                .bestScore(4800.0)
                .currentStreak(15)
                .longestStreak(20)
                .lastPlayedDate(LocalDate.now())
                .lastPlayedAt(LocalDateTime.now().minusHours(1))
                .build();
    }

    private GameRank createTestGameRank() {
        return GameRank.builder()
                .id(1L)
                .member(testMember)
                .gameMode(GameMode.ROADVIEW)
                .ratingScore(2100)
                .rankTier(RankTier.PLATINUM)
                .rankLevel(RankLevel.TWO)
                .build();
    }
}

