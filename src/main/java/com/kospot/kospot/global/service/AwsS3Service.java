package com.kospot.kospot.global.service;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.CannedAccessControlList;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.kospot.kospot.exception.object.domain.S3Handler;
import com.kospot.kospot.exception.payload.code.ErrorStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.FileHandler;

@Slf4j
@Service
@RequiredArgsConstructor
public class AwsS3Service {

    @Value("${cloud.aws.s3.bucket}")
    private String bucket;

    private final AmazonS3 amazonS3;
    private final AmazonS3Client amazonS3Client;

    private static final String LOCAL_FILE_PATH = "resources/file/dump";

    public String uploadImage(MultipartFile image) {
        validateFileExtension(image.getOriginalFilename());
        String fileName = createFileName(image);

        try {
            File uploadFile = uploadLocalFile(image).orElseThrow(
                    () ->  new S3Handler(ErrorStatus.FILE_INVALID_EXTENSION)
            );
            amazonS3Client.putObject(new PutObjectRequest(bucket, fileName, uploadFile).withCannedAcl(
                    CannedAccessControlList.PublicRead));
            removeNewFile(uploadFile);
        } catch (IOException e) {
            throw new S3Handler(ErrorStatus.FILE_UPLOAD_FAILED);
        }

        return fileName;
    }

    private String createFileName(MultipartFile multipartFile) {
        return UUID.randomUUID().toString().substring(0, 10) + multipartFile.getOriginalFilename();
    }

    private void validateFileExtension(String fileName) {
        int lastDotIndex = fileName.lastIndexOf(".");
        if (lastDotIndex == -1) {
            throw new S3Handler(ErrorStatus.FILE_EXTENSION_NOT_FOUND);
        }

        String extention = fileName.substring(lastDotIndex + 1).toLowerCase();
        List<String> allowedExtentionList = Arrays.asList("jpg", "jpeg", "png", "gif");

        if (!allowedExtentionList.contains(extention)) {
            throw new S3Handler(ErrorStatus.FILE_INVALID_EXTENSION);
        }
    }

    private Optional<File> uploadLocalFile(MultipartFile multipartFile) throws IOException{
        File convertFile = new File(LOCAL_FILE_PATH + multipartFile.getOriginalFilename());
        if(convertFile.createNewFile()){
            try (FileOutputStream fos = new FileOutputStream(convertFile)) { // FileOutputStream 데이터를 파일에 바이트 스트림으로 저장하기 위함
                fos.write(multipartFile.getBytes());
            }
            return Optional.of(convertFile);
        }
        return Optional.empty();
    }

    private void removeNewFile(File file){
        if (file.delete()) {
            log.info("File delete success");
            return;
        }
        log.info("File delete fail");
    }
}
