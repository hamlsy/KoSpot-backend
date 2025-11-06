package com.kospot.domain.coordinate.repository;

import com.kospot.domain.coordinate.entity.Coordinate;
import com.kospot.domain.coordinate.entity.Sido;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CoordinateRepository extends JpaRepository<Coordinate, Long> {

    // 특정 Sido 랜덤
    @Query("SELECT COUNT(c) FROM Coordinate c WHERE c.address.sido = :sido")
    long countBySido(@Param("sido") Sido sido);

    @Query("SELECT c FROM Coordinate c WHERE c.address.sido = :sido")
    Page<Coordinate> findBySidoWithOffset(@Param("sido") Sido sido, Pageable pageable);

    // 전체 랜덤
    @Query("SELECT COUNT(c) FROM Coordinate c")
    long countAll();

    @Query("SELECT c FROM Coordinate c")
    Page<Coordinate> findAllCoordinates(Pageable pageable);

}
