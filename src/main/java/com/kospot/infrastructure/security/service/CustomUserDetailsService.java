package com.kospot.infrastructure.security.service;

import com.kospot.domain.member.adaptor.MemberAdaptor;
import com.kospot.domain.member.entity.Member;
import com.kospot.infrastructure.security.vo.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class CustomUserDetailsService implements UserDetailsService {
    private final MemberAdaptor memberAdaptor;

    @Override
    public UserDetails loadUserByUsername(String memberId) throws UsernameNotFoundException {
        Member member = memberAdaptor.queryById(Long.parseLong(memberId));
        return CustomUserDetails.from(member);
    }

}