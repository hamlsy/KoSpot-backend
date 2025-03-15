package com.kospot.kospot.domain.item.entity;

import com.kospot.kospot.domain.auditing.entity.BaseTimeEntity;
import com.kospot.kospot.domain.image.entity.Image;
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

    @Lob
    @Column(columnDefinition = "TEXT")
    private String description;

    private int price;

    private int quantity;

    @Enumerated(EnumType.STRING)
    private ItemType itemType;

    private boolean isAvailable = true;

    @OneToOne(mappedBy = "item")
    private Image image;

    //business
    public static Item create(String name, String description, ItemType itemType, int price, int quantity) {
        return Item.builder()
                .name(name)
                .description(description)
                .itemType(itemType)
                .price(price)
                .quantity(quantity)
                .build();
    }

    public void updateItemInfo(String name, String description, ItemType itemType, int price, int quantity) {
        this.name = name;
        this.description = description;
        this.itemType = itemType;
        this.price = price;
        this.quantity = quantity;
    }

    public void deleteFromShop() {
        this.isAvailable = false;
    }

    public void restoreToShop() {
        this.isAvailable = true;
    }


}
