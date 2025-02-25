package com.kospot.kospot.coordinate.service;

import com.kospot.kospot.domain.coordinate.entity.Coordinate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

@SpringBootTest
public class CoordinateServiceTest {
    @Mock
    private CoordinateService coordinateService;


    @Mock
    private List<Coordinate> coordinates;

    @BeforeEach
    void setUp(){
        for(int i = 0; i < 30; i++){

        }
    }

    @Test
    @DisplayName("시도별 좌표 랜덤 선택을 테스트합니다.")
    void findRandomCoordiateBySido_Success() throws Exception {

    }

    @Test
    @DisplayName("전체 좌표 랜덤 선택을 테스트합니다.")
    void findRandomCoordiateByAll_Success() throws Exception {

    }

}
