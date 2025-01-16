package com.kospot.kospot.domain.memberItem.repository;

import com.kospot.kospot.domain.memberItem.entity.MemberItem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MemberItemRepository extends JpaRepository<MemberItem, Long> {
}
