package com.kospot.kospot.domain.game.controller;

import com.kospot.kospot.domain.game.dto.response.StartGameResponse;
import com.kospot.kospot.domain.game.service.RoadViewGameService;
import com.kospot.kospot.exception.payload.code.SuccessStatus;
import com.kospot.kospot.exception.payload.dto.ApiResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/roadViewGame")
public class RoadViewGameController {

    private final RoadViewGameService service;

    @PostMapping("/start/{sidoKey}")
    public ApiResponseDto<StartGameResponse> startRoadViewGame(@PathVariable("sidoKey") String sidoKey){
        StartGameResponse response = service.startGame(sidoKey);
        return ApiResponseDto.onSuccess(SuccessStatus._SUCCESS);
    }

    @PostMapping("/end")
    public ApiResponseDto<?> endRoadViewGame(){

        return ApiResponseDto.onSuccess(SuccessStatus._SUCCESS);
    }

}
