package com.kospot.kospot.domain.game.controller;

import com.kospot.kospot.domain.game.service.GameService;
import com.kospot.kospot.exception.payload.dto.ApiResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/roadViewGame")
public class RoadViewGameController {

    private GameService gameService;

    @PostMapping("/start")
    public ApiResponseDto<?> startRoadViewGame(){

        return null;
    }

    @PostMapping("/end")
    public ApiResponseDto<?> endRoadViewGame(){

        return null;
    }

}
