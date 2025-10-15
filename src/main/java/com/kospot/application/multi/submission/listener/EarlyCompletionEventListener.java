package com.kospot.application.multi.submission.listener;

import com.kospot.application.multi.round.roadview.CheckAndCompleteRoundEarlyUseCase;
import com.kospot.domain.multi.submission.event.PlayerSubmissionCompletedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class EarlyCompletionEventListener {

    private final CheckAndCompleteRoundEarlyUseCase checkAndCompleteRoundEarlyUseCase;

    @Async
    @EventListener
    public void onPlayerSubmission(PlayerSubmissionCompletedEvent event) {
        boolean completed = checkAndCompleteRoundEarlyUseCase.execute(
                event.getMode(),
                event.getMatchType(),
                event.getGameRoomId(),
                event.getGameId(),
                event.getRoundId()
        );
        
        if (completed) {
            log.info("🎉 Round completed early via event - RoundId: {}", event.getRoundId());
        }
    }

}
