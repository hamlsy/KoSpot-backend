package com.kospot.domain.memberitem.entity;

import com.kospot.domain.auditing.entity.BaseTimeEntity;
import com.kospot.domain.item.entity.Item;
import com.kospot.domain.member.entity.Member;
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
public class MemberItem extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "memberItem_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "item_id")
    private Item item;

    private Boolean isEquipped = false;

    //business
    public void equip() {
        this.isEquipped = true;
    }

    public void unEquip() {
        this.isEquipped = false;
    }

    public static MemberItem create(Member member, Item item) {
        return MemberItem.builder()
                .member(member)
                .item(item)
                .build();
    }

}
