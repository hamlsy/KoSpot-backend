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

    public void markVisited(Member member) {
        if(!member.isFirstVisited()) {
            member.markVisited();
        }
    }

}
