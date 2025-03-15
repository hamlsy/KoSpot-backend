package com.kospot.kospot.domain.image.repository;

import com.kospot.kospot.domain.image.entity.Image;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ImageRepository extends JpaRepository<Image, Long> {
}
