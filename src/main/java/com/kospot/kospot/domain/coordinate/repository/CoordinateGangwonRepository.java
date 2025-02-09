package com.kospot.kospot.domain.coordinate.repository;

import com.kospot.kospot.domain.coordinate.aop.SidoRepository;
import com.kospot.kospot.domain.coordinate.entity.coordinates.CoordinateGangwon;
import com.kospot.kospot.domain.coordinate.entity.sido.Sido;
import org.springframework.stereotype.Repository;

@SidoRepository(Sido.GANGWON)
public interface CoordinateGangwonRepository extends BaseCoordinateRepository<CoordinateGangwon, Long> {

}
