package com.kospot.domain.coordinate.repository.location;

import com.kospot.domain.coordinate.aop.SidoRepository;
import com.kospot.domain.coordinate.entity.coordinates.CoordinateSeoul;
import com.kospot.domain.coordinate.vo.Sido;
import com.kospot.domain.coordinate.repository.base.CoordinateRepository;

@SidoRepository(Sido.SEOUL)
//@Repository
public interface CoordinateSeoulRepository extends CoordinateRepository<CoordinateSeoul, Long> {

}
