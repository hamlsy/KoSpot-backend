package com.kospot.item.infrastructure.persistence;

import com.kospot.item.domain.entity.Item;
import com.kospot.item.domain.vo.ItemType;
import com.kospot.member.domain.entity.Member;
import com.kospot.item.presentation.dto.response.ItemResponse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ItemRepository extends JpaRepository<Item, Long> {

    @Query("""
            select i from Item i
            join fetch i.image
            where i.itemType = :itemType and i.isDefault = true
            """)
    Optional<Item> findDefaultItemByType(@Param("itemType") ItemType itemType);

    List<Item> findAllByItemType(ItemType itemType);

    @Query("select i from Item i join fetch i.image im where i.id = :id")
    Optional<Item> findByIdFetchImage(@Param("id") Long id);

    @Query("select i from Item i join fetch i.image im where i.isAvailable = true and i.itemType = :itemType")
    List<Item> findAvailableItemsByItemTypeFetchImage(@Param("itemType") ItemType itemType);

    @Query("""
        select new com.kospot.item.presentation.dto.response.ItemResponse(
          i.id, i.name, i.description, i.price, i.stock, img.imageUrl,
          (case when exists (
            select 1 from MemberItem mi
            where mi.item = i and mi.member = :member
          ) then true else false end),
          (select min(mi.id) from MemberItem mi where mi.item = i and mi.member = :member),
          (case when exists (
            select 1 from MemberItem mi
            where mi.item = i and mi.member = :member and mi.isEquipped = true
          ) then true else false end)
        )
        from Item i
        left join i.image img
        where i.itemType = :itemType and i.isAvailable = true
    """)
    List<ItemResponse> findAvailableItemsWithOwnersByTypeFetchImage(
            @Param("member") Member member,
            @Param("itemType") ItemType itemType);
}
