package com.kospot.domain.coordinateidcache.repository;

import com.kospot.domain.coordinate.vo.Sido;
import com.kospot.domain.coordinateidcache.entity.CoordinateIdCache;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CoordinateIdCacheRepository extends JpaRepository<CoordinateIdCache, Sido> {
}
