package com.kospot.admin.banner;

import com.kospot.application.admin.banner.*;
import com.kospot.domain.banner.entity.Banner;
import com.kospot.domain.banner.repository.BannerRepository;
import com.kospot.domain.image.entity.Image;
import com.kospot.domain.image.repository.ImageRepository;
import com.kospot.domain.image.vo.ImageType;
import com.kospot.domain.member.entity.Member;
import com.kospot.domain.member.repository.MemberRepository;
import com.kospot.domain.member.vo.Role;
import com.kospot.presentation.admin.dto.response.AdminBannerResponse;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@Slf4j
@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class BannerUseCaseTest {

    @Autowired
    private ActivateBannerUseCase activateBannerUseCase;

    @Autowired
    private DeactivateBannerUseCase deactivateBannerUseCase;

    @Autowired
    private FindAllBannersUseCase findAllBannersUseCase;

    @Autowired
    private DeleteBannerUseCase deleteBannerUseCase;

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

    @DisplayName("배너 활성화 테스트")
    @Test
    void activateBanner_Success() {
        // given
        Image image = createTestImage();
        Banner banner = bannerRepository.save(
                Banner.builder()
                        .title("테스트 배너")
                        .image(image)
                        .linkUrl("https://kospot.com")
                        .description("테스트 설명")
                        .displayOrder(1)
                        .isActive(false)
                        .build()
        );

        // when
        activateBannerUseCase.execute(admin, banner.getId());

        // then
        Banner updated = bannerRepository.findById(banner.getId()).orElseThrow();
        assertTrue(updated.getIsActive());
    }

    @DisplayName("배너 비활성화 테스트")
    @Test
    void deactivateBanner_Success() {
        // given
        Image image = createTestImage();
        Banner banner = bannerRepository.save(
                Banner.builder()
                        .title("테스트 배너")
                        .image(image)
                        .linkUrl("https://kospot.com")
                        .description("테스트 설명")
                        .displayOrder(1)
                        .isActive(true)
                        .build()
        );

        // when
        deactivateBannerUseCase.execute(admin, banner.getId());

        // then
        Banner updated = bannerRepository.findById(banner.getId()).orElseThrow();
        assertFalse(updated.getIsActive());
    }

    @DisplayName("배너 목록 조회 테스트")
    @Test
    void findAllBanners_Success() {
        // given
        Image image1 = createTestImage();
        Image image2 = createTestImage();

        bannerRepository.save(
                Banner.builder()
                        .title("배너1")
                        .image(image1)
                        .linkUrl("https://kospot.com")
                        .displayOrder(1)
                        .isActive(true)
                        .build()
        );
        bannerRepository.save(
                Banner.builder()
                        .title("배너2")
                        .image(image2)
                        .linkUrl("https://kospot.com")
                        .displayOrder(2)
                        .isActive(false)
                        .build()
        );

        // when
        List<AdminBannerResponse.BannerInfo> banners = findAllBannersUseCase.execute(admin);

        // then
        assertEquals(2, banners.size());
        log.info("조회된 배너 목록: {}", banners);
    }

    @DisplayName("배너 삭제 테스트")
    @Test
    void deleteBanner_Success() {
        // given
        Image image = createTestImage();
        Banner banner = bannerRepository.save(
                Banner.builder()
                        .title("삭제할 배너")
                        .image(image)
                        .linkUrl("https://kospot.com")
                        .displayOrder(1)
                        .isActive(true)
                        .build()
        );

        Long imageId = image.getId();

        // when
        deleteBannerUseCase.execute(admin, banner.getId());

        // then
        assertFalse(bannerRepository.findById(banner.getId()).isPresent());
        // Image도 cascade로 함께 삭제되어야 함
        assertFalse(imageRepository.findById(imageId).isPresent());
    }

    @DisplayName("배너 활성화 - 권한 없음")
    @Test
    void activateBanner_NoPermission_ThrowsException() {
        // given
        Image image = createTestImage();
        Banner banner = bannerRepository.save(
                Banner.builder()
                        .title("테스트 배너")
                        .image(image)
                        .linkUrl("https://kospot.com")
                        .displayOrder(1)
                        .isActive(false)
                        .build()
        );

        // when & then
        assertThrows(Exception.class, () -> activateBannerUseCase.execute(user, banner.getId()));
    }

    @DisplayName("배너 삭제 - 권한 없음")
    @Test
    void deleteBanner_NoPermission_ThrowsException() {
        // given
        Image image = createTestImage();
        Banner banner = bannerRepository.save(
                Banner.builder()
                        .title("테스트 배너")
                        .image(image)
                        .linkUrl("https://kospot.com")
                        .displayOrder(1)
                        .isActive(true)
                        .build()
        );

        // when & then
        assertThrows(Exception.class, () -> deleteBannerUseCase.execute(user, banner.getId()));
    }

    @DisplayName("배너 displayOrder 정렬 테스트")
    @Test
    void findAllBanners_OrderedByDisplayOrder_Success() {
        // given
        Image image1 = createTestImage();
        Image image2 = createTestImage();
        Image image3 = createTestImage();

        bannerRepository.save(
                Banner.builder()
                        .title("배너3")
                        .image(image3)
                        .linkUrl("https://kospot.com")
                        .displayOrder(3)
                        .isActive(true)
                        .build()
        );
        bannerRepository.save(
                Banner.builder()
                        .title("배너1")
                        .image(image1)
                        .linkUrl("https://kospot.com")
                        .displayOrder(1)
                        .isActive(true)
                        .build()
        );
        bannerRepository.save(
                Banner.builder()
                        .title("배너2")
                        .image(image2)
                        .linkUrl("https://kospot.com")
                        .displayOrder(2)
                        .isActive(true)
                        .build()
        );

        // when
        List<AdminBannerResponse.BannerInfo> banners = findAllBannersUseCase.execute(admin);

        // then
        assertEquals(3, banners.size());
        assertEquals("배너1", banners.get(0).getTitle());
        assertEquals("배너2", banners.get(1).getTitle());
        assertEquals("배너3", banners.get(2).getTitle());
    }

    private Image createTestImage() {
        return imageRepository.save(
                Image.builder()
                        .imageUrl("https://test-s3.amazonaws.com/test-image.jpg")
                        .s3Key("banner/test-image.jpg")
                        .imageType(ImageType.BANNER)
                        .build()
        );
    }
}

