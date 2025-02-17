package com.kospot.kospot.domain.game.controller;

import com.kospot.kospot.domain.game.dto.response.StartGameResponse;
import com.kospot.kospot.domain.game.service.RoadViewGameService;
import com.kospot.kospot.exception.payload.code.SuccessStatus;
import com.kospot.kospot.exception.payload.dto.ApiResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/roadViewGame")
public class RoadViewGameController {

    private final RoadViewGameService service;

    /**
     *  -----------------PRACTICE------------------
     */

    @PostMapping("/practice/start")
    public ApiResponseDto<StartGameResponse.RoadView> startPracticeGame(@RequestParam("sido") String sidoKey){
        StartGameResponse.RoadView response = service.startPracticeGame(sidoKey);
        return ApiResponseDto.onSuccess(response);
    }

    @PostMapping("/practice/end")
    public ApiResponseDto<?> endRoadViewGame(){

        return ApiResponseDto.onSuccess(SuccessStatus._SUCCESS);
    }

    /**
     *  ------------------------------------------
     */

    /**
     *  -----------------RANK------------------
     */


    /**
     *  ------------------------------------------
     */

}
