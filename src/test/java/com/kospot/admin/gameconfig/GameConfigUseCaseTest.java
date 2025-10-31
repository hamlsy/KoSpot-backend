package com.kospot.admin.gameconfig;

import com.kospot.application.admin.gameconfig.*;
import com.kospot.domain.game.vo.GameMode;
import com.kospot.domain.gameconfig.entity.GameConfig;
import com.kospot.domain.gameconfig.repository.GameConfigRepository;
import com.kospot.domain.member.entity.Member;
import com.kospot.domain.member.repository.MemberRepository;
import com.kospot.domain.member.vo.Role;
import com.kospot.domain.multi.game.vo.PlayerMatchType;
import com.kospot.presentation.admin.dto.request.AdminGameConfigRequest;
import com.kospot.presentation.admin.dto.response.AdminGameConfigResponse;
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
public class GameConfigUseCaseTest {

    @Autowired
    private CreateGameConfigUseCase createGameConfigUseCase;

    @Autowired
    private InitializeAllGameConfigsUseCase initializeAllGameConfigsUseCase;

    @Autowired
    private ActivateGameConfigUseCase activateGameConfigUseCase;

    @Autowired
    private DeactivateGameConfigUseCase deactivateGameConfigUseCase;

    @Autowired
    private FindAllGameConfigsUseCase findAllGameConfigsUseCase;

    @Autowired
    private DeleteGameConfigUseCase deleteGameConfigUseCase;

    @Autowired
    private GameConfigRepository gameConfigRepository;

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

    @DisplayName("게임 설정 생성 - 싱글 모드")
    @Test
    void createGameConfig_SingleMode_Success() {
        // given
        AdminGameConfigRequest.Create request = new AdminGameConfigRequest.Create(
                "ROADVIEW",
                null,
                true
        );

        // when
        Long configId = createGameConfigUseCase.execute(admin, request);

        // then
        GameConfig config = gameConfigRepository.findById(configId).orElseThrow();
        assertEquals(GameMode.ROADVIEW, config.getGameMode());
        assertTrue(config.getIsSingleMode());
        assertNull(config.getPlayerMatchType());
        assertTrue(config.getIsActive());
        log.info("생성된 싱글 모드 설정: {}", config);
    }

    @DisplayName("게임 설정 생성 - 멀티 모드")
    @Test
    void createGameConfig_MultiMode_Success() {
        // given
        AdminGameConfigRequest.Create request = new AdminGameConfigRequest.Create(
                "ROADVIEW",
                "SOLO",
                false
        );

        // when
        Long configId = createGameConfigUseCase.execute(admin, request);

        // then
        GameConfig config = gameConfigRepository.findById(configId).orElseThrow();
        assertEquals(GameMode.ROADVIEW, config.getGameMode());
        assertEquals(PlayerMatchType.SOLO, config.getPlayerMatchType());
        assertFalse(config.getIsSingleMode());
        assertTrue(config.getIsActive());
        log.info("생성된 멀티 모드 설정: {}", config);
    }

    @DisplayName("게임 설정 생성 - 권한 없음")
    @Test
    void createGameConfig_NoPermission_ThrowsException() {
        // given
        AdminGameConfigRequest.Create request = new AdminGameConfigRequest.Create(
                "ROADVIEW",
                null,
                true
        );

        // when & then
        assertThrows(Exception.class, () -> createGameConfigUseCase.execute(user, request));
    }

    @DisplayName("모든 기본 게임 설정 초기화")
    @Test
    void initializeAllGameConfigs_Success() {
        // when
        initializeAllGameConfigsUseCase.execute(admin);

        // then
        List<GameConfig> configs = gameConfigRepository.findAll();
        assertEquals(6, configs.size()); // 싱글 2개 + 멀티 4개

        // 싱글 모드 확인
        assertTrue(configs.stream().anyMatch(c -> 
                c.getGameMode() == GameMode.ROADVIEW && c.getIsSingleMode()));
        assertTrue(configs.stream().anyMatch(c -> 
                c.getGameMode() == GameMode.PHOTO && c.getIsSingleMode()));

        // 멀티 모드 확인
        assertTrue(configs.stream().anyMatch(c -> 
                c.getGameMode() == GameMode.ROADVIEW && 
                c.getPlayerMatchType() == PlayerMatchType.SOLO && 
                !c.getIsSingleMode()));
        assertTrue(configs.stream().anyMatch(c -> 
                c.getGameMode() == GameMode.ROADVIEW && 
                c.getPlayerMatchType() == PlayerMatchType.TEAM && 
                !c.getIsSingleMode()));

        // 모두 활성화 상태인지 확인
        assertTrue(configs.stream().allMatch(GameConfig::getIsActive));

        log.info("생성된 설정 개수: {}", configs.size());
    }

    @DisplayName("모든 기본 게임 설정 초기화 - 중복 생성 방지")
    @Test
    void initializeAllGameConfigs_SkipExisting_Success() {
        // given - 먼저 하나 생성
        gameConfigRepository.save(GameConfig.builder()
                .gameMode(GameMode.ROADVIEW)
                .isSingleMode(true)
                .isActive(true)
                .build());

        // when
        initializeAllGameConfigsUseCase.execute(admin);

        // then
        List<GameConfig> configs = gameConfigRepository.findAll();
        assertEquals(6, configs.size()); // 여전히 6개 (중복 생성 안 됨)
    }

    @DisplayName("게임 설정 활성화")
    @Test
    void activateGameConfig_Success() {
        // given
        GameConfig config = gameConfigRepository.save(
                GameConfig.builder()
                        .gameMode(GameMode.ROADVIEW)
                        .isSingleMode(true)
                        .isActive(false)
                        .build()
        );

        // when
        activateGameConfigUseCase.execute(admin, config.getId());

        // then
        GameConfig updated = gameConfigRepository.findById(config.getId()).orElseThrow();
        assertTrue(updated.getIsActive());
    }

    @DisplayName("게임 설정 비활성화")
    @Test
    void deactivateGameConfig_Success() {
        // given
        GameConfig config = gameConfigRepository.save(
                GameConfig.builder()
                        .gameMode(GameMode.ROADVIEW)
                        .isSingleMode(true)
                        .isActive(true)
                        .build()
        );

        // when
        deactivateGameConfigUseCase.execute(admin, config.getId());

        // then
        GameConfig updated = gameConfigRepository.findById(config.getId()).orElseThrow();
        assertFalse(updated.getIsActive());
    }

    @DisplayName("게임 설정 목록 조회")
    @Test
    void findAllGameConfigs_Success() {
        // given
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
                        .playerMatchType(PlayerMatchType.SOLO)
                        .isSingleMode(false)
                        .isActive(false)
                        .build()
        );

        // when
        List<AdminGameConfigResponse.GameConfigInfo> configs = findAllGameConfigsUseCase.execute(admin);

        // then
        assertEquals(2, configs.size());
        log.info("조회된 설정 목록: {}", configs);
    }

    @DisplayName("게임 설정 삭제")
    @Test
    void deleteGameConfig_Success() {
        // given
        GameConfig config = gameConfigRepository.save(
                GameConfig.builder()
                        .gameMode(GameMode.ROADVIEW)
                        .isSingleMode(true)
                        .isActive(true)
                        .build()
        );

        // when
        deleteGameConfigUseCase.execute(admin, config.getId());

        // then
        assertFalse(gameConfigRepository.findById(config.getId()).isPresent());
    }

    @DisplayName("게임 설정 삭제 - 권한 없음")
    @Test
    void deleteGameConfig_NoPermission_ThrowsException() {
        // given
        GameConfig config = gameConfigRepository.save(
                GameConfig.builder()
                        .gameMode(GameMode.ROADVIEW)
                        .isSingleMode(true)
                        .isActive(true)
                        .build()
        );

        // when & then
        assertThrows(Exception.class, () -> deleteGameConfigUseCase.execute(user, config.getId()));
    }
}

