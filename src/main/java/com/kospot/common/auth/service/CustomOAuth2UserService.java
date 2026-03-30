package com.kospot.common.auth.service;

import com.kospot.member.application.usecase.RegisterSocialMemberUseCase;
import com.kospot.member.domain.entity.Member;
import com.kospot.member.domain.vo.AuthProvider;
import com.kospot.member.infrastructure.persistence.MemberRepository;
import com.kospot.common.auth.domain.CustomOAuthUser;
import com.kospot.common.auth.domain.OAuth2Attributes;
import com.kospot.common.auth.vo.SocialType;
import com.kospot.common.auth.dto.OAuth2UserInfo;
import com.kospot.common.auth.utils.OAuth2Utils;
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

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final MemberRepository memberRepository;
    private final RegisterSocialMemberUseCase registerSocialMemberUseCase;

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

        AuthProvider authProvider = switch (socialType) {
            case GOOGLE -> AuthProvider.GOOGLE;
            case NAVER -> AuthProvider.NAVER;
            case KAKAO -> AuthProvider.KAKAO;
        };

        // 1. username 기준 조회 — 기존 소셜 유저
        Optional<Member> byUsername = memberRepository.findByUsername(username);
        if (byUsername.isPresent()) {
            return new CustomOAuthUser(byUsername.get(),
                    Collections.singleton(new SimpleGrantedAuthority(byUsername.get().getRole().getName())),
                    attributes);
        }

        // 2. email 기준 조회 — 동일 이메일로 이미 가입된 계정 (LOCAL 또는 다른 소셜)
        if (email != null) {
            Optional<Member> byEmail = memberRepository.findByEmail(email);
            if (byEmail.isPresent()) {
                return new CustomOAuthUser(byEmail.get(),
                        Collections.singleton(new SimpleGrantedAuthority(byEmail.get().getRole().getName())),
                        attributes);
            }
        }

        // 3. 신규 소셜 가입
        Member newMember = registerSocialMemberUseCase.execute(username, email, authProvider);
        return new CustomOAuthUser(newMember,
                Collections.singleton(new SimpleGrantedAuthority(newMember.getRole().getName())),
                attributes);
    }

}