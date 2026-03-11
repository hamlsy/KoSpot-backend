package com.kospot.friend.infrastructure.persistence;

import com.kospot.friend.domain.entity.Friendship;
import com.kospot.friend.domain.vo.FriendshipStatus;
import com.kospot.game.domain.vo.GameMode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface FriendshipRepository extends JpaRepository<Friendship, Long> {

    Optional<Friendship> findByCanonicalPairKey(String canonicalPairKey);

    List<Friendship> findByCanonicalPairKeyIn(List<String> canonicalPairKeys);

    @Query("""
            select new com.kospot.friend.infrastructure.persistence.FriendSummaryQueryModel(
                case when f.memberLowId = :memberId then f.memberHighId else f.memberLowId end,
                m.nickname,
                image.imageUrl,
                gr.rankTier,
                gr.rankLevel,
                coalesce(gr.ratingScore, 0)
            )
            from Friendship f
            join Member m on m.id = case when f.memberLowId = :memberId then f.memberHighId else f.memberLowId end
            left join m.equippedMarkerImage image
            left join GameRank gr on gr.member.id = m.id and gr.gameMode = :gameMode
            where (f.memberLowId = :memberId or f.memberHighId = :memberId)
            and f.status = :status
            order by f.lastModifiedDate desc
            """)
    List<FriendSummaryQueryModel> findFriendSummaries(@Param("memberId") Long memberId,
            @Param("status") FriendshipStatus status,
            @Param("gameMode") GameMode gameMode);
}
