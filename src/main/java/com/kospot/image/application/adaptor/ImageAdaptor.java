package com.kospot.image.application.adaptor;

import com.kospot.image.domain.entity.Image;
import com.kospot.image.infrastructure.persistence.ImageRepository;
import com.kospot.common.exception.object.domain.S3Handler;
import com.kospot.common.exception.payload.code.ErrorStatus;
import com.kospot.common.annotation.adaptor.Adaptor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Adaptor
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ImageAdaptor {

    private final ImageRepository repository;

    public Image queryById(Long id){
        return repository.findById(id).orElseThrow(
                () -> new S3Handler(ErrorStatus.IMAGE_NOT_FOUND)
        );
    }

}
