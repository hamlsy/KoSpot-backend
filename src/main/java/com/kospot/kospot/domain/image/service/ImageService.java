package com.kospot.kospot.domain.image.service;

import com.kospot.kospot.domain.image.adaptor.ImageAdaptor;
import com.kospot.kospot.domain.image.entity.Image;
import com.kospot.kospot.domain.image.entity.ImageType;
import com.kospot.kospot.domain.image.repository.ImageRepository;
import com.kospot.kospot.domain.item.entity.Item;
import com.kospot.kospot.domain.item.entity.ItemType;
import com.kospot.kospot.global.service.AwsS3Service;
import com.kospot.kospot.presentation.image.dto.request.ImageRequest;
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

    private final AwsS3Service awsS3Service;
    private final ImageRepository imageRepository;
    private final ImageAdaptor imageAdaptor;

    private static final String S3_IMAGE_PATH = "file/image/";
    private static final String S3_ITEM_PATH = S3_IMAGE_PATH + "item/";

    public void uploadItemImage(MultipartFile file, Item item) {
        String uploadFilePath = S3_ITEM_PATH + item.getItemType().name().toLowerCase() + "/";
        String fileName = awsS3Service.uploadImage(file, uploadFilePath);
        String s3Key = uploadFilePath + fileName;
        String fileUrl = awsS3Service.generateFileUrl(s3Key);
        Image image = Image.create(uploadFilePath, fileName, s3Key, fileUrl, ImageType.ITEM);
        image.setItemEntity(item);

        imageRepository.save(image);
    }

    public void updateImage(ImageRequest.Update request) {
        Image image = imageAdaptor.queryById(request.getImageId());

        //delete
        awsS3Service.deleteFile(image.getS3Key());

        //upload
        String newImageName = awsS3Service.uploadImage(request.getNewImage(), image.getImagePath());
        String newS3Key = image.getImagePath() + newImageName;
        String newImageUrl = awsS3Service.generateFileUrl(newS3Key);

        //update
        image.updateImage(newImageName, newS3Key, newImageUrl);

    }

    //todo implement upload notice images, banner image, event images

}
