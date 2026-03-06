package com.kospot.point.usecase;

import com.kospot.point.application.usecase.FindAllPointHistoryPagingUseCase;
import com.kospot.member.domain.entity.Member;
import com.kospot.member.infrastructure.persistence.MemberRepository;
import com.kospot.point.domain.entity.PointHistory;
import com.kospot.point.domain.vo.PointHistoryType;
import com.kospot.point.infrastructure.persistence.PointHistoryRepository;
import com.kospot.point.presentation.response.PointHistoryResponse;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Slf4j
@SpringBootTest
@ActiveProfiles("test")
public class PointHistoryUseCaseTest {

    @Autowired
    private PointHistoryRepository pointHistoryRepository;

    @Autowired
    private MemberRepository memberRepository;

    //use case
    @Autowired
    private FindAllPointHistoryPagingUseCase findAllPointHistoryPaging;

    private Member member;

    @BeforeEach
    void setUp() {
        member = memberRepository.save(
                Member.builder()
                        .username("member1")
                        .nickname("nick1")
                        .build()
        );
        for (int i = 0; i < 30; i++) {
            PointHistory pointHistory = PointHistory.builder()
                    .changeAmount(i)
                    .member(member)
                    .pointHistoryType(PointHistoryType.ITEM_PURCHASE)
                    .build();
            pointHistoryRepository.save(pointHistory);
        }
    }

    @DisplayName("포인트 기록 조회를 테스트합니다.")
    @Test
    void findAllPointHistoryPagingTest() {
        //given

        //when
        List<PointHistoryResponse> responses = findAllPointHistoryPaging.execute(member, 0);

        //then
        assertEquals(10, responses.size());
        log.info("responses: {}", responses);
    }

}
