package com.kospot.domain.banner.repository;

import com.kospot.domain.banner.entity.Banner;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BannerRepository extends JpaRepository<Banner, Long> {

    List<Banner> findAllByOrderByDisplayOrderAsc();

    List<Banner> findAllByIsActiveTrueOrderByDisplayOrderAsc();
}

