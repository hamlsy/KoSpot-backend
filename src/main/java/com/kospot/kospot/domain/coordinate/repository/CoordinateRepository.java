package com.kospot.kospot.domain.coordinate.repository;

import com.kospot.kospot.domain.coordinate.entity.Coordinate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CoordinateRepository extends JpaRepository<Coordinate, Long> {
    List<Coordinate> findByAddress_Sido(String sido);
}
