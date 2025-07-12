package com.kospot.domain.coordinate.repository.location;

import com.kospot.domain.coordinate.aop.SidoRepository;
import com.kospot.domain.coordinate.entity.coordinates.CoordinateJeonbuk;
import com.kospot.domain.coordinate.vo.Sido;
import com.kospot.domain.coordinate.repository.base.CoordinateRepository;

@SidoRepository(Sido.JEONBUK)
public interface CoordinateJeonbukRepository extends CoordinateRepository<CoordinateJeonbuk, Long> {

}
