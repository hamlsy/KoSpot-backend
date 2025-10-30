package com.kospot.admin.coordinate;

import com.kospot.application.admin.coordinate.CreateCoordinateUseCase;
import com.kospot.application.admin.coordinate.DeleteCoordinateUseCase;
import com.kospot.application.admin.coordinate.FindAllCoordinatesUseCase;
import com.kospot.domain.coordinate.entity.Coordinate;
import com.kospot.domain.coordinate.repository.CoordinateRepository;
import com.kospot.domain.coordinate.entity.LocationType;
import com.kospot.domain.member.entity.Member;
import com.kospot.domain.member.repository.MemberRepository;
import com.kospot.domain.member.vo.Role;
import com.kospot.presentation.admin.dto.request.AdminCoordinateRequest;
import com.kospot.presentation.admin.dto.response.AdminCoordinateResponse;
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

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@Slf4j
@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class CoordinateUseCaseTest {

    @Autowired
    private CreateCoordinateUseCase createCoordinateUseCase;

    @Autowired
    private DeleteCoordinateUseCase deleteCoordinateUseCase;

    @Autowired
    private FindAllCoordinatesUseCase findAllCoordinatesUseCase;

    @Autowired
    private CoordinateRepository coordinateRepository;

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

    @DisplayName("좌표 생성 테스트")
    @Test
    void createCoordinate_Success() {
        // given
        AdminCoordinateRequest.Create request = new AdminCoordinateRequest.Create(
                37.5665,
                126.9780,
                "서울시청",
                "seoul",
                "중구",
                "세종대로 110",
                "일반관광지"
        );

        // when
        Long coordinateId = createCoordinateUseCase.execute(admin, request);

        // then
        Coordinate coordinate = coordinateRepository.findById(coordinateId).orElseThrow();
        assertEquals(37.5665, coordinate.getLat());
        assertEquals(126.9780, coordinate.getLng());
        assertEquals("서울시청", coordinate.getPoiName());
        assertEquals(LocationType.TOURIST_ATTRACTION, coordinate.getLocationType());
        log.info("생성된 좌표: {}", coordinate);
    }

    @DisplayName("좌표 생성 - 권한 없음")
    @Test
    void createCoordinate_NoPermission_ThrowsException() {
        // given
        AdminCoordinateRequest.Create request = new AdminCoordinateRequest.Create(
                37.5665,
                126.9780,
                "서울시청",
                "seoul",
                "중구",
                "세종대로 110",
                "일반관광지"
        );

        // when & then
        assertThrows(Exception.class, () -> createCoordinateUseCase.execute(user, request));
    }

    @DisplayName("좌표 목록 조회 - 페이징")
    @Test
    void findAllCoordinates_WithPaging_Success() {
        // given
        for (int i = 1; i <= 25; i++) {
            coordinateRepository.save(
                    Coordinate.builder()
                            .lat(37.5 + i * 0.01)
                            .lng(126.9 + i * 0.01)
                            .poiName("장소" + i)
                            .locationType(LocationType.TOURIST_ATTRACTION)
                            .build()
            );
        }

        Pageable pageable = PageRequest.of(0, 10);

        // when
        Page<AdminCoordinateResponse.CoordinateInfo> coordinatesPage = 
                findAllCoordinatesUseCase.execute(admin, pageable);

        // then
        assertEquals(10, coordinatesPage.getContent().size());
        log.info("조회된 좌표 개수: {}", coordinatesPage.getContent().size());
        log.info("첫 번째 좌표: {}", coordinatesPage.getContent().get(0));
    }

    @DisplayName("좌표 삭제 테스트")
    @Test
    void deleteCoordinate_Success() {
        // given
        Coordinate coordinate = coordinateRepository.save(
                Coordinate.builder()
                        .lat(37.5665)
                        .lng(126.9780)
                        .poiName("테스트 장소")
                        .locationType(LocationType.NATURE_LEISURE)
                        .build()
        );

        // when
        deleteCoordinateUseCase.execute(admin, coordinate.getId());

        // then
        assertFalse(coordinateRepository.findById(coordinate.getId()).isPresent());
    }

    @DisplayName("좌표 삭제 - 권한 없음")
    @Test
    void deleteCoordinate_NoPermission_ThrowsException() {
        // given
        Coordinate coordinate = coordinateRepository.save(
                Coordinate.builder()
                        .lat(37.5665)
                        .lng(126.9780)
                        .poiName("테스트 장소")
                        .locationType(LocationType.TOURIST_ATTRACTION)
                        .build()
        );

        // when & then
        assertThrows(Exception.class, () -> deleteCoordinateUseCase.execute(user, coordinate.getId()));
    }

    @DisplayName("좌표 생성 - 다양한 위치 타입 테스트")
    @Test
    void createCoordinate_VariousLocationTypes_Success() {
        // given
        String[] typeStrings = {
                "유명관광지",
                "폭포/계곡",
                "테마공원/대형놀이공원",
                "지역축제"
        };

        LocationType[] expectedTypes = {
                LocationType.TOURIST_ATTRACTION,
                LocationType.NATURE_LEISURE,
                LocationType.NATURE_LEISURE,
                LocationType.FESTIVAL_CULTURE
        };

        // when
        for (int i = 0; i < typeStrings.length; i++) {
            AdminCoordinateRequest.Create request = new AdminCoordinateRequest.Create(
                    37.5 + i * 0.01,
                    126.9 + i * 0.01,
                    "장소" + i,
                    "seoul",
                    "중구",
                    "주소" + i,
                    typeStrings[i]
            );

            createCoordinateUseCase.execute(admin, request);
        }

        // then
        List<Coordinate> coordinates = coordinateRepository.findAll();
        assertEquals(expectedTypes.length, coordinates.size());

        for (int i = 0; i < expectedTypes.length; i++) {
            assertEquals(expectedTypes[i], coordinates.get(i).getLocationType());
        }
    }
}

