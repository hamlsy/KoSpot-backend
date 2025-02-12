package com.kospot.kospot.domain.coordinate.repository;

import com.kospot.kospot.domain.coordinate.aop.SidoRepository;
import com.kospot.kospot.domain.coordinate.entity.coordinates.CoordinateGyeonggi;
import com.kospot.kospot.domain.coordinate.entity.sido.Sido;

@SidoRepository(Sido.GYEONGGI)
public interface CoordinateGyeonggiRepository extends CoordinateRepository<CoordinateGyeonggi, Long> {

}
