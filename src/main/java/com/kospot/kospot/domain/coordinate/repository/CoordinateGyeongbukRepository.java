package com.kospot.kospot.domain.coordinate.repository;

import com.kospot.kospot.domain.coordinate.aop.SidoRepository;
import com.kospot.kospot.domain.coordinate.entity.coordinates.CoordinateGyeongbuk;
import com.kospot.kospot.domain.coordinate.entity.sido.Sido;

@SidoRepository(Sido.GYEONGBUK)
public interface CoordinateGyeongbukRepository extends BaseCoordinateRepository<CoordinateGyeongbuk, Long> {
}

