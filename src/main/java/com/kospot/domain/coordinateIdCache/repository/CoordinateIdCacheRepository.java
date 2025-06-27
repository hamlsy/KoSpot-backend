package com.kospot.domain.coordinateIdCache.repository;

import com.kospot.domain.coordinate.vo.Sido;
import com.kospot.domain.coordinateIdCache.entity.CoordinateIdCache;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CoordinateIdCacheRepository extends JpaRepository<CoordinateIdCache, Sido> {
}
