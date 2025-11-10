package com.kospot.application.multi.access.service;

import com.kospot.domain.member.entity.Member;
import com.kospot.presentation.multi.access.dto.response.GameAccessResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class GameAccessService {

    public GameAccessResponse checkAccess(Member member, String roomId) {
        // 방 존재 확인

        // 게임 상태 확인

        // 게임 진행 중인 경우

        // 참여자인 경우 게임 정보 반환

        //대기 중인 경우, 내 roomId 확인 및 redis 확인

        return null;
    }

}
