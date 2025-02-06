package com.kospot.kospot.domain.coordinate.service;

import com.kospot.kospot.domain.coordinate.entity.sido.Sido;
import com.kospot.kospot.domain.coordinate.repository.BaseCoordinateRepository;
import com.kospot.kospot.domain.coordinate.repository.CoordinateSeoulRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@RequiredArgsConstructor
public class DynamicCoordinateRepositoryFactory {
    private final Map<Sido, BaseCoordinateRepository<?, Long>> repositoryCache = new ConcurrentHashMap<>();



}
