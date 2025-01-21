package com.kospot.kospot.domain.coordinate.repository;

import com.kospot.kospot.domain.coordinate.entity.Coordinate;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CoordinateRepository extends JpaRepository<Coordinate, Long> {
}
