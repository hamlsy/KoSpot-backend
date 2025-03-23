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

    @Lob
    private String title;

    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String content;

    @OneToMany(mappedBy = "notice", fetch = FetchType.LAZY)
    private List<Image> images = new ArrayList<>();

    //business
    public static Notice create(String title, String content) {
        return Notice.builder()
                .title(title)
                .content(content)
                .build();
    }

    public void update(String title, String content) {
        this.title = title;
        this.content = content;
    }

    public void addImages(List<Image> images) {
        images.forEach(this::addImage);
    }

    private void addImage(Image image) {
        images.add(image);
        image.setNoticeFk(this);
    }

}
