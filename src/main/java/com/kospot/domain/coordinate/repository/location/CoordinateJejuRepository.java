package com.kospot.domain.coordinate.repository.location;

import com.kospot.domain.coordinate.aop.SidoRepository;
import com.kospot.domain.coordinate.entity.coordinates.CoordinateJeju;
import com.kospot.domain.coordinate.entity.sido.Sido;
import com.kospot.domain.coordinate.repository.base.CoordinateRepository;

@SidoRepository(Sido.JEJU)
public interface CoordinateJejuRepository extends CoordinateRepository<CoordinateJeju, Long> {

}
