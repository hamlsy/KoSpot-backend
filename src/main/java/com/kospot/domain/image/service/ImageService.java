package com.kospot.domain.image.service;

import com.kospot.domain.image.adaptor.ImageAdaptor;
import com.kospot.domain.image.entity.Image;
import com.kospot.domain.image.entity.ImageType;
import com.kospot.domain.image.repository.ImageRepository;
import com.kospot.domain.item.entity.Item;
import com.kospot.global.service.AwsS3Service;
import com.kospot.presentation.image.dto.request.ImageRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ImageService {

    private final AwsS3Service awsS3Service;
    private final ImageRepository imageRepository;
    private final ImageAdaptor imageAdaptor;

    private static final String S3_IMAGE_PATH = "file/image/";
    private static final String S3_NOTICE_PATH = S3_IMAGE_PATH + "notice/";
    private static final String S3_ITEM_PATH = S3_IMAGE_PATH + "item/";

    public void uploadItemImage(MultipartFile file, Item item) {
        if(isValidImage(file)){
            String uploadFilePath = S3_ITEM_PATH + item.getItemType().name().toLowerCase() + "/";
            Image image = uploadImage(file, uploadFilePath, ImageType.ITEM);
            imageRepository.save(image);
        }
    }

    //todo refactoring bulk insert
    public List<Image> uploadNoticeImages(List<MultipartFile> files) {
        if(isNotValidImages(files)) {
            return null;
        }
        List<Image> images = files.stream().map(f -> uploadImage(f, S3_NOTICE_PATH, ImageType.NOTICE)).collect(Collectors.toList());
        imageRepository.saveAll(images);
        return images;
    }

    private Image uploadImage(MultipartFile file, String uploadFilePath, ImageType imageType) {
        if(isNotValidImage(file)){
            return null;
        }
        String fileName = awsS3Service.uploadImage(file, uploadFilePath);
        String s3Key = uploadFilePath + fileName;
        String fileUrl = awsS3Service.generateFileUrl(s3Key);
        return Image.create(uploadFilePath, fileName, s3Key, fileUrl, imageType);
    }

    private boolean isNotValidImages(List<MultipartFile> files) {
        return files == null;
    }

    private boolean isNotValidImage(MultipartFile file) {
        return file == null;
    }

    private boolean isValidImage(MultipartFile file) {
        return file != null;
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

    public void deleteImage(Image image) {
        awsS3Service.deleteFile(image.getS3Key());
    }

    //todo implement upload notice images, banner image, event images

}
