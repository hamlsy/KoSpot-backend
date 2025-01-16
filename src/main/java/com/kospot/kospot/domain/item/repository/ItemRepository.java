package com.kospot.kospot.domain.item.repository;

import com.kospot.kospot.domain.item.entity.Item;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ItemRepository extends JpaRepository<Item, Long> {
}
