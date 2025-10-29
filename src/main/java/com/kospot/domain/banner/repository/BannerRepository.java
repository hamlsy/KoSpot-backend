package com.kospot.domain.banner.repository;

import com.kospot.domain.banner.entity.Banner;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BannerRepository extends JpaRepository<Banner, Long> {

    @Query("SELECT b FROM Banner b LEFT JOIN FETCH b.image WHERE b.id = :id")
    Optional<Banner> findByIdFetchImage(@Param("id") Long id);

    @Query("SELECT b FROM Banner b LEFT JOIN FETCH b.image ORDER BY b.displayOrder ASC")
    List<Banner> findAllFetchImage();

    List<Banner> findAllByOrderByDisplayOrderAsc();

    List<Banner> findAllByIsActiveTrueOrderByDisplayOrderAsc();
}

