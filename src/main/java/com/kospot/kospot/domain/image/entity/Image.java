package com.kospot.kospot.domain.image.entity;

import com.kospot.kospot.domain.auditing.entity.BaseTimeEntity;
import com.kospot.kospot.domain.item.entity.Item;
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

    private String fileUrl;
    private String s3Key;
    private String fileName;

    @Enumerated(EnumType.STRING)
    private ImageType imageType;

    @OneToOne(fetch=FetchType.LAZY)
    @JoinColumn(name = "item_id")
    private Item item;

    //todo Notice, Event <- manny to one, Banner <- one to one


    public static Image create(String fileUrl, String s3Key, String fileName){
        return Image.builder()
                .fileUrl(fileUrl)
                .s3Key(s3Key)
                .fileName(fileName)
                .build();
    }

    //business
    public void setItemEntity(Item item){
        this.item = item;
    }

}
