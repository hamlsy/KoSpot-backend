package com.kospot.domain.image.repository;

import com.kospot.domain.image.entity.Image;
import com.kospot.domain.image.vo.ImageType;
import com.kospot.domain.item.vo.ImageStatus;
import com.kospot.domain.member.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface ImageRepository extends JpaRepository<Image, Long> {

    @Query("""
        select i
        from Image i
        where i.imageType = :type
          and i.imageStatus = :status
          and i.s3Key in :s3Keys
    """)
    List<Image> findTempNoticeImagesByS3Keys(
            @Param("type") ImageType type,
            @Param("status") ImageStatus status,
            @Param("s3Keys") Collection<String> s3Keys
    );
}
