package com.kospot.kospot.presentation.multiGame.gameRoom.controller;


import com.kospot.kospot.application.multiGame.gameRoom.*;
import com.kospot.kospot.domain.member.entity.Member;
import com.kospot.kospot.exception.payload.code.SuccessStatus;
import com.kospot.kospot.exception.payload.dto.ApiResponseDto;
import com.kospot.kospot.presentation.multiGame.gameRoom.dto.request.GameRoomRequest;
import com.kospot.kospot.presentation.multiGame.gameRoom.dto.response.FindGameRoomResponse;
import com.kospot.kospot.presentation.multiGame.gameRoom.dto.response.GameRoomResponse;
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
@RequestMapping("/api/gameRoom/")
public class GameRoomController {

    private final FindAllGameRoomUseCase findAllGameRoomUseCase;
    private final CreateGameRoomUseCase createGameRoomUseCase;
    private final JoinGameRoomUseCase joinGameRoomUseCase;
    private final LeaveGameRoomUseCase leaveGameRoomUseCase;
    private final KickPlayerUseCase kickPlayerUseCase;

    /**
     * Test
     */

    //todo search refactoring
    @Operation(summary = "게임 방 전체 조회", description = "멀티 게임 방을 전체 조회합니다.")
    @GetMapping("/{page}")
    public ApiResponseDto<List<FindGameRoomResponse>> findGameRoom(@RequestBody GameRoomRequest.Find request, @PathVariable("page") int page) {
        return ApiResponseDto.onSuccess(findAllGameRoomUseCase.execute(request, page));
    }

    //todo 방 새로고침

    //todo 게임 방 내부 조회(입장), 실시간 플레이어들 상태(입장, 퇴장) <- websocket 고려


    @Operation(summary = "게임 방 생성", description = "멀티 게임 방을 생성합니다.")
    @PostMapping("/")
    public ApiResponseDto<GameRoomResponse> createGameRoom(Member member, @RequestBody GameRoomRequest.Create request) {
        return ApiResponseDto.onSuccess(createGameRoomUseCase.execute(member, request));
    }

    @Operation(summary = "게임 방 참여", description = "멀티 게임 방에 참여합니다.")
    @PostMapping("/{id}/join")
    public ApiResponseDto<?> joinGameRoom(Member member, @PathVariable("id") Long gameRoomId,
                                          @RequestBody GameRoomRequest.Join request) {
        joinGameRoomUseCase.execute(member, gameRoomId, request);
        return ApiResponseDto.onSuccess(SuccessStatus._SUCCESS);
    }

    @Operation(summary = "게임 방 퇴장", description = "게임 방에서 퇴장합니다.")
    @PostMapping("/{id}/leave")
    public ApiResponseDto<?> leaveGameRoom(Member member, @PathVariable("id") Long gameRoomId) {
        leaveGameRoomUseCase.execute(member, gameRoomId);
        return ApiResponseDto.onSuccess(SuccessStatus._SUCCESS);
    }

    @Operation(summary = "게임 방 강퇴", description = "게임 방에서 강퇴시킵니다.")
    @PostMapping("/{id}/kick")
    public ApiResponseDto<?> kickPlayer(Member member, @RequestBody GameRoomRequest.Kick request,@PathVariable("id") Long gameRoomId) {
        kickPlayerUseCase.execute(member, request, gameRoomId);
        return ApiResponseDto.onSuccess(SuccessStatus._SUCCESS);
    }


}
