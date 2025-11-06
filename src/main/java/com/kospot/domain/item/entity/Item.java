package com.kospot.domain.item.entity;

import com.kospot.domain.auditing.entity.BaseTimeEntity;
import com.kospot.domain.image.entity.Image;
import com.kospot.domain.item.vo.ItemType;
import com.kospot.infrastructure.exception.object.domain.ItemHandler;
import com.kospot.infrastructure.exception.payload.code.ErrorStatus;
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

    private int stock;

    @Enumerated(EnumType.STRING)
    private ItemType itemType;

    private boolean isAvailable = true;
    // 기본 아이템 식별
    private boolean isDefault = false;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "image_id")
    private Image image;

    //business
    public static Item create(String name, String description, ItemType itemType, int price, int stock, Image image) {
        return Item.builder()
                .name(name)
                .description(description)
                .itemType(itemType)
                .price(price)
                .stock(stock)
                .isAvailable(true)
                .isDefault(false)
                .image(image)
                .build();
    }

    public static Item createDefault(String name, String description, ItemType itemType, Image image) {
        return Item.builder()
                .name(name)
                .description(description)
                .itemType(itemType)
                .price(0)
                .stock(0)
                .isAvailable(true)
                .isDefault(true)
                .image(image)
                .build();
    }

    public void updateItemInfo(String name, String description, ItemType itemType, int price, int quantity) {
        this.name = name;
        this.description = description;
        this.itemType = itemType;
        this.price = price;
        this.stock = quantity;
    }

    public void deleteFromShop() {
        this.isAvailable = false;
    }

    public void restoreToShop() {
        this.isAvailable = true;
    }

    public void purchase() {
        validateStock();
        this.stock -= 1;
    }

    // validation
    private void validateStock(){
        if(this.stock <= 0) {
            throw new ItemHandler(ErrorStatus.ITEM_OUT_OF_STOCK);
        }
    }

}
