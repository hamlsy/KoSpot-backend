package com.kospot.kospot.domain.image.adaptor;

import com.kospot.kospot.domain.image.entity.Image;
import com.kospot.kospot.domain.image.repository.ImageRepository;
import com.kospot.kospot.exception.object.domain.S3Handler;
import com.kospot.kospot.exception.payload.code.ErrorStatus;
import com.kospot.kospot.global.annotation.adaptor.Adaptor;
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
