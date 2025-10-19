# 🔄 RoadView Submission 통합 리팩토링 가이드

## 📋 목차
1. [리팩토링 개요](#1-리팩토링-개요)
2. [변경 사항 요약](#2-변경-사항-요약)
3. [마이그레이션 가이드](#3-마이그레이션-가이드)
4. [DB 마이그레이션](#4-db-마이그레이션)
5. [코드 변경 가이드](#5-코드-변경-가이드)
6. [테스트 가이드](#6-테스트-가이드)
7. [FAQ](#7-faq)

---

## 1. 리팩토링 개요

### 🎯 **목적**
- RoadViewPlayerSubmission과 RoadViewTeamSubmission의 중복 코드 90% 제거
- 단일 책임 원칙 준수
- 유지보수성 향상

### ✅ **장점**
| 항목 | 개선 전 | 개선 후 | 개선율 |
|------|---------|---------|--------|
| 엔티티 수 | 3개 (Base + Player + Team) | 1개 (통합) | ⬇️ 66% |
| Repository | 2개 | 1개 | ⬇️ 50% |
| Service | 2개 | 1개 | ⬇️ 50% |
| 중복 코드 | 90% 중복 | 0% | ✅ 100% 제거 |
| 조회 쿼리 | 6개 | 2개 (+ 하위 호환 4개) | ⬇️ 67% |
| 테스트 코드 | 2배 작성 필요 | 1번만 작성 | ⬇️ 50% |

### 🔑 **핵심 설계 결정**
1. **다형성 연관관계**: GamePlayer와 teamNumber를 nullable로 설정
2. **매치타입 구분**: `PlayerMatchType` enum으로 개인전/팀전 구분
3. **하위 호환성**: `@Deprecated` 메서드로 기존 코드 지원
4. **정적 팩토리 메서드**: `forPlayer()`, `forTeam()`으로 안전한 객체 생성

---

## 2. 변경 사항 요약

### 📁 **새로 생성된 파일**
```
src/main/java/com/kospot/domain/multi/submission/
├── entity/roadview/
│   └── RoadViewSubmission.java                          [NEW] 통합 엔티티
├── repository/
│   └── RoadViewSubmissionRepository.java                [NEW] 통합 Repository
└── service/
    └── RoadViewSubmissionService.java                   [NEW] 통합 Service
```

### 🔄 **수정된 파일**
```
src/main/java/com/kospot/domain/multi/round/
├── entity/
│   └── RoadViewGameRound.java                           [MODIFIED] 제출 리스트 통합
├── repository/
│   └── RoadViewGameRoundRepository.java                 [MODIFIED] Fetch 쿼리 통합
└── adaptor/
    └── RoadViewGameRoundAdaptor.java                    [MODIFIED] 조회 메서드 통합
```

### ⚠️ **Deprecated (삭제 예정)**
```
src/main/java/com/kospot/domain/multi/submission/
├── entity/roadview/
│   ├── BaseRoadViewSubmission.java                      [DEPRECATED] 사용 안 함
│   ├── RoadViewPlayerSubmission.java                    [DEPRECATED] → RoadViewSubmission
│   └── RoadViewTeamSubmission.java                      [DEPRECATED] → RoadViewSubmission
├── repository/
│   ├── RoadViewPlayerSubmissionRepository.java          [DEPRECATED] → RoadViewSubmissionRepository
│   └── RoadViewTeamSubmissionRepository.java            [DEPRECATED] → RoadViewSubmissionRepository
└── service/
    ├── RoadViewPlayerSubmissionService.java             [DEPRECATED] → RoadViewSubmissionService
    └── RoadViewTeamSubmissionService.java               [DEPRECATED] → RoadViewSubmissionService
```

---

## 3. 마이그레이션 가이드

### 📝 **마이그레이션 단계**

#### **Phase 1: 준비 (1일)**
1. 이 문서 숙지
2. 기존 코드 백업
3. 테스트 환경 구축

#### **Phase 2: 새 코드 배포 (1일)**
1. 새로운 `RoadViewSubmission` 엔티티 배포
2. 기존 코드는 그대로 유지 (하위 호환)
3. 새 테이블 생성 확인

#### **Phase 3: 데이터 마이그레이션 (1일)**
4. 기존 데이터를 새 테이블로 복사
5. 데이터 정합성 검증
6. 양쪽 테이블 동시 운영

#### **Phase 4: 코드 전환 (2일)**
7. UseCase 코드를 새 Service로 변경
8. 통합 테스트 수행
9. 스모크 테스트 통과 확인

#### **Phase 5: 정리 (1일)**
10. 기존 테이블/코드 제거
11. Deprecated 메서드 제거
12. 문서화 완료

---

## 4. DB 마이그레이션

### 📊 **테이블 스키마**

#### **개선 전 (2개 테이블)**
```sql
-- 개인전 테이블
CREATE TABLE road_view_player_submission (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    game_player_id BIGINT NOT NULL,
    game_round_id BIGINT NOT NULL,
    lat DOUBLE NOT NULL,
    lng DOUBLE NOT NULL,
    distance DOUBLE NOT NULL,
    time_to_answer DOUBLE NOT NULL,
    earned_score INT,
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);

-- 팀전 테이블
CREATE TABLE road_view_team_submission (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    team_number INT NOT NULL,
    game_round_id BIGINT NOT NULL,
    lat DOUBLE NOT NULL,
    lng DOUBLE NOT NULL,
    distance DOUBLE NOT NULL,
    time_to_answer DOUBLE NOT NULL,
    earned_score INT,
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);
```

#### **개선 후 (1개 테이블)**
```sql
-- 통합 테이블
CREATE TABLE road_view_submission (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    match_type VARCHAR(20) NOT NULL,           -- 'SOLO' 또는 'TEAM'
    game_player_id BIGINT,                     -- 개인전: not null, 팀전: null
    team_number INT,                           -- 팀전: not null, 개인전: null
    game_round_id BIGINT NOT NULL,
    lat DOUBLE NOT NULL,
    lng DOUBLE NOT NULL,
    distance DOUBLE NOT NULL,
    time_to_answer DOUBLE NOT NULL,
    earned_score INT,
    rank INT,                                  -- 순위 캐시
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    
    -- 인덱스
    INDEX idx_round_match_type (game_round_id, match_type),
    INDEX idx_round_player (game_round_id, game_player_id),
    INDEX idx_round_team (game_round_id, team_number)
);
```

### 🔄 **마이그레이션 스크립트**

#### **Step 1: 새 테이블 생성**
```sql
CREATE TABLE road_view_submission (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    match_type VARCHAR(20) NOT NULL,
    game_player_id BIGINT,
    team_number INT,
    game_round_id BIGINT NOT NULL,
    lat DOUBLE NOT NULL,
    lng DOUBLE NOT NULL,
    distance DOUBLE NOT NULL,
    time_to_answer DOUBLE NOT NULL,
    earned_score INT,
    rank INT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    INDEX idx_round_match_type (game_round_id, match_type),
    INDEX idx_round_player (game_round_id, game_player_id),
    INDEX idx_round_team (game_round_id, team_number),
    FOREIGN KEY (game_player_id) REFERENCES game_player(id),
    FOREIGN KEY (game_round_id) REFERENCES road_view_game_round(id)
);
```

#### **Step 2: 데이터 마이그레이션**
```sql
-- 개인전 데이터 복사
INSERT INTO road_view_submission (
    match_type,
    game_player_id,
    team_number,
    game_round_id,
    lat,
    lng,
    distance,
    time_to_answer,
    earned_score,
    created_at,
    updated_at
)
SELECT 
    'SOLO' AS match_type,
    game_player_id,
    NULL AS team_number,
    game_round_id,
    lat,
    lng,
    distance,
    time_to_answer,
    earned_score,
    created_at,
    updated_at
FROM road_view_player_submission;

-- 팀전 데이터 복사
INSERT INTO road_view_submission (
    match_type,
    game_player_id,
    team_number,
    game_round_id,
    lat,
    lng,
    distance,
    time_to_answer,
    earned_score,
    created_at,
    updated_at
)
SELECT 
    'TEAM' AS match_type,
    NULL AS game_player_id,
    team_number,
    game_round_id,
    lat,
    lng,
    distance,
    time_to_answer,
    earned_score,
    created_at,
    updated_at
FROM road_view_team_submission;
```

#### **Step 3: 데이터 검증**
```sql
-- 개수 확인
SELECT 
    (SELECT COUNT(*) FROM road_view_player_submission) AS player_count,
    (SELECT COUNT(*) FROM road_view_team_submission) AS team_count,
    (SELECT COUNT(*) FROM road_view_submission WHERE match_type = 'SOLO') AS new_player_count,
    (SELECT COUNT(*) FROM road_view_submission WHERE match_type = 'TEAM') AS new_team_count,
    (SELECT COUNT(*) FROM road_view_submission) AS total_new_count;

-- 샘플 데이터 비교
SELECT 'OLD PLAYER' AS source, id, game_player_id, game_round_id, distance 
FROM road_view_player_submission LIMIT 5
UNION ALL
SELECT 'NEW PLAYER' AS source, id, game_player_id, game_round_id, distance 
FROM road_view_submission WHERE match_type = 'SOLO' LIMIT 5;
```

#### **Step 4: 기존 테이블 백업 및 삭제**
```sql
-- 백업 (나중에 문제 발생 시 복구용)
RENAME TABLE road_view_player_submission TO road_view_player_submission_backup_20241012;
RENAME TABLE road_view_team_submission TO road_view_team_submission_backup_20241012;

-- 일정 기간 후 (예: 1개월) 백업 삭제
-- DROP TABLE road_view_player_submission_backup_20241012;
-- DROP TABLE road_view_team_submission_backup_20241012;
```

---

## 5. 코드 변경 가이드

### 🔄 **개인전 제출 코드 변경**

#### **Before (기존)**
```java
// 기존 Service 사용
@Service
public class SubmitRoadViewPlayerAnswerUseCase {
    private final RoadViewPlayerSubmissionService submissionService;
    
    public void execute(...) {
        RoadViewPlayerSubmission submission = request.toEntity();
        submissionService.createSubmission(round, player, submission);
    }
}
```

#### **After (통합)**
```java
// 통합 Service 사용
@Service
public class SubmitRoadViewPlayerAnswerUseCase {
    private final RoadViewSubmissionService submissionService;  // ✅ 통합 Service
    
    public void execute(...) {
        // ✅ 정적 팩토리 메서드로 안전한 객체 생성
        RoadViewSubmission submission = RoadViewSubmission.forPlayer(
                player,
                round,
                request.getLat(),
                request.getLng(),
                request.getDistance(),
                request.getTimeToAnswer()
        );
        
        // ✅ 개인전 전용 메서드
        submissionService.createPlayerSubmission(round, player, submission);
    }
}
```

### 🔄 **팀전 제출 코드 변경**

#### **Before (기존)**
```java
@Service
public class SubmitRoadViewTeamAnswerUseCase {
    private final RoadViewTeamSubmissionService submissionService;
    
    public void execute(...) {
        RoadViewTeamSubmission submission = request.toEntity();
        submissionService.createSubmission(round, teamNumber, submission);
    }
}
```

#### **After (통합)**
```java
@Service
public class SubmitRoadViewTeamAnswerUseCase {
    private final RoadViewSubmissionService submissionService;  // ✅ 통합 Service
    
    public void execute(...) {
        // ✅ 정적 팩토리 메서드로 안전한 객체 생성
        RoadViewSubmission submission = RoadViewSubmission.forTeam(
                teamNumber,
                round,
                request.getLat(),
                request.getLng(),
                request.getDistance(),
                request.getTimeToAnswer()
        );
        
        // ✅ 팀전 전용 메서드
        submissionService.createTeamSubmission(round, teamNumber, submission);
    }
}
```

### 🔄 **라운드 종료 코드 변경**

#### **Before (기존)**
```java
@UseCase
public class EndRoadViewSoloRoundUseCase {
    private final RoadViewPlayerSubmissionService submissionService;
    
    public RoadViewRoundResponse.PlayerResult execute(Long gameId, Long roundId) {
        // 개인전 제출 조회
        List<RoadViewPlayerSubmission> submissions = round.getRoadViewPlayerSubmissions();
        
        // 점수 계산
        submissionService.updateRankAndScore(submissions);
    }
}
```

#### **After (통합)**
```java
@UseCase
public class EndRoadViewSoloRoundUseCase {
    private final RoadViewSubmissionService submissionService;  // ✅ 통합 Service
    
    public RoadViewRoundResponse.PlayerResult execute(Long gameId, Long roundId) {
        // ✅ 통합 제출 조회 (내부적으로 필터링)
        List<RoadViewSubmission> submissions = round.getPlayerSubmissions();
        
        // ✅ 미제출 플레이어 0점 처리
        submissionService.handleNonSubmittedPlayers(gameId, round);
        
        // ✅ 개인전 점수 계산
        submissionService.calculatePlayerRankAndScore(submissions);
    }
}
```

---

## 6. 테스트 가이드

### ✅ **테스트 체크리스트**

#### **단위 테스트**
- [ ] RoadViewSubmission.forPlayer() 정적 팩토리 메서드
- [ ] RoadViewSubmission.forTeam() 정적 팩토리 메서드
- [ ] RoadViewSubmission.getSubmitterId() 다형성
- [ ] RoadViewSubmissionService.createPlayerSubmission()
- [ ] RoadViewSubmissionService.createTeamSubmission()
- [ ] RoadViewSubmissionService.calculatePlayerRankAndScore()
- [ ] RoadViewSubmissionService.calculateTeamRankAndScore()

#### **통합 테스트**
- [ ] 개인전 제출 → DB 저장 확인
- [ ] 팀전 제출 → DB 저장 확인
- [ ] 중복 제출 방지 확인
- [ ] 미제출자 0점 처리 확인
- [ ] 순위 계산 정확성 확인
- [ ] 점수 분배 정확성 확인

#### **성능 테스트**
- [ ] N+1 쿼리 문제 없음 확인
- [ ] 조회 성능 저하 없음 확인
- [ ] 인덱스 효과 확인

### 🧪 **테스트 예시**

```java
@SpringBootTest
@Transactional
class RoadViewSubmissionServiceTest {

    @Autowired
    private RoadViewSubmissionService submissionService;

    @Test
    @DisplayName("개인전 제출 생성 성공")
    void createPlayerSubmission_Success() {
        // Given
        GamePlayer player = createTestPlayer();
        RoadViewGameRound round = createTestRound();
        RoadViewSubmission submission = RoadViewSubmission.forPlayer(
                player, round, 37.5, 127.0, 1000.0, 5000.0
        );

        // When
        RoadViewSubmission saved = submissionService.createPlayerSubmission(
                round, player, submission
        );

        // Then
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getMatchType()).isEqualTo(PlayerMatchType.SOLO);
        assertThat(saved.getGamePlayer()).isEqualTo(player);
        assertThat(saved.getTeamNumber()).isNull();
    }

    @Test
    @DisplayName("팀전 제출 생성 성공")
    void createTeamSubmission_Success() {
        // Given
        RoadViewGameRound round = createTestRound();
        RoadViewSubmission submission = RoadViewSubmission.forTeam(
                1, round, 37.5, 127.0, 1000.0, 5000.0
        );

        // When
        RoadViewSubmission saved = submissionService.createTeamSubmission(
                round, 1, submission
        );

        // Then
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getMatchType()).isEqualTo(PlayerMatchType.TEAM);
        assertThat(saved.getTeamNumber()).isEqualTo(1);
        assertThat(saved.getGamePlayer()).isNull();
    }
}
```

---

## 7. FAQ

### ❓ **Q1: null 컬럼이 있는데 괜찮나요?**
**A:** 네, 괜찮습니다. 
- `matchType`으로 어떤 컬럼을 사용할지 명확히 구분됨
- JPA Single Table Inheritance도 동일한 패턴 사용
- 트레이드오프: null 컬럼 vs 중복 코드 90% → null 컬럼이 훨씬 유리

### ❓ **Q2: 기존 코드는 언제 삭제하나요?**
**A:** 단계적 삭제 권장
1. Phase 1-4: 새 코드와 기존 코드 공존 (`@Deprecated`)
2. 1-2주 안정화 기간
3. Phase 5: 기존 코드 제거

### ❓ **Q3: 하위 호환성은 어떻게 보장하나요?**
**A:** `@Deprecated` 메서드로 보장
```java
@Deprecated
public void addPlayerSubmission(RoadViewSubmission submission) {
    addSubmission(submission);  // 새 메서드로 위임
}
```

### ❓ **Q4: Photo 모드는 어떻게 하나요?**
**A:** 동일한 패턴 적용
1. `PhotoSubmission` 통합 엔티티 생성
2. `PhotoSubmissionRepository` 통합
3. `PhotoSubmissionService` 통합
4. 패턴이 일관되므로 학습 비용 감소

### ❓ **Q5: 롤백이 필요하면 어떻게 하나요?**
**A:** 백업 테이블 활용
```sql
-- 롤백 스크립트
RENAME TABLE road_view_submission TO road_view_submission_new;
RENAME TABLE road_view_player_submission_backup_20241012 TO road_view_player_submission;
RENAME TABLE road_view_team_submission_backup_20241012 TO road_view_team_submission;
```

---

## 📚 **참고 자료**

- [Refactoring: Improving the Design of Existing Code - Martin Fowler](https://refactoring.com/)
- [JPA Single Table Inheritance](https://docs.oracle.com/javaee/7/tutorial/persistence-intro004.htm)
- [Clean Code - Robert C. Martin](https://www.amazon.com/Clean-Code-Handbook-Software-Craftsmanship/dp/0132350882)

---

## ✅ **최종 체크리스트**

### **배포 전**
- [ ] 모든 테스트 통과
- [ ] 코드 리뷰 완료
- [ ] DB 마이그레이션 스크립트 준비
- [ ] 롤백 계획 수립
- [ ] 팀원들에게 공지

### **배포 후**
- [ ] 스모크 테스트 수행
- [ ] 모니터링 확인 (에러율, 응답 시간)
- [ ] 데이터 정합성 검증
- [ ] 1-2주 안정화 기간 확보
- [ ] 기존 코드 제거 (Deprecated)

---

**작성일:** 2024-10-12  
**작성자:** KoSpot Backend Team  
**버전:** 1.0.0

