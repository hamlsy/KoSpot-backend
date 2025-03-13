package com.kospot.kospot.domain.image.service;

import com.kospot.kospot.domain.image.entity.Image;
import com.kospot.kospot.domain.image.entity.ImageType;
import com.kospot.kospot.domain.image.repository.ImageRepository;
import com.kospot.kospot.domain.item.entity.Item;
import com.kospot.kospot.domain.item.entity.ItemType;
import com.kospot.kospot.global.service.AwsS3Service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ImageService {

    private AwsS3Service awsS3Service;
    private ImageRepository imageRepository;

    private static final String S3_IMAGE_PATH = "file/image/";
    private static final String S3_ITEM_PATH = S3_IMAGE_PATH + "item/";

    public Image uploadItemImage(MultipartFile file, Item item){
        String uploadFilePath = S3_ITEM_PATH + item.getItemType().name().toLowerCase();
        String fileName = awsS3Service.uploadImage(file, uploadFilePath);
        Image image = Image.create(uploadFilePath, uploadFilePath + fileName, fileName);
        image.setItemEntity(item);

        return imageRepository.save(image);
    }

    //todo implement upload notice images, banner image, event images

}
