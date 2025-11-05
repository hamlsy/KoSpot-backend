package com.kospot.domain.member.service;

import com.kospot.domain.member.entity.Member;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class MemberService {

    private void markVisited(Member member) {
        if(member.isFirstVisited()) {
            member.markVisited();
        }
    }

    public void setNickname(Member member, String nickname) {
        markVisited(member);
        member.setNickname(nickname);
    }

    public void updateNickname(Member member, String nickname) {
        member.setNickname(nickname);
    }

}
