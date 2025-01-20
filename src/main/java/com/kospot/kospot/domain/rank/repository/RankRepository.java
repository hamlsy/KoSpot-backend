package com.kospot.kospot.domain.rank.repository;

import com.kospot.kospot.domain.rank.entity.Rank;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RankRepository extends JpaRepository<Rank, Long> {
}
