package com.kospot.kospot.domain.coordinate.service;

import com.kospot.kospot.domain.coordinate.aop.SidoRepository;
import com.kospot.kospot.domain.coordinate.entity.sido.Sido;
import com.kospot.kospot.domain.coordinate.repository.BaseCoordinateRepository;
import com.kospot.kospot.domain.coordinate.repository.CoordinateSeoulRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class DynamicCoordinateRepositoryFactory {
    private final Map<Sido, BaseCoordinateRepository<?, Long>> repositoryCache;

    public DynamicCoordinateRepositoryFactory(List<BaseCoordinateRepository<?, Long>> repositories) {
        repositoryCache = repositories.stream()
                .filter(repo -> repo.getClass().isAnnotationPresent(SidoRepository.class))
                .collect(Collectors.toMap(
                        repo -> repo.getClass().getAnnotation(SidoRepository.class).value(),
                        repo -> repo
                ));
    }

    @SuppressWarnings("unchecked")
    public <T> BaseCoordinateRepository<T, Long> getRepository(Sido sido) {
        BaseCoordinateRepository<?, Long> repository = repositoryCache.get(sido);
        //todo refactoring exception
        if (repository == null) {
            throw new IllegalArgumentException("Repository not found for sido: " + sido);
        }
        return (BaseCoordinateRepository<T, Long>) repository;
    }

}
