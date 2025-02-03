package com.kospot.kospot.domain.coordinate.repository;

import com.kospot.kospot.domain.coordinate.entity.Coordinate;
import com.kospot.kospot.domain.coordinate.entity.region.sido.Sido;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CoordinateRepository extends JpaRepository<Coordinate, Long> {
    Optional<List<Coordinate>> findByAddress_Sido(Sido sido);

}
