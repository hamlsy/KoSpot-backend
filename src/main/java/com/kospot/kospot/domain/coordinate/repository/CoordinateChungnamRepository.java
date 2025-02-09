package com.kospot.kospot.domain.coordinate.repository;

import com.kospot.kospot.domain.coordinate.aop.SidoRepository;
import com.kospot.kospot.domain.coordinate.entity.coordinates.CoordinateChungnam;
import com.kospot.kospot.domain.coordinate.entity.sido.Sido;
import org.springframework.stereotype.Repository;

@SidoRepository(Sido.CHUNGNAM)
public interface CoordinateChungnamRepository extends BaseCoordinateRepository<CoordinateChungnam, Long> {

}
