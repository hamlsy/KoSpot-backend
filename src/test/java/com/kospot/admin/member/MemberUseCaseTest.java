package com.kospot.admin.member;

import com.kospot.application.admin.member.FindAllMembersUseCase;
import com.kospot.application.admin.member.FindMemberDetailUseCase;
import com.kospot.domain.member.entity.Member;
import com.kospot.domain.member.entity.MemberStatistic;
import com.kospot.domain.member.repository.MemberRepository;
import com.kospot.domain.member.repository.MemberStatisticRepository;
import com.kospot.domain.member.vo.Role;
import com.kospot.presentation.admin.dto.response.AdminMemberResponse;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

@Slf4j
@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class MemberUseCaseTest {

    @Autowired
    private FindAllMembersUseCase findAllMembersUseCase;

    @Autowired
    private FindMemberDetailUseCase findMemberDetailUseCase;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private MemberStatisticRepository memberStatisticRepository;

    private Member admin;

    @BeforeEach
    void setUp() {
        this.admin = memberRepository.save(
                Member.builder()
                        .username("admin")
                        .nickname("관리자")
                        .role(Role.ADMIN)
                        .build()
        );
    }

    @DisplayName("회원 목록 조회 - 페이징 테스트")
    @Test
    void findAllMembers_WithPaging_Success() {
        // given
        for (int i = 1; i <= 25; i++) {
            Member member = memberRepository.save(
                    Member.builder()
                            .username("user" + i)
                            .nickname("사용자" + i)
                            .role(Role.USER)
                            .build()
            );

            // 각 회원에 대한 통계 생성
            memberStatisticRepository.save(
                    MemberStatistic.builder()
                            .member(member)
                            .singlePracticeGames((long) (10 * i))
                            .singleRankGames((long) (5 * i))
                            .multiGames((long) (3 * i))
                            .build()
            );
        }

        Pageable pageable = PageRequest.of(0, 10);

        // when
        Page<AdminMemberResponse.MemberInfo> membersPage = findAllMembersUseCase.execute(admin, pageable, null);

        // then
        assertEquals(10, membersPage.getContent().size());
        log.info("조회된 회원 수: {}", membersPage.getContent().size());
        log.info("첫 번째 회원: {}", membersPage.getContent().get(0));
    }

    @DisplayName("회원 상세 정보 조회 테스트")
    @Test
    void findMemberDetail_Success() {
        // given
        Member member = memberRepository.save(
                Member.builder()
                        .username("testuser")
                        .nickname("테스트유저")
                        .role(Role.USER)
                        .build()
        );

        memberStatisticRepository.save(
                MemberStatistic.builder()
                        .member(member)
                        .singlePracticeGames(20L)
                        .singlePracticeAvgScore(80.5)
                        .singleRankGames(15L)
                        .singleRankAvgScore(75.3)
                        .multiGames(15L)
                        .multiAvgScore(70.2)
                        .multiFirstPlace(5L)
                        .multiSecondPlace(5L)
                        .multiThirdPlace(5L)
                        .bestScore(95.5)
                        .currentStreak(3)
                        .longestStreak(7)
                        .build()
        );

        // when
        AdminMemberResponse.MemberDetail detail = findMemberDetailUseCase.execute(admin, member.getId());

        // then
        assertNotNull(detail);
        assertEquals("testuser", detail.getUsername());
        assertEquals("테스트유저", detail.getNickname());
        assertEquals(Role.USER, detail.getRole());
        assertEquals(20L, detail.getSinglePracticeGames());
        assertEquals(80.5, detail.getSinglePracticeAvgScore());
        assertEquals(15L, detail.getSingleRankGames());
        assertEquals(75.3, detail.getSingleRankAvgScore());
        assertEquals(15L, detail.getMultiGames());
        assertEquals(70.2, detail.getMultiAvgScore());
        assertEquals(5L, detail.getMultiFirstPlace());
        assertEquals(5L, detail.getMultiSecondPlace());
        assertEquals(5L, detail.getMultiThirdPlace());
        assertEquals(95.5, detail.getBestScore());
        assertEquals(3, detail.getCurrentStreak());
        assertEquals(7, detail.getLongestStreak());
        log.info("회원 상세 정보: {}", detail);
    }

    @DisplayName("회원 목록 조회 - 권한 없음")
    @Test
    void findAllMembers_NoPermission_ThrowsException() {
        // given
        Member user = memberRepository.save(
                Member.builder()
                        .username("user")
                        .nickname("사용자")
                        .role(Role.USER)
                        .build()
        );

        Pageable pageable = PageRequest.of(0, 10);

        // when & then
        assertThrows(Exception.class, () -> findAllMembersUseCase.execute(user, pageable, null));
    }

    @DisplayName("회원 상세 조회 - 권한 없음")
    @Test
    void findMemberDetail_NoPermission_ThrowsException() {
        // given
        Member user = memberRepository.save(
                Member.builder()
                        .username("user")
                        .nickname("사용자")
                        .role(Role.USER)
                        .build()
        );

        Member targetMember = memberRepository.save(
                Member.builder()
                        .username("target")
                        .nickname("대상")
                        .role(Role.USER)
                        .build()
        );

        // when & then
        assertThrows(Exception.class, () -> findMemberDetailUseCase.execute(user, targetMember.getId()));
    }

    @DisplayName("회원 목록 조회 - 다양한 역할 테스트")
    @Test
    void findAllMembers_VariousRoles_Success() {
        // given
        memberRepository.save(
                Member.builder()
                        .username("admin2")
                        .nickname("관리자2")
                        .role(Role.ADMIN)
                        .build()
        );
        memberRepository.save(
                Member.builder()
                        .username("user1")
                        .nickname("사용자1")
                        .role(Role.USER)
                        .build()
        );
        memberRepository.save(
                Member.builder()
                        .username("user2")
                        .nickname("사용자2")
                        .role(Role.USER)
                        .build()
        );

        Pageable pageable = PageRequest.of(0, 10);

        // when
        Page<AdminMemberResponse.MemberInfo> membersPage = findAllMembersUseCase.execute(admin, pageable, null);

        // then
        assertTrue(membersPage.getContent().size() >= 3);
        assertTrue(membersPage.getContent().stream().anyMatch(m -> m.getRole() == Role.ADMIN));
        assertTrue(membersPage.getContent().stream().anyMatch(m -> m.getRole() == Role.USER));
    }
}

