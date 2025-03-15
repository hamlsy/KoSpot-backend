package com.kospot.kospot.domain.item.repository;

import com.kospot.kospot.domain.item.entity.Item;
import com.kospot.kospot.domain.item.entity.ItemType;
import com.kospot.kospot.domain.member.entity.Member;
import com.kospot.kospot.presentation.item.dto.response.ItemResponse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ItemRepository extends JpaRepository<Item, Long> {

    List<Item> findAllByItemType(ItemType itemType);

    @Query("select i from Item i join fetch i.image im where i.id = :id")
    Optional<Item> findByIdFetchImage(@Param("id") Long id);

    @Query("select i from Item i join fetch i.image im where i.isAvailable = true and i.itemType = :itemType")
    List<Item> findAvailableItemsByItemTypeFetchImage(@Param("itemType") ItemType itemType);

    @Query("select new com.kospot.kospot.presentation.item.dto.response.ItemResponse.ItemDto(" +
            "i.id, i.name, i.image.imageUrl, i.description, i.price, i.stock, i.image.imageUrl, " +
            "case when mi.id is not null then true else false end) " +
            "from Item i left join MemberItem mi on i.id = mi.item.id and mi.member = :member " +
            "and i.itemType = :itemType and i.aisAvailable = true")
    List<ItemResponse.ItemDto> findAvailableItemsWithOwnersByTypeFetchImage(
            @Param("member") Member member,
            @Param("itemType") ItemType itemType);
}

