package com.kospot.infrastructure.redis.domain.member.service;

import com.kospot.infrastructure.redis.domain.member.dao.MemberProfileRedisRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MemberProfileRedisService {

    private static final String MEMBER_PROFILE_KEY = "member:%s:profile";
    private static final long DEFAULT_EXPIRE_HOURS = 24; // 필요하면 바꾸기

    private final MemberProfileRedisRepository memberProfileRedisRepository;

    /**
     * 프로필 저장(등록/초기화)
     */
    public void saveProfile(Long memberId, String nickname, String markerImageUrl) {
        String key = buildKey(memberId);
        memberProfileRedisRepository.saveProfile(key, nickname, markerImageUrl, DEFAULT_EXPIRE_HOURS);
    }

    /**
     * 닉네임만 변경
     */
    public void updateNickname(Long memberId, String nickname) {
        String key = buildKey(memberId);
        memberProfileRedisRepository.saveField(key, "nickname", nickname);
        // 필요하면 여기서 expire 연장
        // memberProfileRedisRepository.expire(key, DEFAULT_EXPIRE_HOURS); 이런 식으로 추가 메서드 만들 수도 있음
    }


    /**
     * 마커이미지만 변경
     */
    public void updateMarkerImageUrl(Long memberId, String markerImageUrl) {
        String key = buildKey(memberId);
        memberProfileRedisRepository.saveField(key, "markerImageUrl", markerImageUrl);
    }

    /**
     * 프로필 삭제
     */
    public void deleteProfile(Long memberId) {
        String key = buildKey(memberId);
        memberProfileRedisRepository.deleteProfile(key);
    }

    private String buildKey(Long memberId) {
        return String.format(MEMBER_PROFILE_KEY, memberId);
    }
}
