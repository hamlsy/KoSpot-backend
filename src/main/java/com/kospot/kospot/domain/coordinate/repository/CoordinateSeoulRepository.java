package com.kospot.kospot.domain.coordinate.repository;

import com.kospot.kospot.domain.coordinate.aop.SidoRepository;
import com.kospot.kospot.domain.coordinate.entity.coordinates.CoordinateSeoul;
import com.kospot.kospot.domain.coordinate.entity.sido.Sido;
import org.springframework.stereotype.Repository;

@SidoRepository(Sido.SEOUL)
@Repository
public interface CoordinateSeoulRepository extends BaseCoordinateRepository<CoordinateSeoul, Long> {

}
