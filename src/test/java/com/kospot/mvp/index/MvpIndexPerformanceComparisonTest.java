package com.kospot.mvp.index;

import com.kospot.member.domain.entity.Member;
import com.kospot.member.infrastructure.persistence.MemberRepository;
import com.kospot.member.domain.vo.Role;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
class MvpIndexPerformanceComparisonTest {

    private static final int ROW_COUNT = 12000;
    private static final int ITERATIONS = 30;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private MemberRepository memberRepository;

    @Test
    @DisplayName("MVP 후보 조회 인덱스 적용 전/후 성능 비교")
    void compareBeforeAfterIndexPerformance() {
        Member member = memberRepository.save(Member.builder()
                .username("mvp-index-user-" + System.nanoTime())
                .nickname("mvp-index-nick-" + System.nanoTime())
                .role(Role.USER)
                .build());

        insertRoadViewGames(member.getId());

        String baselineQuery = """
                SELECT rg.id
                FROM road_view_game rg IGNORE INDEX (idx_road_view_game_mvp)
                WHERE rg.game_mode = 'RANK'
                  AND rg.game_status = 'COMPLETED'
                  AND rg.ended_at >= ?
                  AND rg.ended_at < ?
                ORDER BY rg.score DESC, rg.ended_at ASC, rg.id ASC
                LIMIT 1
                """;

        String optimizedQuery = """
                SELECT rg.id
                FROM road_view_game rg FORCE INDEX (idx_road_view_game_mvp)
                WHERE rg.game_mode = 'RANK'
                  AND rg.game_status = 'COMPLETED'
                  AND rg.ended_at >= ?
                  AND rg.ended_at < ?
                ORDER BY rg.score DESC, rg.ended_at ASC, rg.id ASC
                LIMIT 1
                """;

        Timestamp start = Timestamp.valueOf(LocalDateTime.now().minusHours(24));
        Timestamp end = Timestamp.valueOf(LocalDateTime.now().plusHours(1));

        runWarmup(baselineQuery, start, end);
        runWarmup(optimizedQuery, start, end);

        double baselineAvgMs = measureAverageMs(baselineQuery, start, end, ITERATIONS);
        double optimizedAvgMs = measureAverageMs(optimizedQuery, start, end, ITERATIONS);
        double improvement = ((baselineAvgMs - optimizedAvgMs) / baselineAvgMs) * 100.0;

        Map<String, Object> baselineExplain = explainRoadViewGameRow(baselineQuery, start, end);
        Map<String, Object> optimizedExplain = explainRoadViewGameRow(optimizedQuery, start, end);
        long baselineRows = toLong(baselineExplain.get("rows"));
        long optimizedRows = toLong(optimizedExplain.get("rows"));

        System.out.println("===== MVP INDEX PERFORMANCE REPORT =====");
        System.out.printf("Baseline avg(ms): %.3f%n", baselineAvgMs);
        System.out.printf("Optimized avg(ms): %.3f%n", optimizedAvgMs);
        System.out.printf("Improvement(%%): %.2f%n", improvement);
        System.out.printf("Baseline key: %s, rows: %d%n", baselineExplain.get("key"), baselineRows);
        System.out.printf("Optimized key: %s, rows: %d%n", optimizedExplain.get("key"), optimizedRows);

        String optimizedKey = String.valueOf(optimizedExplain.get("key"));

        assertNotNull(optimizedExplain.get("key"));
        assertTrue(optimizedKey.contains("idx_road_view_game_mvp"),
                () -> "optimized key=" + optimizedKey);
        assertTrue(optimizedRows <= baselineRows,
                String.format("optimizedRows=%d, baselineRows=%d", optimizedRows, baselineRows));
    }

    private void insertRoadViewGames(Long memberId) {
        String insertSql = """
                INSERT INTO road_view_game
                (answer_distance, poi_name, score, practice_sido, coordinate_id,
                 submitted_lng, submitted_lat, answer_time,
                 member_member_id, game_type, game_mode, game_status, ended_at,
                 created_date, last_modified_date)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;

        Timestamp now = Timestamp.valueOf(LocalDateTime.now());
        List<Object[]> batchArgs = java.util.stream.IntStream.range(0, ROW_COUNT)
                .mapToObj(i -> {
                    double score = (i % 1000) + (i % 10) * 0.1;
                    Timestamp endedAt = Timestamp.valueOf(LocalDateTime.now().minusMinutes(i % 1440));
                    return new Object[]{
                            10.0,
                            "poi-" + i,
                            score,
                            null,
                            null,
                            127.0,
                            37.0,
                            30.0,
                            memberId,
                            "ROADVIEW",
                            "RANK",
                            "COMPLETED",
                            endedAt,
                            now,
                            now
                    };
                })
                .toList();

        jdbcTemplate.batchUpdate(insertSql, batchArgs);
    }

    private void runWarmup(String sql, Timestamp start, Timestamp end) {
        for (int i = 0; i < 5; i++) {
            jdbcTemplate.queryForList(sql, start, end);
        }
    }

    private double measureAverageMs(String sql, Timestamp start, Timestamp end, int iterations) {
        long totalNanos = 0L;
        for (int i = 0; i < iterations; i++) {
            long begin = System.nanoTime();
            jdbcTemplate.queryForList(sql, start, end);
            totalNanos += (System.nanoTime() - begin);
        }
        return (totalNanos / 1_000_000.0) / iterations;
    }

    private Map<String, Object> explainRoadViewGameRow(String sql, Timestamp start, Timestamp end) {
        String explainSql = "EXPLAIN " + sql;
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(explainSql, start, end);
        return rows.stream()
                .filter(row -> {
                    Object table = row.get("table");
                    if (table == null) {
                        return false;
                    }
                    String tableName = String.valueOf(table);
                    return "rg".equalsIgnoreCase(tableName)
                            || tableName.toLowerCase().contains("road_view_game");
                })
                .findFirst()
                .orElse(rows.get(0));
    }

    private long toLong(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        return Long.parseLong(String.valueOf(value));
    }
}
