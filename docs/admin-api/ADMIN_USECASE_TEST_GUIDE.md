# Admin UseCase 테스트 가이드

## 개요

이 문서는 관리자 기능 관련 UseCase들의 테스트 코드에 대한 가이드입니다.

## 테스트 파일 목록

### 1. 게임 설정 관리 테스트
**파일**: `src/test/java/com/kospot/admin/gameconfig/GameConfigUseCaseTest.java`

**테스트 UseCase:**
- `CreateGameConfigUseCase` - 게임 설정 개별 생성
- `InitializeAllGameConfigsUseCase` - 모든 기본 설정 초기화
- `ActivateGameConfigUseCase` - 게임 설정 활성화
- `DeactivateGameConfigUseCase` - 게임 설정 비활성화
- `FindAllGameConfigsUseCase` - 게임 설정 목록 조회
- `DeleteGameConfigUseCase` - 게임 설정 삭제

**주요 테스트 케이스:**
```java
@Test
void createGameConfig_SingleMode_Success()          // 싱글 모드 생성
@Test
void createGameConfig_MultiMode_Success()           // 멀티 모드 생성
@Test
void createGameConfig_NoPermission_ThrowsException() // 권한 없음 예외
@Test
void initializeAllGameConfigs_Success()             // 전체 초기화
@Test
void initializeAllGameConfigs_SkipExisting_Success() // 중복 생성 방지
@Test
void activateGameConfig_Success()                   // 활성화
@Test
void deactivateGameConfig_Success()                 // 비활성화
@Test
void findAllGameConfigs_Success()                   // 목록 조회
@Test
void deleteGameConfig_Success()                     // 삭제
@Test
void deleteGameConfig_NoPermission_ThrowsException() // 권한 없음 예외
```

**주요 검증 사항:**
- ✅ 싱글 모드와 멀티 모드 구분
- ✅ 관리자 권한 검증
- ✅ 중복 생성 방지
- ✅ 활성화/비활성화 상태 변경
- ✅ 6개 기본 설정 생성 (싱글 2개 + 멀티 4개)

---

### 2. 좌표 관리 테스트
**파일**: `src/test/java/com/kospot/admin/coordinate/CoordinateUseCaseTest.java`

**테스트 UseCase:**
- `CreateCoordinateUseCase` - 좌표 생성
- `DeleteCoordinateUseCase` - 좌표 삭제
- `FindAllCoordinatesUseCase` - 좌표 목록 조회

**주요 테스트 케이스:**
```java
@Test
void createCoordinate_Success()                        // 좌표 생성
@Test
void createCoordinate_NoPermission_ThrowsException()   // 권한 없음 예외
@Test
void findAllCoordinates_WithPaging_Success()           // 페이징 조회
@Test
void deleteCoordinate_Success()                        // 삭제
@Test
void deleteCoordinate_NoPermission_ThrowsException()   // 권한 없음 예외
@Test
void createCoordinate_VariousLocationTypes_Success()   // 다양한 위치 타입
```

**주요 검증 사항:**
- ✅ 좌표 필드 (lat, lng, poiName) 검증
- ✅ LocationType 매핑 검증
  - `유명관광지` → `TOURIST_ATTRACTION`
  - `폭포/계곡` → `NATURE_LEISURE`
  - `지역축제` → `FESTIVAL_CULTURE`
  - `테마공원/대형놀이공원` → `NATURE_LEISURE`
- ✅ 페이징 처리 (25개 중 10개 조회)
- ✅ 관리자 권한 검증

---

### 3. 배너 관리 테스트
**파일**: `src/test/java/com/kospot/admin/banner/BannerUseCaseTest.java`

**테스트 UseCase:**
- `ActivateBannerUseCase` - 배너 활성화
- `DeactivateBannerUseCase` - 배너 비활성화
- `FindAllBannersUseCase` - 배너 목록 조회
- `DeleteBannerUseCase` - 배너 삭제

**주요 테스트 케이스:**
```java
@Test
void activateBanner_Success()                        // 활성화
@Test
void deactivateBanner_Success()                      // 비활성화
@Test
void findAllBanners_Success()                        // 목록 조회
@Test
void deleteBanner_Success()                          // 삭제 (Image도 cascade)
@Test
void activateBanner_NoPermission_ThrowsException()   // 권한 없음 예외
@Test
void deleteBanner_NoPermission_ThrowsException()     // 권한 없음 예외
@Test
void findAllBanners_OrderedByDisplayOrder_Success()  // displayOrder 정렬
```

**주요 검증 사항:**
- ✅ 활성화/비활성화 상태 변경
- ✅ Banner 삭제 시 Image도 cascade로 삭제
- ✅ displayOrder에 따른 정렬
- ✅ 관리자 권한 검증
- ✅ Image 엔티티와의 OneToOne 관계

---

### 4. 회원 관리 테스트
**파일**: `src/test/java/com/kospot/admin/member/MemberUseCaseTest.java`

**테스트 UseCase:**
- `FindAllMembersUseCase` - 회원 목록 조회
- `FindMemberDetailUseCase` - 회원 상세 조회

**주요 테스트 케이스:**
```java
@Test
void findAllMembers_WithPaging_Success()             // 페이징 목록 조회
@Test
void findMemberDetail_Success()                      // 상세 정보 조회
@Test
void findAllMembers_NoPermission_ThrowsException()   // 권한 없음 예외
@Test
void findMemberDetail_NoPermission_ThrowsException() // 권한 없음 예외
@Test
void findAllMembers_VariousRoles_Success()           // 다양한 역할 조회
```

**주요 검증 사항:**
- ✅ 페이징 처리 (25개 중 10개 조회)
- ✅ MemberStatistic 통계 정보 조회
  - `singlePracticeGames`, `singleRankGames`
  - `multiGames`, `multiFirstPlace`, `multiSecondPlace`, `multiThirdPlace`
  - `bestScore`, `currentStreak`, `longestStreak`
- ✅ Role별 필터링 (ADMIN, USER)
- ✅ 관리자 권한 검증

---

### 5. 메인 페이지 테스트
**파일**: `src/test/java/com/kospot/main/MainPageUseCaseTest.java`

**테스트 UseCase:**
- `FindMainPageInfoUseCase` - 메인 페이지 정보 조회

**주요 테스트 케이스:**
```java
@Test
void findMainPageInfo_NoGameConfig_DefaultTrue()        // GameConfig 없을 때 기본값 true
@Test
void findMainPageInfo_Admin_Success()                   // 관리자 조회
@Test
void findMainPageInfo_OnlyRoadviewEnabled_Success()     // 로드뷰만 활성화
@Test
void findMainPageInfo_OnlyMultiplayEnabled_Success()    // 멀티플레이만 활성화
@Test
void findMainPageInfo_RecentNoticesLimit_Success()      // 최근 공지사항 3개 제한
@Test
void findMainPageInfo_ExcludeInactiveBanners_Success()  // 비활성 배너 제외
@Test
void findMainPageInfo_AllModesDisabled_Success()        // 모든 모드 비활성화
```

**주요 검증 사항:**
- ✅ **GameConfig가 없으면 모든 모드 true (기본값)**
- ✅ `isAdmin` 플래그 (관리자 여부)
- ✅ `GameModeStatus` 집계
  - `roadviewEnabled`: 로드뷰 싱글 또는 멀티 활성화
  - `photoEnabled`: 포토 싱글 또는 멀티 활성화
  - `multiplayEnabled`: 멀티플레이 모드 활성화
- ✅ 최근 공지사항 **최대 3개** 조회
- ✅ 비활성화된 배너 자동 제외
- ✅ 최신 순 정렬

---

## 테스트 실행 방법

### 전체 테스트 실행
```bash
./gradlew test
```

### 특정 테스트 클래스 실행
```bash
# 게임 설정 테스트
./gradlew test --tests GameConfigUseCaseTest

# 좌표 관리 테스트
./gradlew test --tests CoordinateUseCaseTest

# 배너 관리 테스트
./gradlew test --tests BannerUseCaseTest

# 회원 관리 테스트
./gradlew test --tests MemberUseCaseTest

# 메인 페이지 테스트
./gradlew test --tests MainPageUseCaseTest
```

### 특정 메서드만 실행
```bash
./gradlew test --tests GameConfigUseCaseTest.initializeAllGameConfigs_Success
```

### Admin 관련 테스트만 실행
```bash
./gradlew test --tests "com.kospot.admin.*"
```

---

## 테스트 환경 설정

### application-test.yml
테스트는 `test` 프로파일을 사용합니다.

```yaml
spring:
  datasource:
    url: jdbc:h2:mem:testdb
    driver-class-name: org.h2.Driver
    username: sa
    password:
  jpa:
    hibernate:
      ddl-auto: create-drop
    show-sql: true
```

### @Transactional
모든 테스트는 `@Transactional`을 사용하여 각 테스트 후 자동 롤백됩니다.

---

## 테스트 커버리지

### 게임 설정 관리 (GameConfig)
- ✅ 생성 (싱글/멀티)
- ✅ 전체 초기화
- ✅ 활성화/비활성화
- ✅ 목록 조회
- ✅ 삭제
- ✅ 권한 검증 (2개)
- **총 10개 테스트**

### 좌표 관리 (Coordinate)
- ✅ 생성
- ✅ 삭제
- ✅ 페이징 조회
- ✅ 다양한 위치 타입
- ✅ 권한 검증 (2개)
- **총 6개 테스트**

### 배너 관리 (Banner)
- ✅ 활성화/비활성화
- ✅ 목록 조회
- ✅ 삭제 (cascade)
- ✅ displayOrder 정렬
- ✅ 권한 검증 (2개)
- **총 7개 테스트**

### 회원 관리 (Member)
- ✅ 페이징 목록 조회
- ✅ 상세 정보 조회
- ✅ Role별 조회
- ✅ 권한 검증 (2개)
- **총 5개 테스트**

### 메인 페이지 (MainPage)
- ✅ 기본값 처리
- ✅ 관리자 조회
- ✅ 모드별 활성화
- ✅ 공지사항 제한
- ✅ 배너 필터링
- **총 7개 테스트**

**총 테스트 수: 35개**

---

## 주요 테스트 패턴

### 1. Given-When-Then 패턴
```java
@Test
void createGameConfig_Success() {
    // given - 테스트 데이터 준비
    AdminGameConfigRequest.Create request = new AdminGameConfigRequest.Create(...);
    
    // when - 실제 실행
    Long configId = createGameConfigUseCase.execute(admin, request);
    
    // then - 검증
    GameConfig config = gameConfigRepository.findById(configId).orElseThrow();
    assertEquals(GameMode.ROADVIEW, config.getGameMode());
}
```

### 2. 권한 검증 패턴
```java
@Test
void createGameConfig_NoPermission_ThrowsException() {
    // given
    AdminGameConfigRequest.Create request = new AdminGameConfigRequest.Create(...);
    
    // when & then
    assertThrows(Exception.class, () -> createGameConfigUseCase.execute(user, request));
}
```

### 3. 페이징 테스트 패턴
```java
@Test
void findAllCoordinates_WithPaging_Success() {
    // given - 25개 생성
    for (int i = 1; i <= 25; i++) {
        coordinateRepository.save(...);
    }
    Pageable pageable = PageRequest.of(0, 10);
    
    // when
    Page<CoordinateInfo> page = findAllCoordinatesUseCase.execute(admin, pageable);
    
    // then - 10개만 조회
    assertEquals(10, page.getContent().size());
}
```

### 4. Cascade 테스트 패턴
```java
@Test
void deleteBanner_Success() {
    // given
    Image image = createTestImage();
    Banner banner = bannerRepository.save(Banner.builder().image(image).build());
    Long imageId = image.getId();
    
    // when
    deleteBannerUseCase.execute(admin, banner.getId());
    
    // then - Banner와 Image 모두 삭제
    assertFalse(bannerRepository.findById(banner.getId()).isPresent());
    assertFalse(imageRepository.findById(imageId).isPresent());
}
```

---

## 트러블슈팅

### 1. 테스트 격리 문제
**증상**: 다른 테스트의 데이터가 남아있음  
**해결**: `@Transactional` 확인, `@BeforeEach`에서 데이터 초기화

### 2. Lazy Loading 예외
**증상**: `LazyInitializationException` 발생  
**해결**: `@Transactional` 추가 또는 `fetch = FetchType.EAGER`

### 3. 권한 검증 실패
**증상**: 관리자 검증이 작동하지 않음  
**해결**: `admin.validateAdmin()` 메서드 확인, Role이 ADMIN인지 확인

### 4. Page vs List
**증상**: Type mismatch 에러  
**해결**: `Page<T>`를 반환하는 경우 `.getContent()`로 List 변환

---

## 추가 테스트 작성 가이드

### 새로운 UseCase 테스트 작성 시
1. **패키지 구조 유지**: `com.kospot.admin.{domain}`
2. **@SpringBootTest + @ActiveProfiles("test")** 사용
3. **@Transactional** 추가 (자동 롤백)
4. **BeforeEach에서 admin, user 생성**
5. **Given-When-Then 패턴** 준수
6. **권한 검증 테스트** 필수 포함
7. **로그 출력**: `log.info()`로 결과 확인

### 테스트 네이밍 컨벤션
```
{메서드명}_{시나리오}_{예상결과}

예시:
- createGameConfig_SingleMode_Success
- createGameConfig_NoPermission_ThrowsException
- findAllMembers_WithPaging_Success
```

---

## 참고 자료

- [기존 Notice 테스트](../../src/test/java/com/kospot/notice/usecase/NoticeUseCaseTest.java)
- [기존 Item 테스트](../../src/test/java/com/kospot/item/usecase/ItemUseCaseTest.java)
- [Admin API 가이드](./ADMIN_API_GUIDE.md)
- [메인 페이지 API 가이드](../frontend-integration/MAIN_PAGE_API_GUIDE.md)

