package com.kospot.kospot.domain.coordinate.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.NoRepositoryBean;

@NoRepositoryBean
public interface BaseCoordinateRepository<T, ID> extends JpaRepository<T, ID> {

}
