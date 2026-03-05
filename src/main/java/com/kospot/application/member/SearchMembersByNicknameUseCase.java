package com.kospot.application.member;

import com.kospot.domain.friend.adaptor.FriendAdaptor;
import com.kospot.domain.friend.entity.FriendRequest;
import com.kospot.domain.friend.entity.Friendship;
import com.kospot.domain.friend.service.FriendPairService;
import com.kospot.domain.friend.vo.FriendRequestStatus;
import com.kospot.domain.friend.vo.FriendshipStatus;
import com.kospot.domain.member.adaptor.MemberAdaptor;
import com.kospot.domain.member.entity.Member;
import com.kospot.infrastructure.annotation.usecase.UseCase;
import com.kospot.presentation.member.dto.response.SearchMemberResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@UseCase
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class SearchMembersByNicknameUseCase {

    private final MemberAdaptor memberAdaptor;
    private final FriendAdaptor friendAdaptor;
    private final FriendPairService friendPairService;

    public List<SearchMemberResponse> execute(Long memberId, String nickname) {
        Member member = memberAdaptor.queryById(memberId);
        List<Member> foundMember = memberAdaptor.queryAllByNicknameKeyword(nickname);

        if (foundMember.isEmpty()) {
            return List.of();
        }

        List<String> canonicalPairKeys = foundMember.stream()
                .filter(m -> !m.getId().equals(member.getId()))
                .map(m -> friendPairService.canonicalPairKey(member.getId(), m.getId()))
                .toList();

        Map<String, Friendship> friendshipMap = friendAdaptor.queryFriendshipsByCanonicalPairs(canonicalPairKeys)
                .stream()
                .collect(Collectors.toMap(Friendship::getCanonicalPairKey, f -> f));

        Map<String, FriendRequest> requestMap = friendAdaptor.queryRequestsByCanonicalPairs(canonicalPairKeys)
                .stream()
                .collect(Collectors.toMap(FriendRequest::getCanonicalPairKey, r -> r));

        return foundMember.stream().map(m -> {
            if (m.getId().equals(member.getId())) {
                return SearchMemberResponse.builder()
                        .memberId(m.getId())
                        .nickname(m.getNickname())
                        .markerImageUrl(
                                m.getEquippedMarkerImage() != null ? m.getEquippedMarkerImage().getImageUrl() : null)
                        .isFriend(false)
                        .requestSend(false)
                        .build();
            }

            String key = friendPairService.canonicalPairKey(member.getId(), m.getId());
            Friendship friendship = friendshipMap.get(key);
            FriendRequest request = requestMap.get(key);

            boolean isFriend = friendship != null && friendship.getStatus() == FriendshipStatus.ACTIVE;
            boolean requestSend = request != null && request.getStatus() == FriendRequestStatus.PENDING
                    && request.getRequesterMemberId().equals(member.getId());

            return SearchMemberResponse.builder()
                    .memberId(m.getId())
                    .nickname(m.getNickname())
                    .markerImageUrl(
                            m.getEquippedMarkerImage() != null ? m.getEquippedMarkerImage().getImageUrl() : null)
                    .isFriend(isFriend)
                    .requestSend(requestSend)
                    .build();
        }).toList();
    }

}
