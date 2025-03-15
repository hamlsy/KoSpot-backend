package com.kospot.kospot.domain.memberItem.repository;

import com.kospot.kospot.domain.item.entity.ItemType;
import com.kospot.kospot.domain.member.entity.Member;
import com.kospot.kospot.domain.memberItem.entity.MemberItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface MemberItemRepository extends JpaRepository<MemberItem, Long> {

    @Modifying
    @Query("delete from MemberItem mi where mi.item.id = :itemId")
    void deleteAllByItemId(@Param("itemId") Long itemId);

    @Query("select mi from MemberItem mi join fetch mi.item where mi.id = :id")
    Optional<MemberItem> findByIdFetchItem(@Param("id") Long id);

    // 중복 아이템 장착 방지
    @Query("select mi from MemberItem mi join " +
            "mi.item i where mi.member = :member " +
            "and i.itemType = :itemType " +
            "and mi.isEquipped = true")
    List<MemberItem> findEquippedItemByMemberAndItemType(@Param("member") Member member,
                                                         @Param("itemType") ItemType itemType);

}
