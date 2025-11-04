package com.kospot.member.usecase;

import com.kospot.application.member.GetMemberProfileUseCase;
import com.kospot.domain.game.vo.GameMode;
import com.kospot.domain.gamerank.entity.GameRank;
import com.kospot.domain.gamerank.repository.GameRankRepository;
import com.kospot.domain.gamerank.vo.RankLevel;
import com.kospot.domain.gamerank.vo.RankTier;
import com.kospot.domain.member.entity.Member;
import com.kospot.domain.member.entity.MemberStatistic;
import com.kospot.domain.member.repository.MemberRepository;
import com.kospot.domain.member.repository.MemberStatisticRepository;
import com.kospot.domain.member.vo.Role;
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

    private Member testMember;
    private MemberStatistic testStatistic;

    @BeforeEach
    void setUp() {
        testMember = createTestMember("testUser", "testNickname");
        testStatistic = createTestStatistic(testMember);
        createTestGameRank(testMember);
        createTestGameRankForPhoto(testMember);
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
        
        // 로드뷰 모드 - 연습
        assertEquals(10L, statistics.getRoadView().getPractice().getTotalGames());
        assertEquals(3500.5, statistics.getRoadView().getPractice().getAverageScore());
        
        // 로드뷰 모드 - 랭크
        assertEquals(20L, statistics.getRoadView().getRank().getTotalGames());
        assertEquals(4200.8, statistics.getRoadView().getRank().getAverageScore());
        
        // 로드뷰 모드 - 멀티
        assertEquals(15L, statistics.getRoadView().getMulti().getTotalGames());
        assertEquals(3800.0, statistics.getRoadView().getMulti().getAverageScore());
        assertEquals(5L, statistics.getRoadView().getMulti().getFirstPlaceCount());
        assertEquals(7L, statistics.getRoadView().getMulti().getSecondPlaceCount());
        assertEquals(3L, statistics.getRoadView().getMulti().getThirdPlaceCount());
        
        // 포토 모드는 0으로 초기화
        assertEquals(0L, statistics.getPhoto().getPractice().getTotalGames());
        assertEquals(0L, statistics.getPhoto().getRank().getTotalGames());
        assertEquals(0L, statistics.getPhoto().getMulti().getTotalGames());
        
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
        
        // 포토 랭크도 확인
        assertNotNull(rankInfo.getPhotoRank());
    }

    @DisplayName("프로필 이미지 URL이 올바르게 조회된다")
    @Test
    void getMemberProfile_ProfileImageUrl() {
        // when
        MemberProfileResponse response = getMemberProfileUseCase.execute(testMember);

        // then
        assertNotNull(response);
        assertNull(response.getProfileImageUrl());
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
        createTestGameRankForPhoto(newMember);

        // when
        MemberProfileResponse response = getMemberProfileUseCase.execute(newMember);

        // then
        assertNotNull(response);
        assertEquals(0L, response.getStatistics().getRoadView().getPractice().getTotalGames());
        assertEquals(0.0, response.getStatistics().getRoadView().getPractice().getAverageScore());
        assertEquals(0L, response.getStatistics().getPhoto().getPractice().getTotalGames());
        assertEquals(0.0, response.getStatistics().getPhoto().getPractice().getAverageScore());
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
                .roadviewPracticeGames(10L)
                .roadviewPracticeAvgScore(3500.5)
                .roadviewPracticeTotalScore(35005.0)
                .roadviewRankGames(20L)
                .roadviewRankAvgScore(4200.8)
                .roadviewRankTotalScore(84016.0)
                .roadviewMultiGames(15L)
                .roadviewMultiAvgScore(3800.0)
                .roadviewMultiTotalScore(57000.0)
                .roadviewMultiFirstPlace(5L)
                .roadviewMultiSecondPlace(7L)
                .roadviewMultiThirdPlace(3L)
                .photoPracticeGames(0L)
                .photoPracticeAvgScore(0.0)
                .photoPracticeTotalScore(0.0)
                .photoRankGames(0L)
                .photoRankAvgScore(0.0)
                .photoRankTotalScore(0.0)
                .photoMultiGames(0L)
                .photoMultiAvgScore(0.0)
                .photoMultiTotalScore(0.0)
                .photoMultiFirstPlace(0L)
                .photoMultiSecondPlace(0L)
                .photoMultiThirdPlace(0L)
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

    private GameRank createTestGameRankForPhoto(Member member) {
        GameRank gameRank = GameRank.builder()
                .member(member)
                .gameMode(GameMode.PHOTO)
                .ratingScore(1000)
                .rankTier(RankTier.BRONZE)
                .rankLevel(RankLevel.ONE)
                .build();
        return gameRankRepository.save(gameRank);
    }

}

