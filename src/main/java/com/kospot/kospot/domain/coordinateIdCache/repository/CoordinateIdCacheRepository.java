package com.kospot.kospot.domain.coordinateIdCache.repository;

import com.kospot.kospot.domain.coordinate.entity.sido.Sido;
import com.kospot.kospot.domain.coordinateIdCache.entity.CoordinateIdCache;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CoordinateIdCacheRepository extends JpaRepository<CoordinateIdCache, Sido> {
}
