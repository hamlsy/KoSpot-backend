package com.kospot.domain.notice.entity;


import com.kospot.domain.auditing.entity.BaseTimeEntity;
import com.kospot.domain.image.entity.Image;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.ArrayList;
import java.util.List;


@Getter
@Entity
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Notice extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 200, nullable = false)
    private String title;

    @Lob
    @Column(columnDefinition = "LONGTEXT", nullable = false)
    private String contentMd;     // 마크다운 원문

    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String contentHtml;   // (선택) 렌더 결과 캐시

    @OneToMany(mappedBy = "notice", fetch = FetchType.LAZY)
    private List<Image> images = new ArrayList<>();

    //business
    public static Notice create(String title, String contentMd, String contentHtml) {
        return Notice.builder()
                .title(title)
                .contentMd(contentMd)
                .contentHtml(contentHtml)
                .build();
    }

    public void update(String title, String contentMd, String contentHtml) {
        this.title = title;
        this.contentMd = contentMd;
        this.contentHtml = contentHtml;
    }

    public void addImages(List<Image> images) {
        images.forEach(this::addImage);
    }

    private void addImage(Image image) {
        this.images.add(image);
        image.setNoticeFk(this);
    }

}
