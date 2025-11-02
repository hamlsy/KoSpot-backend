package com.kospot.member.service;

import com.kospot.domain.game.vo.GameType;
import com.kospot.domain.member.adaptor.MemberStatisticAdaptor;
import com.kospot.domain.member.entity.Member;
import com.kospot.domain.member.entity.MemberStatistic;
import com.kospot.domain.member.repository.MemberRepository;
import com.kospot.domain.member.service.MemberStatisticService;
import com.kospot.domain.member.vo.Role;
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
public class MemberStatisticServiceTest {

    @Autowired
    private MemberStatisticService memberStatisticService;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private MemberStatisticAdaptor memberStatisticAdaptor;

    private Member testMember;

    @BeforeEach
    void setUp() {
        testMember = createTestMember("testUser", "testNickname");
    }

    @DisplayName("회원 통계를 초기화한다")
    @Test
    void initializeStatistic() {
        // when
        memberStatisticService.initializeStatistic(testMember);

        // then
        MemberStatistic statistic = memberStatisticAdaptor.queryByMember(testMember);
        assertNotNull(statistic);
        assertEquals(0L, statistic.getRoadviewPracticeGames());
        assertEquals(0L, statistic.getRoadviewRankGames());
        assertEquals(0L, statistic.getRoadviewMultiGames());
        assertEquals(0.0, statistic.getBestScore());
        assertEquals(0, statistic.getCurrentStreak());
    }

    @DisplayName("싱글 연습 게임 통계를 업데이트한다")
    @Test
    void updateSingleGameStatistic_Practice() {
        // given
        memberStatisticService.initializeStatistic(testMember);
        LocalDateTime playTime = LocalDateTime.now();

        // when
        memberStatisticService.updateSingleGameStatistic(testMember, GameType.PRACTICE, 3500.0, playTime);

        // then
        MemberStatistic statistic = memberStatisticAdaptor.queryByMember(testMember);
        assertEquals(1L, statistic.getRoadviewPracticeGames());
        assertEquals(3500.0, statistic.getRoadviewPracticeAvgScore());
        assertEquals(3500.0, statistic.getRoadviewPracticeTotalScore());
        assertEquals(3500.0, statistic.getBestScore());
        assertEquals(1, statistic.getCurrentStreak());
        assertEquals(playTime, statistic.getLastPlayedAt());
    }

    @DisplayName("싱글 랭크 게임 통계를 업데이트한다")
    @Test
    void updateSingleGameStatistic_Rank() {
        // given
        memberStatisticService.initializeStatistic(testMember);
        LocalDateTime playTime = LocalDateTime.now();

        // when
        memberStatisticService.updateSingleGameStatistic(testMember, GameType.RANK, 4200.0, playTime);

        // then
        MemberStatistic statistic = memberStatisticAdaptor.queryByMember(testMember);
        assertEquals(1L, statistic.getRoadviewRankGames());
        assertEquals(4200.0, statistic.getRoadviewRankAvgScore());
        assertEquals(4200.0, statistic.getRoadviewRankTotalScore());
        assertEquals(4200.0, statistic.getBestScore());
    }

    @DisplayName("여러 게임 후 평균 점수가 올바르게 계산된다")
    @Test
    void updateSingleGameStatistic_AverageCalculation() {
        // given
        memberStatisticService.initializeStatistic(testMember);
        LocalDateTime playTime = LocalDateTime.now();

        // when
        memberStatisticService.updateSingleGameStatistic(testMember, GameType.PRACTICE, 3000.0, playTime);
        memberStatisticService.updateSingleGameStatistic(testMember, GameType.PRACTICE, 4000.0, playTime);
        memberStatisticService.updateSingleGameStatistic(testMember, GameType.PRACTICE, 5000.0, playTime);

        // then
        MemberStatistic statistic = memberStatisticAdaptor.queryByMember(testMember);
        assertEquals(3L, statistic.getRoadviewPracticeGames());
        assertEquals(4000.0, statistic.getRoadviewPracticeAvgScore());
        assertEquals(12000.0, statistic.getRoadviewPracticeTotalScore());
    }

    @DisplayName("멀티 게임 통계를 업데이트한다")
    @Test
    void updateMultiGameStatistic() {
        // given
        memberStatisticService.initializeStatistic(testMember);
        LocalDateTime playTime = LocalDateTime.now();

        // when
        memberStatisticService.updateMultiGameStatistic(testMember, 3800.0, 1, playTime);

        // then
        MemberStatistic statistic = memberStatisticAdaptor.queryByMember(testMember);
        assertEquals(1L, statistic.getRoadviewMultiGames());
        assertEquals(3800.0, statistic.getRoadviewMultiAvgScore());
        assertEquals(3800.0, statistic.getRoadviewMultiTotalScore());
        assertEquals(1L, statistic.getRoadviewMultiFirstPlace());
        assertEquals(0L, statistic.getRoadviewMultiSecondPlace());
        assertEquals(0L, statistic.getRoadviewMultiThirdPlace());
    }

    @DisplayName("멀티 게임 순위별 통계가 올바르게 누적된다")
    @Test
    void updateMultiGameStatistic_RankAccumulation() {
        // given
        memberStatisticService.initializeStatistic(testMember);
        LocalDateTime playTime = LocalDateTime.now();

        // when
        memberStatisticService.updateMultiGameStatistic(testMember, 4000.0, 1, playTime);
        memberStatisticService.updateMultiGameStatistic(testMember, 3500.0, 2, playTime);
        memberStatisticService.updateMultiGameStatistic(testMember, 3000.0, 3, playTime);
        memberStatisticService.updateMultiGameStatistic(testMember, 3800.0, 1, playTime);

        // then
        MemberStatistic statistic = memberStatisticAdaptor.queryByMember(testMember);
        assertEquals(4L, statistic.getRoadviewMultiGames());
        assertEquals(2L, statistic.getRoadviewMultiFirstPlace());
        assertEquals(1L, statistic.getRoadviewMultiSecondPlace());
        assertEquals(1L, statistic.getRoadviewMultiThirdPlace());
    }

    @DisplayName("최고 점수가 올바르게 업데이트된다")
    @Test
    void updateBestScore() {
        // given
        memberStatisticService.initializeStatistic(testMember);
        LocalDateTime playTime = LocalDateTime.now();

        // when
        memberStatisticService.updateSingleGameStatistic(testMember, GameType.PRACTICE, 3000.0, playTime);
        memberStatisticService.updateSingleGameStatistic(testMember, GameType.PRACTICE, 4500.0, playTime);
        memberStatisticService.updateSingleGameStatistic(testMember, GameType.PRACTICE, 4000.0, playTime);

        // then
        MemberStatistic statistic = memberStatisticAdaptor.queryByMember(testMember);
        assertEquals(4500.0, statistic.getBestScore());
    }

    @DisplayName("연속 플레이 기록이 올바르게 업데이트된다")
    @Test
    void updateStreak_Consecutive() {
        // given
        memberStatisticService.initializeStatistic(testMember);
        LocalDate today = LocalDate.now();
        LocalDate yesterday = today.minusDays(1);
        LocalDate dayBeforeYesterday = today.minusDays(2);

        // when
        memberStatisticService.updateSingleGameStatistic(testMember, GameType.PRACTICE, 3000.0, 
                dayBeforeYesterday.atTime(10, 0));
        memberStatisticService.updateSingleGameStatistic(testMember, GameType.PRACTICE, 3000.0, 
                yesterday.atTime(10, 0));
        memberStatisticService.updateSingleGameStatistic(testMember, GameType.PRACTICE, 3000.0, 
                today.atTime(10, 0));

        // then
        MemberStatistic statistic = memberStatisticAdaptor.queryByMember(testMember);
        assertEquals(3, statistic.getCurrentStreak());
        assertEquals(3, statistic.getLongestStreak());
    }

    @DisplayName("연속 플레이가 끊기면 리셋된다")
    @Test
    void updateStreak_Reset() {
        // given
        memberStatisticService.initializeStatistic(testMember);
        LocalDate today = LocalDate.now();
        LocalDate fiveDaysAgo = today.minusDays(5);

        // when
        memberStatisticService.updateSingleGameStatistic(testMember, GameType.PRACTICE, 3000.0, 
                fiveDaysAgo.atTime(10, 0));
        memberStatisticService.updateSingleGameStatistic(testMember, GameType.PRACTICE, 3000.0, 
                fiveDaysAgo.plusDays(1).atTime(10, 0));
        memberStatisticService.updateSingleGameStatistic(testMember, GameType.PRACTICE, 3000.0, 
                today.atTime(10, 0));

        // then
        MemberStatistic statistic = memberStatisticAdaptor.queryByMember(testMember);
        assertEquals(1, statistic.getCurrentStreak());
        assertEquals(2, statistic.getLongestStreak());
    }

    @DisplayName("같은 날 여러 게임을 해도 연속 플레이는 1로 카운트된다")
    @Test
    void updateStreak_SameDay() {
        // given
        memberStatisticService.initializeStatistic(testMember);
        LocalDate today = LocalDate.now();

        // when
        memberStatisticService.updateSingleGameStatistic(testMember, GameType.PRACTICE, 3000.0, 
                today.atTime(10, 0));
        memberStatisticService.updateSingleGameStatistic(testMember, GameType.PRACTICE, 3000.0, 
                today.atTime(15, 0));
        memberStatisticService.updateSingleGameStatistic(testMember, GameType.PRACTICE, 3000.0, 
                today.atTime(20, 0));

        // then
        MemberStatistic statistic = memberStatisticAdaptor.queryByMember(testMember);
        assertEquals(1, statistic.getCurrentStreak());
        assertEquals(3L, statistic.getRoadviewPracticeGames());
    }

    @DisplayName("최근 플레이 시간이 올바르게 업데이트된다")
    @Test
    void updateLastPlayedAt() {
        // given
        memberStatisticService.initializeStatistic(testMember);
        LocalDateTime firstPlay = LocalDateTime.now().minusHours(5);
        LocalDateTime secondPlay = LocalDateTime.now().minusHours(2);
        LocalDateTime thirdPlay = LocalDateTime.now();

        // when
        memberStatisticService.updateSingleGameStatistic(testMember, GameType.PRACTICE, 3000.0, firstPlay);
        memberStatisticService.updateSingleGameStatistic(testMember, GameType.PRACTICE, 3000.0, secondPlay);
        memberStatisticService.updateSingleGameStatistic(testMember, GameType.PRACTICE, 3000.0, thirdPlay);

        // then
        MemberStatistic statistic = memberStatisticAdaptor.queryByMember(testMember);
        assertEquals(thirdPlay, statistic.getLastPlayedAt());
    }

    @DisplayName("싱글과 멀티 게임 통계가 독립적으로 관리된다")
    @Test
    void updateStatistic_IndependentTracking() {
        // given
        memberStatisticService.initializeStatistic(testMember);
        LocalDateTime playTime = LocalDateTime.now();

        // when
        memberStatisticService.updateSingleGameStatistic(testMember, GameType.PRACTICE, 3000.0, playTime);
        memberStatisticService.updateSingleGameStatistic(testMember, GameType.RANK, 4000.0, playTime);
        memberStatisticService.updateMultiGameStatistic(testMember, 3500.0, 1, playTime);

        // then
        MemberStatistic statistic = memberStatisticAdaptor.queryByMember(testMember);
        assertEquals(1L, statistic.getRoadviewPracticeGames());
        assertEquals(1L, statistic.getRoadviewRankGames());
        assertEquals(1L, statistic.getRoadviewMultiGames());
        assertEquals(3000.0, statistic.getRoadviewPracticeAvgScore());
        assertEquals(4000.0, statistic.getRoadviewRankAvgScore());
        assertEquals(3500.0, statistic.getRoadviewMultiAvgScore());
    }

    private Member createTestMember(String username, String nickname) {
        Member member = Member.builder()
                .username(username)
                .nickname(nickname)
                .email(username + "@kospot.com")
                .role(Role.USER)
                .point(10000)
                .build();
        return memberRepository.save(member);
    }
}

