package com.kospot.domain.coordinate.repository.location;

import com.kospot.domain.coordinate.aop.SidoRepository;
import com.kospot.domain.coordinate.entity.coordinates.CoordinateUlsan;
import com.kospot.domain.coordinate.vo.Sido;
import com.kospot.domain.coordinate.repository.base.CoordinateRepository;

@SidoRepository(Sido.ULSAN)
public interface CoordinateUlsanRepository extends CoordinateRepository<CoordinateUlsan, Long> {

}
