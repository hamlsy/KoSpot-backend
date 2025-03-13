package com.kospot.kospot.domain.item.repository;

import com.kospot.kospot.domain.item.entity.Item;
import com.kospot.kospot.domain.item.entity.ItemType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ItemRepository extends JpaRepository<Item, Long> {

    List<Item> findAllByItemType(ItemType itemType);

    @Query("select i from Item i join fetch i.Image where i.id = :id")
    Optional<Item> findByIdFetchImage(@Param("id") Long id);
}

