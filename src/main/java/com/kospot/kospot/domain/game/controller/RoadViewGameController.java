package com.kospot.kospot.domain.game.controller;

import com.kospot.kospot.domain.game.service.RoadViewGameService;
import com.kospot.kospot.exception.payload.dto.ApiResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/roadViewGame")
public class RoadViewGameController {

    private RoadViewGameService roadViewGameService;

    @PostMapping("/start")
    public ApiResponseDto<?> roadViewGameStart(){

        return null;
    }

    @PostMapping("/end")
    public ApiResponseDto<?> roadViewGameEnd(){

        return null;
    }

}
