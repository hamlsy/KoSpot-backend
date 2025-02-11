package com.kospot.kospot.domain.coordinate.repository;

import com.kospot.kospot.domain.coordinate.aop.SidoRepository;
import com.kospot.kospot.domain.coordinate.entity.coordinates.CoordinateJeonbuk;
import com.kospot.kospot.domain.coordinate.entity.sido.Sido;

@SidoRepository(Sido.JEONBUK)
public interface CoordinateJeonbukRepository extends CoordinateRepository<CoordinateJeonbuk, Long> {

}
