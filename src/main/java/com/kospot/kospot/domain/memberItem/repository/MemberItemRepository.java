package com.kospot.kospot.domain.memberItem.repository;

import com.kospot.kospot.domain.memberItem.entity.MemberItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MemberItemRepository extends JpaRepository<MemberItem, Long> {

    @Modifying
    @Query("delete from MemberItem mi where mi.item.id = :itemId")
    void deleteAllByItemId(@Param("itemId") Long itemId);

}
