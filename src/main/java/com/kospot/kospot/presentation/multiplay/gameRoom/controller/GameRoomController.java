package com.kospot.kospot.presentation.multiplay.gameRoom.controller;


import com.kospot.kospot.application.multiplay.gameRoom.CreateGameRoomUseCase;
import com.kospot.kospot.domain.member.entity.Member;
import com.kospot.kospot.domain.multiplay.gameRoom.adaptor.GameRoomAdaptor;
import com.kospot.kospot.exception.payload.dto.ApiResponseDto;
import com.kospot.kospot.presentation.item.dto.response.ItemResponse;
import com.kospot.kospot.presentation.multiplay.gameRoom.dto.request.GameRoomRequest;
import com.kospot.kospot.presentation.multiplay.gameRoom.dto.response.GameRoomResponse;
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

    private final CreateGameRoomUseCase createGameRoomUseCase;

    /**
     *  Test
     */

    @Operation(summary = "게임 방 생성", description = "멀티 게임 방을 생성합니다.")
    @PostMapping("/")
    public ApiResponseDto<GameRoomResponse> createGameRoom(Member member, @RequestBody GameRoomRequest.Create request) {
        return ApiResponseDto.onSuccess(createGameRoomUseCase.execute(member, request));
    }

    @Operation(summary = "게임 방 참여", description = "멀티 게임 방에 참여합니다.")
    @PostMapping("/{id}")
    public ApiResponseDto<?> joinGameRoom(Member member, @PathVariable("id") Long gameRoomId,
                                          @RequestBody GameRoomRequest.Join request) {
        return ApiResponseDto.onSuccess();
    }


}
