package com.kospot.member.infrastructure.redis.adaptor;

import com.kospot.member.application.adaptor.MemberAdaptor;
import com.kospot.member.domain.entity.Member;
import com.kospot.member.infrastructure.redis.dao.MemberProfileRedisRepository;
import com.kospot.member.infrastructure.redis.service.MemberProfileRedisService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class MemberProfileRedisAdaptor {

    private static final String MEMBER_PROFILE_KEY = "member:%s:profile";

    private final MemberAdaptor memberAdaptor;
    private final MemberProfileRedisService memberProfileRedisService;
    private final MemberProfileRedisRepository memberProfileRedisRepository;

    public MemberProfileView findProfile(Long memberId) {
        String key = buildKey(memberId);
        Map<Object, Object> data = memberProfileRedisRepository.findProfile(key);
        if (data == null || data.isEmpty()) {
            Member member = memberAdaptor.queryByIdFetchMarkerImage(memberId);
            MemberProfileView profileView = new MemberProfileView(memberId, member.getNickname(), member.getEquippedMarkerImage().getImageUrl());
            memberProfileRedisService.saveProfile(memberId, profileView.nickname, profileView.markerImageUrl);
            return profileView;
        }
        String nickname = (String) data.get("nickname");
        String markerImageUrl = (String) data.get("markerImageUrl");
        return new MemberProfileView(memberId, nickname, markerImageUrl);
    }

    public String findNickname(Long memberId) {
        String key = buildKey(memberId);
        Object value = memberProfileRedisRepository.findField(key, "nickname");
        return value != null ? value.toString() : null;
    }

    public String findMarkerImageUrl(Long memberId) {
        String key = buildKey(memberId);
        Object value = memberProfileRedisRepository.findField(key, "markerImageUrl");
        return value != null ? value.toString() : null;
    }

    private String buildKey(Long memberId) {
        return String.format(MEMBER_PROFILE_KEY, memberId);
    }

    // 조회 결과용 간단 DTO
    public record MemberProfileView(Long memberId, String nickname, String markerImageUrl) {}
}
