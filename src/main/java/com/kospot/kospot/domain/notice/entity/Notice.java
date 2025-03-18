package com.kospot.kospot.domain.notice.entity;


import com.kospot.kospot.domain.auditing.entity.BaseTimeEntity;
import com.kospot.kospot.domain.image.entity.Image;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.List;


@Getter
@Entity
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
public class Notice extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Lob
    private String title;

    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String content;

    @OneToMany(fetch = FetchType.LAZY)
    private List<Image> images;

    //business
    public static Notice create(String title, String content, List<Image> images) {
        return Notice.builder()
                .title(title)
                .content(content)
                .images(images)
                .build();
    }

    public void update(String title, String content) {
        this.title = title;
        this.content = content;
    }

}
