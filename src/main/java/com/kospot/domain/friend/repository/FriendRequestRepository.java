package com.kospot.domain.friend.repository;

import com.kospot.domain.friend.entity.FriendRequest;
import com.kospot.domain.friend.vo.FriendRequestStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface FriendRequestRepository extends JpaRepository<FriendRequest, Long> {

    Optional<FriendRequest> findByCanonicalPairKey(String canonicalPairKey);

    @Query("select fr from FriendRequest fr where fr.receiverMemberId = :receiverMemberId and fr.status = :status order by fr.createdDate desc")
    List<FriendRequest> findIncomingByReceiverAndStatus(@Param("receiverMemberId") Long receiverMemberId,
                                                        @Param("status") FriendRequestStatus status,
                                                        Pageable pageable);
}
