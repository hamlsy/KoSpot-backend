package com.kospot.domain.coordinate.repository.location;

import com.kospot.domain.coordinate.aop.SidoRepository;
import com.kospot.domain.coordinate.entity.coordinates.CoordinateGyeongbuk;
import com.kospot.domain.coordinate.vo.Sido;
import com.kospot.domain.coordinate.repository.base.CoordinateRepository;

@SidoRepository(Sido.GYEONGBUK)
public interface CoordinateGyeongbukRepository extends CoordinateRepository<CoordinateGyeongbuk, Long> {
}

