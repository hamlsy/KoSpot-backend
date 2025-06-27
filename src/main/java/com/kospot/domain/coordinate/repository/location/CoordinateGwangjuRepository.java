package com.kospot.domain.coordinate.repository.location;

import com.kospot.domain.coordinate.aop.SidoRepository;
import com.kospot.domain.coordinate.entity.coordinates.CoordinateGwangju;
import com.kospot.domain.coordinate.vo.Sido;
import com.kospot.domain.coordinate.repository.base.CoordinateRepository;

@SidoRepository(Sido.GWANGJU)
public interface CoordinateGwangjuRepository extends CoordinateRepository<CoordinateGwangju, Long> {

}
