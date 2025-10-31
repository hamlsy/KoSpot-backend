package com.kospot.main;

import com.kospot.application.main.FindMainPageInfoUseCase;
import com.kospot.domain.banner.entity.Banner;
import com.kospot.domain.banner.repository.BannerRepository;
import com.kospot.domain.game.vo.GameMode;
import com.kospot.domain.gameconfig.entity.GameConfig;
import com.kospot.domain.gameconfig.repository.GameConfigRepository;
import com.kospot.domain.image.entity.Image;
import com.kospot.domain.image.repository.ImageRepository;
import com.kospot.domain.image.vo.ImageType;
import com.kospot.domain.member.entity.Member;
import com.kospot.domain.member.repository.MemberRepository;
import com.kospot.domain.member.vo.Role;
import com.kospot.domain.multi.game.vo.PlayerMatchType;
import com.kospot.domain.notice.entity.Notice;
import com.kospot.domain.notice.repository.NoticeRepository;
import com.kospot.presentation.main.dto.response.MainPageResponse;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

@Slf4j
@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class MainPageUseCaseTest {

    @Autowired
    private FindMainPageInfoUseCase findMainPageInfoUseCase;

    @Autowired
    private GameConfigRepository gameConfigRepository;

    @Autowired
    private NoticeRepository noticeRepository;

    @Autowired
    private BannerRepository bannerRepository;

    @Autowired
    private ImageRepository imageRepository;

    @Autowired
    private MemberRepository memberRepository;

    private Member admin;
    private Member user;

    @BeforeEach
    void setUp() {
        this.admin = memberRepository.save(
                Member.builder()
                        .username("admin")
                        .nickname("관리자")
                        .role(Role.ADMIN)
                        .build()
        );
        this.user = memberRepository.save(
                Member.builder()
                        .username("user")
                        .nickname("사용자")
                        .role(Role.USER)
                        .build()
        );
    }

    @DisplayName("메인 페이지 정보 조회 - GameConfig 없을 때 기본값 true")
    @Test
    void findMainPageInfo_NoGameConfig_DefaultTrue() {
        // given - GameConfig 없음, 공지사항과 배너만 생성
        createTestNotices(3);
        createTestBanners(2);

        // when
        MainPageResponse.MainPageInfo mainPageInfo = findMainPageInfoUseCase.execute(user);

        // then
        assertNotNull(mainPageInfo);
        assertFalse(mainPageInfo.getIsAdmin()); // 일반 사용자

        // GameConfig가 없으면 기본값으로 모두 true
        MainPageResponse.GameModeStatus gameModeStatus = mainPageInfo.getGameModeStatus();
        assertTrue(gameModeStatus.getRoadviewEnabled());
        assertTrue(gameModeStatus.getPhotoEnabled());
        assertTrue(gameModeStatus.getMultiplayEnabled());

        assertEquals(3, mainPageInfo.getRecentNotices().size());
        assertEquals(2, mainPageInfo.getBanners().size());
        log.info("메인 페이지 정보 (GameConfig 없음): {}", mainPageInfo);
    }

    @DisplayName("메인 페이지 정보 조회 - 관리자")
    @Test
    void findMainPageInfo_Admin_Success() {
        // given
        createAllGameConfigs();
        createTestNotices(5);
        createTestBanners(3);

        // when
        MainPageResponse.MainPageInfo mainPageInfo = findMainPageInfoUseCase.execute(admin);

        // then
        assertNotNull(mainPageInfo);
        assertTrue(mainPageInfo.getIsAdmin()); // 관리자

        MainPageResponse.GameModeStatus gameModeStatus = mainPageInfo.getGameModeStatus();
        assertTrue(gameModeStatus.getRoadviewEnabled());
        assertTrue(gameModeStatus.getPhotoEnabled());
        assertTrue(gameModeStatus.getMultiplayEnabled());

        assertEquals(3, mainPageInfo.getRecentNotices().size()); // 최대 3개만
        assertEquals(3, mainPageInfo.getBanners().size());
        log.info("메인 페이지 정보 (관리자): {}", mainPageInfo);
    }

    @DisplayName("메인 페이지 정보 조회 - 로드뷰만 활성화")
    @Test
    void findMainPageInfo_OnlyRoadviewEnabled_Success() {
        // given - 로드뷰만 활성화
        gameConfigRepository.save(
                GameConfig.builder()
                        .gameMode(GameMode.ROADVIEW)
                        .isSingleMode(true)
                        .isActive(true)
                        .build()
        );
        gameConfigRepository.save(
                GameConfig.builder()
                        .gameMode(GameMode.PHOTO)
                        .isSingleMode(true)
                        .isActive(false) // 비활성화
                        .build()
        );

        createTestNotices(2);
        createTestBanners(1);

        // when
        MainPageResponse.MainPageInfo mainPageInfo = findMainPageInfoUseCase.execute(user);

        // then
        MainPageResponse.GameModeStatus gameModeStatus = mainPageInfo.getGameModeStatus();
        assertTrue(gameModeStatus.getRoadviewEnabled());
        assertFalse(gameModeStatus.getPhotoEnabled());
        assertFalse(gameModeStatus.getMultiplayEnabled());
        log.info("메인 페이지 정보 (로드뷰만 활성화): {}", mainPageInfo);
    }

    @DisplayName("메인 페이지 정보 조회 - 멀티플레이만 활성화")
    @Test
    void findMainPageInfo_OnlyMultiplayEnabled_Success() {
        // given - 멀티플레이만 활성화
        gameConfigRepository.save(
                GameConfig.builder()
                        .gameMode(GameMode.ROADVIEW)
                        .playerMatchType(PlayerMatchType.SOLO)
                        .isSingleMode(false)
                        .isActive(true)
                        .build()
        );
        gameConfigRepository.save(
                GameConfig.builder()
                        .gameMode(GameMode.ROADVIEW)
                        .isSingleMode(true)
                        .isActive(false) // 싱글 비활성화
                        .build()
        );

        createTestNotices(1);
        createTestBanners(1);

        // when
        MainPageResponse.MainPageInfo mainPageInfo = findMainPageInfoUseCase.execute(user);

        // then
        MainPageResponse.GameModeStatus gameModeStatus = mainPageInfo.getGameModeStatus();
        assertTrue(gameModeStatus.getRoadviewEnabled()); // 멀티 로드뷰 활성화
        assertFalse(gameModeStatus.getPhotoEnabled());
        assertTrue(gameModeStatus.getMultiplayEnabled());
        log.info("메인 페이지 정보 (멀티플레이만 활성화): {}", mainPageInfo);
    }

    @DisplayName("메인 페이지 정보 조회 - 최근 공지사항 3개 제한")
    @Test
    void findMainPageInfo_RecentNoticesLimit_Success() {
        // given - 공지사항 10개 생성
        createTestNotices(10);
        createTestBanners(1);
        createAllGameConfigs();

        // when
        MainPageResponse.MainPageInfo mainPageInfo = findMainPageInfoUseCase.execute(user);

        // then
        assertEquals(3, mainPageInfo.getRecentNotices().size()); // 최대 3개만 조회
        // 최신 순으로 정렬되어야 함
        assertEquals("공지사항9", mainPageInfo.getRecentNotices().get(0).getTitle());
        assertEquals("공지사항8", mainPageInfo.getRecentNotices().get(1).getTitle());
        assertEquals("공지사항7", mainPageInfo.getRecentNotices().get(2).getTitle());
    }

    @DisplayName("메인 페이지 정보 조회 - 비활성화된 배너 제외")
    @Test
    void findMainPageInfo_ExcludeInactiveBanners_Success() {
        // given
        createAllGameConfigs();
        createTestNotices(2);

        // 활성화된 배너 2개
        createTestBanner("활성 배너1", 1, true);
        createTestBanner("활성 배너2", 2, true);
        // 비활성화된 배너 2개
        createTestBanner("비활성 배너1", 3, false);
        createTestBanner("비활성 배너2", 4, false);

        // when
        MainPageResponse.MainPageInfo mainPageInfo = findMainPageInfoUseCase.execute(user);

        // then
        assertEquals(2, mainPageInfo.getBanners().size()); // 활성화된 배너만 조회
        assertTrue(mainPageInfo.getBanners().stream()
                .allMatch(banner -> banner.getTitle().startsWith("활성")));
    }

    @DisplayName("메인 페이지 정보 조회 - 모든 모드 비활성화")
    @Test
    void findMainPageInfo_AllModesDisabled_Success() {
        // given - 모든 모드 비활성화
        gameConfigRepository.save(
                GameConfig.builder()
                        .gameMode(GameMode.ROADVIEW)
                        .isSingleMode(true)
                        .isActive(false)
                        .build()
        );
        gameConfigRepository.save(
                GameConfig.builder()
                        .gameMode(GameMode.PHOTO)
                        .isSingleMode(true)
                        .isActive(false)
                        .build()
        );

        createTestNotices(1);
        createTestBanners(1);

        // when
        MainPageResponse.MainPageInfo mainPageInfo = findMainPageInfoUseCase.execute(user);

        // then
        MainPageResponse.GameModeStatus gameModeStatus = mainPageInfo.getGameModeStatus();
        assertFalse(gameModeStatus.getRoadviewEnabled());
        assertFalse(gameModeStatus.getPhotoEnabled());
        assertFalse(gameModeStatus.getMultiplayEnabled());
        log.info("메인 페이지 정보 (모든 모드 비활성화): {}", mainPageInfo);
    }

    // 헬퍼 메서드들
    private void createAllGameConfigs() {
        gameConfigRepository.save(
                GameConfig.builder()
                        .gameMode(GameMode.ROADVIEW)
                        .isSingleMode(true)
                        .isActive(true)
                        .build()
        );
        gameConfigRepository.save(
                GameConfig.builder()
                        .gameMode(GameMode.PHOTO)
                        .isSingleMode(true)
                        .isActive(true)
                        .build()
        );
        gameConfigRepository.save(
                GameConfig.builder()
                        .gameMode(GameMode.ROADVIEW)
                        .playerMatchType(PlayerMatchType.SOLO)
                        .isSingleMode(false)
                        .isActive(true)
                        .build()
        );
    }

    private void createTestNotices(int count) {
        for (int i = 0; i < count; i++) {
            noticeRepository.save(
                    Notice.builder()
                            .title("공지사항" + i)
                            .content("내용" + i)
                            .build()
            );
        }
    }

    private void createTestBanners(int count) {
        for (int i = 1; i <= count; i++) {
            createTestBanner("배너" + i, i, true);
        }
    }

    private void createTestBanner(String title, int displayOrder, boolean isActive) {
        Image image = imageRepository.save(
                Image.builder()
                        .imageUrl("https://test-s3.amazonaws.com/" + title + ".jpg")
                        .s3Key("banner/" + title + ".jpg")
                        .imageType(ImageType.BANNER)
                        .build()
        );

        bannerRepository.save(
                Banner.builder()
                        .title(title)
                        .image(image)
                        .linkUrl("https://kospot.com")
                        .description(title + " 설명")
                        .displayOrder(displayOrder)
                        .isActive(isActive)
                        .build()
        );
    }
}

