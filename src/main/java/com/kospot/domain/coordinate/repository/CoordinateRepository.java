package com.kospot.domain.coordinate.repository;

import com.kospot.domain.coordinate.entity.Coordinate;
import com.kospot.domain.coordinate.entity.Sido;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface CoordinateRepository extends JpaRepository<Coordinate, Long> {

    @Query("SELECT COUNT(c) FROM Coordinate c WHERE c.sido = :sido")
    long countBySido(@Param("sido") Sido sido);

    @Query("SELECT c FROM Coordinate c WHERE c.sido = :sido OFFSET :offset LIMIT 1")
    Coordinate findBySidoWithOffset(@Param("sido") Sido sido, @Param("offset") long offset);

    // 전체 랜덤 Coordinate
    @Query("SELECT COUNT(c) FROM Coordinate c")
    long countAll();

    @Query("SELECT c FROM Coordinate c OFFSET :offset LIMIT 1")
    Coordinate findByRandomOffset(@Param("offset") long offset);
}
