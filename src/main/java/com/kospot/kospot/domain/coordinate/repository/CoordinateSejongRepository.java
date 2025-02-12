package com.kospot.kospot.domain.coordinate.repository;

import com.kospot.kospot.domain.coordinate.aop.SidoRepository;
import com.kospot.kospot.domain.coordinate.entity.coordinates.CoordinateSejong;
import com.kospot.kospot.domain.coordinate.entity.sido.Sido;

@SidoRepository(Sido.SEJONG)
public interface CoordinateSejongRepository extends CoordinateRepository<CoordinateSejong, Long> {

}
