package com.kospot.kospot.domain.coordinate.service;

import com.kospot.kospot.domain.coordinate.aop.SidoRepository;
import com.kospot.kospot.domain.coordinate.entity.sido.Sido;
import com.kospot.kospot.domain.coordinate.repository.BaseCoordinateRepository;
import com.kospot.kospot.exception.object.domain.CoordinateHandler;
import com.kospot.kospot.exception.payload.code.ErrorStatus;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.aop.framework.AopProxyUtils;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class DynamicCoordinateRepositoryFactory {
    private final Map<Sido, BaseCoordinateRepository<?, Long>> repositoryCache;

    public DynamicCoordinateRepositoryFactory(List<BaseCoordinateRepository<?, Long>> repositories) {
        System.out.println("Total repositories: " + repositories.size());
        repositories.forEach(repo -> {
            Class<?> actualClass = AopProxyUtils.ultimateTargetClass(repo);
            System.out.println("Repository class: " + actualClass.getName());
            System.out.println("Has SidoRepository annotation: " + actualClass.isAnnotationPresent(SidoRepository.class));
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
                            return (BaseCoordinateRepository<?, Long>) repositoryInterface.cast(repo);
                        }
                ));
    }

    @SuppressWarnings("unchecked")
    public <T> BaseCoordinateRepository<T, Long> getRepository(Sido sido) {
        BaseCoordinateRepository<?, Long> repository = repositoryCache.get(sido);
        if (repository == null) {
            throw new CoordinateHandler(ErrorStatus.DYNAMIC_COORDINATE_REPOSITORY_FACTORY_NOT_FOUND);
        }
        return (BaseCoordinateRepository<T, Long>) repository;
    }

}
