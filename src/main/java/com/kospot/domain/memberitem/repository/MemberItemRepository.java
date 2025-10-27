package com.kospot.domain.memberitem.repository;

import com.kospot.domain.item.vo.ItemType;
import com.kospot.domain.member.entity.Member;
import com.kospot.domain.memberitem.entity.MemberItem;
import com.kospot.presentation.memberitem.dto.response.MemberItemResponse;
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

    @Query("select mi from MemberItem mi join fetch mi.item i " +
            "left join fetch i.image where mi.id = :id")
    Optional<MemberItem> findByIdFetchItemAndImage(@Param("id") Long id);

    @Query("select mi from MemberItem mi join fetch mi.item where mi.item.id = :itemId")
    Optional<MemberItem> findByItemIdFetchItem(@Param("itemId") Long itemId);

    // 중복 아이템 장착 방지
    @Query("select mi from MemberItem mi join " +
            "mi.item i where mi.member = :member " +
            "and i.itemType = :itemType " +
            "and mi.isEquipped = true")
    List<MemberItem> findEquippedItemByMemberAndItemType(@Param("member") Member member,
                                                         @Param("itemType") ItemType itemType);


    @Query("select new com.kospot.presentation.memberitem.dto.response.MemberItemResponse(" +
            "mi.id, mi.item.name, mi.item.description, mi.isEquipped, mi.createdDate) " +
            "from MemberItem mi join mi.item " +
            "where mi.member = :member and mi.item.itemType = :itemType")
    List<MemberItemResponse> findAllByMemberAndItemTypeFetch(@Param("member") Member member,
                                                             @Param("itemType") ItemType itemType);

    @Query("select count(mi) from MemberItem mi where mi.member = :member")
    long countByMember(@Param("member") Member member);

    @Query("select count(mi) from MemberItem mi where mi.member = :member and mi.isEquipped = true")
    long countEquippedByMember(@Param("member") Member member);

}
