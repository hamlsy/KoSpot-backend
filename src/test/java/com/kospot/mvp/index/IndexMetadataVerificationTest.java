package com.kospot.mvp.index;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
class IndexMetadataVerificationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("MVP 관련 신규 인덱스가 생성된다")
    void shouldCreateMvpRelatedIndexes() {
        List<String> roadViewIndexes = findIndexes("road_view_game");
        List<String> dailyMvpIndexes = findIndexes("daily_mvp");
        List<String> gameRankIndexes = findIndexes("game_rank");

        assertTrue(roadViewIndexes.contains("idx_road_view_game_mvp"));
        assertTrue(roadViewIndexes.contains("idx_road_view_game_member_status_created"));

        assertTrue(dailyMvpIndexes.contains("idx_daily_mvp_member_id"));
        assertTrue(dailyMvpIndexes.contains("idx_daily_mvp_road_view_game_id"));
        assertTrue(dailyMvpIndexes.contains("idx_daily_mvp_reward_date"));

        assertTrue(gameRankIndexes.contains("idx_game_rank_member_mode"));
        assertTrue(gameRankIndexes.contains("idx_game_rank_mode_tier_rating"));
    }

    private List<String> findIndexes(String tableName) {
        return jdbcTemplate.queryForList(
                "SELECT DISTINCT INDEX_NAME FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ?",
                String.class,
                tableName
        );
    }
}
