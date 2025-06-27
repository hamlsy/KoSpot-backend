package com.kospot.infrastructure.auth.service;

import com.kospot.domain.member.entity.Member;
import com.kospot.domain.member.vo.Role;
import com.kospot.domain.member.repository.MemberRepository;
import com.kospot.infrastructure.auth.domain.CustomOAuthUser;
import com.kospot.infrastructure.auth.domain.OAuth2Attributes;
import com.kospot.infrastructure.auth.vo.SocialType;
import com.kospot.infrastructure.auth.dto.OAuth2UserInfo;
import com.kospot.infrastructure.auth.utils.OAuth2Utils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final MemberRepository memberRepository;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);
        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        String userNameAttributeName = userRequest.getClientRegistration().getProviderDetails()
                .getUserInfoEndpoint().getUserNameAttributeName();
        SocialType socialType = OAuth2Utils.getSocialType(registrationId);

        log.info("registrationId={}", registrationId);
        log.info("userNameAttributeName={}", userNameAttributeName);

        // 소셜에서 전달받은 정보를 가진 OAuth2User 에서 Map 을 추출하여 OAuth2Attribute 를 생성
        Map<String, Object> attributes = oAuth2User.getAttributes();

        // 내부에서 OAuth2UserInfo 생성과 함께 OAuth2Attributes 를 생성해서 반환
        OAuth2Attributes oauth2Attributes = OAuth2Attributes.of(socialType, userNameAttributeName, attributes);
        OAuth2UserInfo oauth2UserInfo = oauth2Attributes.getOAuth2UserInfo();

        // 값 추출
        String socialId = oauth2UserInfo.getSocialId();
        String email = oauth2UserInfo.getEmail();

        log.info("socialId={}", socialId);
        log.info("email={}", email);

        String username = registrationId + "_" + socialId;
        Optional<Member> targetMember = memberRepository.findByUsername(username);
        Member member = targetMember.orElseGet(() -> signupSocialMember(username, email));

        return new CustomOAuthUser(member,
                Collections.singleton(new SimpleGrantedAuthority(member.getRole().getName())),
                attributes);
    }

    private Member signupSocialMember(String username, String email) {
        String nickname = "kospot_" + UUID.randomUUID().toString().substring(0, 8); // 임시 닉네임 생성
        Member member = Member.builder()
                .username(username)
                .nickname(nickname)
                .email(email)
                .role(Role.USER)
                .build();
        return memberRepository.save(member);
    }
}