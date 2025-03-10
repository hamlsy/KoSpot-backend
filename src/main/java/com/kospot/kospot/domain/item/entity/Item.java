package com.kospot.kospot.domain.item.entity;

import com.kospot.kospot.domain.auditing.entity.BaseTimeEntity;
import com.kospot.kospot.domain.member.entity.Member;
import com.kospot.kospot.domain.member.entity.Role;
import com.kospot.kospot.domain.memberItem.entity.MemberItem;
import com.kospot.kospot.exception.object.domain.MemberHandler;
import com.kospot.kospot.exception.payload.code.ErrorStatus;
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
public class Item extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "item_id")
    private Long id;

    private String name;

    private String imageUrl;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String description;

    private int price;

    @Enumerated(EnumType.STRING)
    private ItemType itemType;

    private boolean isAvailable = true;

    //business
    public static Item create(String name, String description, ItemType itemType, int price, String imageUrl) {
        return Item.builder()
                .name(name)
                .imageUrl(imageUrl)
                .description(description)
                .price(price)
                .itemType(itemType)
                .build();
    }

    public void deleteFromShop(){
        this.isAvailable = false;
    }

    public void restoreToShop(){
        this.isAvailable = true;
    }


}
