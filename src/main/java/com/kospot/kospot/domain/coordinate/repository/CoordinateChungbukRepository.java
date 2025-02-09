package com.kospot.kospot.domain.coordinate.repository;

import com.kospot.kospot.domain.coordinate.aop.SidoRepository;
import com.kospot.kospot.domain.coordinate.entity.coordinates.CoordinateChungbuk;
import com.kospot.kospot.domain.coordinate.entity.sido.Sido;
import org.springframework.stereotype.Repository;

@SidoRepository(Sido.CHUNGBUK)
public interface CoordinateChungbukRepository extends BaseCoordinateRepository<CoordinateChungbuk, Long> {

}
