package com.kospot.kospot.domain.coordinate.repository;

import com.kospot.kospot.domain.coordinate.aop.SidoRepository;
import com.kospot.kospot.domain.coordinate.entity.coordinates.CoordinateChungbuk;
import com.kospot.kospot.domain.coordinate.entity.sido.Sido;

@SidoRepository(Sido.CHUNGBUK)
public interface CoordinateChungbukRepository extends BaseCoordinateRepository<CoordinateChungbuk, Long> {

}
