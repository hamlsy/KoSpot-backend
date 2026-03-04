package com.kospot.application.member;

import com.kospot.domain.friend.adaptor.FriendAdaptor;
import com.kospot.domain.member.adaptor.MemberAdaptor;
import com.kospot.domain.member.entity.Member;
import com.kospot.infrastructure.annotation.usecase.UseCase;
import com.kospot.presentation.member.dto.response.SearchMemberResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@UseCase
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class SearchMembersByNicknameUseCase {

    private final MemberAdaptor memberAdaptor;

    public List<SearchMemberResponse> execute(Member member, String nickname) {
        List<Member> foundMember = memberAdaptor.queryAllByNicknameKeyword(nickname);

        // isFriend, requestSend

        return foundMember.stream().map(
                m -> SearchMemberResponse.builder()
                        .memberId(m.getId())
                        .nickname(m.getNickname())
                        .markerImageUrl(m.getEquippedMarkerImage() != null ? m.getEquippedMarkerImage().getImageUrl() : null)
                        .build()).toList();
    }


}
