package com.kospot.kospot.notice.usecase;

import com.kospot.kospot.application.notice.*;
import com.kospot.kospot.domain.member.entity.Member;
import com.kospot.kospot.domain.member.entity.Role;
import com.kospot.kospot.domain.member.repository.MemberRepository;
import com.kospot.kospot.domain.notice.entity.Notice;
import com.kospot.kospot.domain.notice.repository.NoticeRepository;
import com.kospot.kospot.presentation.notice.dto.request.NoticeRequest;
import com.kospot.kospot.presentation.notice.dto.response.NoticeResponse;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Slf4j
@SpringBootTest
@ActiveProfiles("test")
public class NoticeUseCaseTest {

    @Autowired
    private CreateNoticeUseCase createNoticeUseCase;

    @Autowired
    private FindAllNoticePagingUseCase findAllNoticePagingUseCase;

    @Autowired
    private FindDetailNoticeUseCase findDetailNoticeUseCase;

    @Autowired
    private DeleteNoticeUseCase deleteNoticeUseCase;

    @Autowired
    private UpdateNoticeUseCase updateNoticeUseCase;

    //repository
    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private NoticeRepository noticeRepository;

    private Member member;
    private Member admin;

    @BeforeEach
    void setUp() {
        this.admin = memberRepository.save(
                Member.builder()
                        .username("admin")
                        .nickname("admin")
                        .role(Role.ADMIN)
                        .build()
        );
        this.member = memberRepository.save(
                Member.builder()
                        .username("member")
                        .nickname("member")
                        .role(Role.USER)
                        .build()
        );

    }

    @DisplayName("공지사항 생성을 테스트합니다.")
    @Test
    void CreateNoticeUseCaseTest() {
        //given
        NoticeRequest.Create request = NoticeRequest.Create.builder()
                .title("title1")
                .content("content1")
                .build();
        //when
        createNoticeUseCase.execute(admin, request);

        //then
        Notice notice = noticeRepository.findById(1L).orElseThrow();
        assertEquals(request.getTitle(), notice.getTitle());
        assertEquals(request.getContent(), notice.getContent());
        assertThrows(Exception.class, () -> createNoticeUseCase.execute(member, request));

    }

    @DisplayName("공지사항 전체 조회를 테스트합니다.")
    @Test
    void findAllNoticePagingUseCaseTest() {
        //given
        for (int i = 0; i < 30; i++) {
            noticeRepository.save(
                    Notice.builder()
                            .title("title" + i)
                            .build()
            );
        }

        //when
        List<NoticeResponse.Summary> responses = findAllNoticePagingUseCase.execute(0);

        //then
        assertEquals(10, responses.size());
        assertEquals("title29", responses.get(0).getTitle());
        log.info("reponses: {}", responses);

    }

    @DisplayName("공지사항 단일 조회를 테스트합니다.")
    @Test
    void findDetailNoticeUseCaseTest() {
        //given
        Notice notice = createTempNotice();

        //when
        NoticeResponse.Detail response = findDetailNoticeUseCase.execute(notice.getId());

        //then
        assertEquals(notice.getTitle(), response.getTitle());
        assertEquals(notice.getContent(), response.getContent());
        log.info("respose: {}", response);

    }

    @DisplayName("공지사항 삭제를 테스트합니다.")
    @Test
    void deleteNoticeUseCaseTest() {
        //given
        Notice notice = createTempNotice();

        //when
        deleteNoticeUseCase.execute(admin, notice.getId());

        //then
        assertThrows(Exception.class, () -> deleteNoticeUseCase.execute(member, notice.getId()));
        assertThrows(Exception.class, () -> noticeRepository.findById(notice.getId()).orElseThrow());

    }

    @DisplayName("공지사항 수정을 테스트합니다.")
    @Test
    void updateNoticeUseCaseTest() {
        //given
        NoticeRequest.Update request = NoticeRequest.Update.builder()
                .title("update")
                .content("update")
                .build();

        Notice notice = createTempNotice();

        //when
        updateNoticeUseCase.execute(admin, notice.getId(), request);

        //then
        assertThrows(Exception.class, () -> updateNoticeUseCase.execute(member, notice.getId(), request));
        Notice updatedNotice = noticeRepository.findById(notice.getId()).orElseThrow();
        assertEquals(request.getTitle(), updatedNotice.getTitle());
        assertEquals(request.getContent(), updatedNotice.getContent());

    }


    private Notice createTempNotice() {
        return noticeRepository.save(
                Notice.builder()
                        .title("title")
                        .content("content")
                        .build()
        );
    }
}
