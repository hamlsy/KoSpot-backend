package com.kospot.kospot.domain.coordinate.repository.base;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.NoRepositoryBean;

@NoRepositoryBean
public interface CoordinateRepository<T, ID> extends JpaRepository<T, ID> {
    @Query("SELECT MAX(c.id) FROM #{#entityName} c")
    Long findMaxId();

}
