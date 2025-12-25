package com.kospot.infrastructure.s3.resolver;

import com.amazonaws.services.s3.AmazonS3Client;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class S3KeyResolver {

    @Value("${cloud.aws.s3.bucket}")
    private String bucket;

    @Value("${cloud.aws.region.static}")
    private String staticRegion;

    private static final String S3_AWS_STATIC_PATH = "https://%s.s3.%s.amazonaws.com/";

    public String toS3Key(String fileUrl) {
        if (fileUrl == null) return null;
        String baseUrl = String.format(S3_AWS_STATIC_PATH, bucket, staticRegion);
        if (fileUrl.startsWith(baseUrl)) {
            return fileUrl.substring(baseUrl.length());
        }

        return null; // 우리 파일이 아니면 null 처리(보안)
    }

}