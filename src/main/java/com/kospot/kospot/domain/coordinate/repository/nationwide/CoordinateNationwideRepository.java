package com.kospot.kospot.domain.coordinate.repository.nationwide;

import com.kospot.kospot.domain.coordinate.aop.SidoRepository;
import com.kospot.kospot.domain.coordinate.entity.coordinates.CoordinateNationwide;
import com.kospot.kospot.domain.coordinate.entity.sido.Sido;
import com.kospot.kospot.domain.coordinate.repository.base.CoordinateRepository;

@SidoRepository(Sido.NATIONWIDE)
public interface CoordinateNationwideRepository extends CoordinateRepository<CoordinateNationwide, Long> {
}
