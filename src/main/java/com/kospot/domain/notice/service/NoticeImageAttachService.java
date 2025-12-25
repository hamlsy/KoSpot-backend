package com.kospot.domain.notice.service;

import com.kospot.domain.image.entity.Image;
import com.kospot.domain.image.repository.ImageRepository;
import com.kospot.domain.image.vo.ImageType;
import com.kospot.domain.item.vo.ImageStatus;
import com.kospot.domain.member.entity.Member;
import com.kospot.domain.notice.entity.Notice;
import com.kospot.domain.notice.utils.MarkdownImageExtractor;
import com.kospot.infrastructure.s3.resolver.S3KeyResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class NoticeImageAttachService {
    private final ImageRepository imageRepository;
    private final S3KeyResolver s3KeyResolver;

    //todo batch update
    public List<Image> attachImagesFromContent(Notice notice, String contentMd) {
        // 1) md에서 url 추출
        Set<String> urls = MarkdownImageExtractor.extractUrls(contentMd);

        // 2) url -> s3Key 변환(우리 도메인만)
        Set<String> s3Keys = urls.stream()
                .map(s3KeyResolver::toS3Key)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        if (s3Keys.isEmpty()) return List.of();

        // 3) TEMP + NOTICE_CONTENT + uploader 조건으로만 조회(보안)
        List<Image> images = imageRepository.findTempNoticeImagesByS3Keys(
                ImageType.NOTICE, ImageStatus.TEMP, s3Keys
        );

        // 4) Notice에 FK 연결 + status 확정
        images.forEach(i -> i.attachToNotice(notice)); // 아래에 메서드 예시

        return images;
    }
}
