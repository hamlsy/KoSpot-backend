package com.kospot.domain.coordinate.service;

import com.kospot.domain.coordinate.aop.SidoRepository;
import com.kospot.domain.coordinate.vo.Sido;
import com.kospot.domain.coordinate.repository.base.CoordinateRepository;
import com.kospot.infrastructure.exception.object.domain.CoordinateHandler;
import com.kospot.infrastructure.exception.payload.code.ErrorStatus;
import org.springframework.aop.framework.AopProxyUtils;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class DynamicCoordinateRepositoryFactory {
    private final Map<Sido, CoordinateRepository<?, Long>> repositoryCache;

    public DynamicCoordinateRepositoryFactory(List<CoordinateRepository<?, Long>> repositories) {
        System.out.println("Total repositories: " + repositories.size());
        repositories.forEach(repo -> {
            Class<?> actualClass = AopProxyUtils.ultimateTargetClass(repo);
            if (actualClass.isAnnotationPresent(SidoRepository.class)) {
                System.out.println("Sido value: " + actualClass.getAnnotation(SidoRepository.class).value());
            }
        });
        repositoryCache = repositories.stream()
                .filter(repo -> Arrays.stream(repo.getClass().getInterfaces())
                        .anyMatch(iface -> AnnotationUtils.findAnnotation(iface, SidoRepository.class) != null))
                .collect(Collectors.toMap(
                        repo -> Arrays.stream(repo.getClass().getInterfaces())
                                .filter(iface -> AnnotationUtils.findAnnotation(iface, SidoRepository.class) != null)
                                .findFirst()
                                .map(iface -> AnnotationUtils.findAnnotation(iface, SidoRepository.class).value())
                                .orElseThrow(),
                        repo -> {
                            Class<?> repositoryInterface = Arrays.stream(repo.getClass().getInterfaces())
                                    .filter(iface -> AnnotationUtils.findAnnotation(iface, SidoRepository.class) != null)
                                    .findFirst()
                                    .orElseThrow();
                            return (CoordinateRepository<?, Long>) repositoryInterface.cast(repo);
                        }
                ));
    }

    @SuppressWarnings("unchecked")
    public <T> CoordinateRepository<T, Long> getRepository(Sido sido) {
        CoordinateRepository<?, Long> repository = repositoryCache.get(sido);
        if (repository == null) {
            throw new CoordinateHandler(ErrorStatus.DYNAMIC_COORDINATE_REPOSITORY_FACTORY_NOT_FOUND);
        }
        return (CoordinateRepository<T, Long>) repository;
    }

}
