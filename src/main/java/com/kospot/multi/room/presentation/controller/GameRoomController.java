package com.kospot.multi.room.presentation.controller;


import com.kospot.multi.game.application.usecase.NotifyStartGameUseCase;
import com.kospot.multi.room.application.usecase.*;
import com.kospot.common.annotation.adsense.BotSuccess;
import com.kospot.common.exception.payload.code.SuccessStatus;
import com.kospot.common.exception.payload.dto.ApiResponseDto;
import com.kospot.common.security.aop.CurrentMember;
import com.kospot.multi.room.presentation.dto.request.GameRoomRequest;
import com.kospot.multi.room.presentation.dto.response.FindGameRoomResponse;
import com.kospot.multi.room.presentation.dto.response.GameRoomDetailResponse;
import com.kospot.multi.room.presentation.dto.response.GameRoomResponse;
import com.kospot.multi.game.presentation.dto.response.MultiGameResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@Slf4j
@RestController
@RequiredArgsConstructor
@ApiResponse(responseCode = "2000", description = "OK")
@Tag(name = "GameRoom Api", description = "멀티 게임 방 API")
@RequestMapping("/rooms")
public class GameRoomController {

    private final FindAllGameRoomUseCase findAllGameRoomUseCase;
    private final GetGameRoomDetailUseCase getGameRoomDetailUseCase;
    private final CreateGameRoomUseCase createGameRoomUseCase;
    private final UpdateGameRoomSettingsUseCase updateGameRoomSettingsUseCase;
    private final JoinGameRoomUseCase joinGameRoomUseCase;
    private final LeaveGameRoomUseCase leaveGameRoomUseCase;
    private final KickPlayerUseCase kickPlayerUseCase;

    //broadcast
    private final NotifyStartGameUseCase notifyStartGameUseCase;

    @Operation(summary = "게임 방 전체 조회", description = "멀티 게임 방을 전체 조회합니다.")
    @BotSuccess
    @GetMapping
    public ApiResponseDto<List<FindGameRoomResponse>> findAllRooms(@RequestParam("page") int page) {
        return ApiResponseDto.onSuccess(findAllGameRoomUseCase.execute(page));
    }

    @Operation(summary = "게임 방 생성", description = "멀티 게임 방을 생성합니다.")
    @PostMapping("/")
    public ApiResponseDto<GameRoomResponse> createRoom(@CurrentMember Long memberId, @RequestBody GameRoomRequest.Create request) {
        return ApiResponseDto.onSuccess(createGameRoomUseCase.execute(memberId, request));
    }

    @Operation(summary = "게임 방 수정", description = "멀티 게임 방을 수정합니다.")
    @PutMapping("/{roomId}")
    public ApiResponseDto<GameRoomResponse> updateRoom(@CurrentMember Long memberId, 
                                                       @PathVariable("roomId") Long roomId,
                                                       @RequestBody GameRoomRequest.Update request) {
        return ApiResponseDto.onSuccess(updateGameRoomSettingsUseCase.execute(memberId, request, roomId));
    }

    @Operation(summary = "게임 방 상세 조회", description = "멀티 게임 방 상세 정보를 조회합니다.")
    @BotSuccess
    @GetMapping("/{roomId}")
    public ApiResponseDto<GameRoomDetailResponse> getRoomDetail(@PathVariable("roomId") Long roomId, @CurrentMember Long memberId) {
        return ApiResponseDto.onSuccess(getGameRoomDetailUseCase.execute(memberId, roomId));
    }

    @Operation(summary = "게임 방 참여", description = "멀티 게임 방에 참여합니다.")
    @PostMapping("/{roomId}/join")
    public ApiResponseDto<?> joinRoom(@CurrentMember Long memberId, 
                                      @PathVariable("roomId") Long roomId,
                                      @RequestBody GameRoomRequest.Join request) {
        joinGameRoomUseCase.executeV1(memberId, roomId, request);
        return ApiResponseDto.onSuccess(SuccessStatus._SUCCESS);
    }

    @Operation(summary = "게임 방 퇴장", description = "게임 방에서 퇴장합니다.")
    @DeleteMapping("/{roomId}/leave")
    public ApiResponseDto<?> leaveRoom(@CurrentMember Long memberId, @PathVariable("roomId") Long roomId) {
        leaveGameRoomUseCase.execute(memberId, roomId);
        return ApiResponseDto.onSuccess(SuccessStatus._SUCCESS);
    }

    @Operation(summary = "게임 방 플레이어 강퇴", description = "게임 방에서 플레이어를 강퇴시킵니다.")
    @DeleteMapping("/{roomId}/kick")
    public ApiResponseDto<?> kickPlayer(@CurrentMember Long memberId, 
                                       @PathVariable("roomId") Long roomId,
                                       @RequestBody GameRoomRequest.Kick request) {
        kickPlayerUseCase.execute(memberId, request, roomId);
        return ApiResponseDto.onSuccess(SuccessStatus._SUCCESS);
    }

    @Operation(summary = "게임 시작 알림", description = "게임 시작을 알립니다.")
    @PostMapping("/{roomId}/start")
    public ApiResponseDto<MultiGameResponse.StartGame> notifyStartGame(@CurrentMember Long memberId,
                                                                       @PathVariable("roomId") Long roomId) {
        return ApiResponseDto.onSuccess(notifyStartGameUseCase.execute(memberId, roomId));
    }

}
