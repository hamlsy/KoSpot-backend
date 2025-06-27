package com.kospot.domain.coordinate.repository.location;

import com.kospot.domain.coordinate.aop.SidoRepository;
import com.kospot.domain.coordinate.entity.coordinates.CoordinateGyeongnam;
import com.kospot.domain.coordinate.vo.Sido;
import com.kospot.domain.coordinate.repository.base.CoordinateRepository;

@SidoRepository(Sido.GYEONGNAM)
public interface CoordinateGyeongnamRepository extends CoordinateRepository<CoordinateGyeongnam, Long> {

}
