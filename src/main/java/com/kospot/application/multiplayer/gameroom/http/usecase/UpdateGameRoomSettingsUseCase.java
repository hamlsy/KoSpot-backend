package com.kospot.application.multiplayer.gameroom.http.usecase;

import com.kospot.domain.member.entity.Member;
import com.kospot.domain.multigame.game.vo.PlayerMatchType;
import com.kospot.domain.multigame.gameRoom.adaptor.GameRoomAdaptor;
import com.kospot.domain.multigame.gameRoom.entity.GameRoom;
import com.kospot.domain.multigame.gameRoom.service.GameRoomService;
import com.kospot.domain.multigame.gameRoom.vo.GameRoomUpdateInfo;
import com.kospot.infrastructure.annotation.usecase.UseCase;
import com.kospot.infrastructure.websocket.domain.gameroom.service.GameRoomNotificationService;
import com.kospot.infrastructure.websocket.domain.gameroom.service.GameRoomRedisService;
import com.kospot.presentation.multigame.gameroom.dto.request.GameRoomRequest;
import com.kospot.presentation.multigame.gameroom.dto.response.GameRoomResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@UseCase
@RequiredArgsConstructor
@Transactional
public class UpdateGameRoomSettingsUseCase {

    private final GameRoomAdaptor gameRoomAdaptor;
    private final GameRoomService gameRoomService;
    private final GameRoomRedisService gameRoomRedisService;
    private final GameRoomNotificationService gameRoomNotificationService;

    public GameRoomResponse execute(Member host, GameRoomRequest.Update request, Long gameRoomId) {
        // host 검증
        GameRoom gameRoom = gameRoomAdaptor.queryByIdFetchHost(gameRoomId);
        gameRoom.validateHost(host);

        handleTeamSettingChanged(gameRoom, request);

        GameRoomUpdateInfo updateInfo = mapToUpdateInfo(request);
        GameRoom updatedGameRoom = gameRoomService.updateGameRoom(updateInfo, gameRoom);

        gameRoomNotificationService.notifyRoomSettingsChanged(gameRoomId.toString(), updateInfo);

        return GameRoomResponse.from();
    }

    public GameRoomUpdateInfo mapToUpdateInfo (GameRoomRequest.Update request) {
        return GameRoomUpdateInfo.builder()
                .title(request.getTitle())
                .gameModeKey(request.getGameModeKey())
                .playerMatchTypeKey(request.getPlayerMatchTypeKey())
                .privateRoom(request.isPrivateRoom())
                .password(request.getPassword())
                .build();
    }

    private void handleTeamSettingChanged(GameRoom gameRoom, GameRoomRequest.Update request) {
        PlayerMatchType requestPlayerMatchType = PlayerMatchType.fromKey(request.getPlayerMatchTypeKey());
        //todo 팀 모드 변경 분기
        if(gameRoom.getPlayerMatchType() != requestPlayerMatchType) { // 팀 변경 됐을 때
            //todo implement
            // 1. 랜덤 팀 부여


            // 2. playerList broadcast

        }
    }


}
