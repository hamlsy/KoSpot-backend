package com.kospot.presentation.multiGame.gameRoom.controller;


import com.kospot.application.multiGame.gameRoom.*;
import com.kospot.domain.member.entity.Member;
import com.kospot.exception.payload.code.SuccessStatus;
import com.kospot.exception.payload.dto.ApiResponseDto;
import com.kospot.presentation.multiGame.gameRoom.dto.request.GameRoomRequest;
import com.kospot.presentation.multiGame.gameRoom.dto.response.FindGameRoomResponse;
import com.kospot.presentation.multiGame.gameRoom.dto.response.GameRoomDetailResponse;
import com.kospot.presentation.multiGame.gameRoom.dto.response.GameRoomResponse;
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
@RequestMapping("/gameRoom/")
public class GameRoomController {

    private final FindAllGameRoomUseCase findAllGameRoomUseCase;
    private final FindGameRoomDetailUseCase findGameRoomDetailUseCase;
    private final CreateGameRoomUseCase createGameRoomUseCase;
    private final UpdateGameRoomUseCase updateGameRoomUseCase;
    private final JoinGameRoomUseCase joinGameRoomUseCase;
    private final LeaveGameRoomUseCase leaveGameRoomUseCase;
    private final KickPlayerUseCase kickPlayerUseCase;

    //todo search refactoring
    @Operation(summary = "게임 방 전체 조회", description = "멀티 게임 방을 전체 조회합니다.")
    @GetMapping("/{page}")
    public ApiResponseDto<List<FindGameRoomResponse>> findGameRoom(@RequestBody GameRoomRequest.Find request, @PathVariable("page") int page) {
        return ApiResponseDto.onSuccess(findAllGameRoomUseCase.execute(request, page));
    }

    @Operation(summary = "게임 방 내부 조회", description = "멀티 게임 방 내부를 조회합니다.")
    @GetMapping("/{id}")
    public ApiResponseDto<GameRoomDetailResponse> findDetailGameRoom(@PathVariable("id") Long gameRoomId) {
        return ApiResponseDto.onSuccess(findGameRoomDetailUseCase.execute(gameRoomId));
    }

    @Operation(summary = "게임 방 생성", description = "멀티 게임 방을 생성합니다.")
    @PostMapping("/")
    public ApiResponseDto<GameRoomResponse> createGameRoom(Member member, @RequestBody GameRoomRequest.Create request) {
        return ApiResponseDto.onSuccess(createGameRoomUseCase.execute(member, request));
    }

    @Operation(summary = "게임 방 수정", description = "멀티 게임 방을 수정합니다.")
    @PutMapping("/{id}")
    public ApiResponseDto<GameRoomResponse> updateGameRoom(Member member, @RequestBody GameRoomRequest.Update request, @PathVariable("id") Long gameRoomId) {
        return ApiResponseDto.onSuccess(updateGameRoomUseCase.execute(member, request, gameRoomId));
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
    public ApiResponseDto<?> kickPlayer(Member member, @RequestBody GameRoomRequest.Kick request, @PathVariable("id") Long gameRoomId) {
        kickPlayerUseCase.execute(member, request, gameRoomId);
        return ApiResponseDto.onSuccess(SuccessStatus._SUCCESS);
    }


}
