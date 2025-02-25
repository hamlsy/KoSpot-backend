package com.kospot.kospot.domain.coordinate.repository.location;

import com.kospot.kospot.domain.coordinate.aop.SidoRepository;
import com.kospot.kospot.domain.coordinate.entity.coordinates.CoordinateGyeongnam;
import com.kospot.kospot.domain.coordinate.entity.sido.Sido;
import com.kospot.kospot.domain.coordinate.repository.base.CoordinateRepository;

@SidoRepository(Sido.GYEONGNAM)
public interface CoordinateGyeongnamRepository extends CoordinateRepository<CoordinateGyeongnam, Long> {

}
