package com.kospot.kospot.domain.coordinate.repository;

import com.kospot.kospot.domain.coordinate.aop.SidoRepository;
import com.kospot.kospot.domain.coordinate.entity.coordinates.CoordinateUlsan;
import com.kospot.kospot.domain.coordinate.entity.sido.Sido;

@SidoRepository(Sido.ULSAN)
public interface CoordinateUlsanRepository extends CoordinateRepository<CoordinateUlsan, Long> {

}
