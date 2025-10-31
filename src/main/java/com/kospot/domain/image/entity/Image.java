package com.kospot.domain.image.entity;

import com.kospot.domain.auditing.entity.BaseTimeEntity;
import com.kospot.domain.image.vo.ImageType;
import com.kospot.domain.notice.entity.Notice;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Getter
@Entity
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
public class Image extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String imagePath;
    private String imageName;
    private String s3Key;
    private String imageUrl;

    @Enumerated(EnumType.STRING)
    private ImageType imageType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "notice_id")
    private Notice notice;

    public static Image create(String imagePath, String imageName, String s3Key, String imageUrl, ImageType imageType) {
        return Image.builder()
                .imagePath(imagePath)
                .s3Key(s3Key)
                .imageName(imageName)
                .imageUrl(imageUrl)
                .imageType(imageType)
                .build();
    }

    //business
    public void updateImage(String imageName, String s3Key, String imageUrl) {
        this.s3Key = s3Key;
        this.imageName = imageName;
        this.imageUrl = imageUrl;
    }

    public void setNoticeFk(Notice notice) {
        this.notice = notice;
    }

}
