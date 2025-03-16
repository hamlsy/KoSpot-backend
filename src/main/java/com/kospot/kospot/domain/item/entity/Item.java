package com.kospot.kospot.domain.item.entity;

import com.kospot.kospot.domain.auditing.entity.BaseTimeEntity;
import com.kospot.kospot.domain.image.entity.Image;
import com.kospot.kospot.exception.object.domain.ItemHandler;
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

    private int stock;

    @Enumerated(EnumType.STRING)
    private ItemType itemType;

    private boolean isAvailable = true;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "image_id")
    private Image image;

    //business
    public static Item create(String name, String description, ItemType itemType, int price, int stock) {
        return Item.builder()
                .name(name)
                .description(description)
                .itemType(itemType)
                .price(price)
                .stock(stock)
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
