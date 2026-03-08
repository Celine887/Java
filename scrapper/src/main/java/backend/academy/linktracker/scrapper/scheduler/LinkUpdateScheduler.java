package backend.academy.linktracker.scrapper.scheduler;

import backend.academy.linktracker.scrapper.service.LinkUpdateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(prefix = "app.scheduler", name = "enabled", havingValue = "true", matchIfMissing = true)
public class LinkUpdateScheduler {

    private final LinkUpdateService linkUpdateService;

    @Scheduled(fixedDelayString = "${app.scheduler.interval}")
    public void update() {
        log.debug("Scheduled link update triggered");
        linkUpdateService.updateAllLinks();
    }
}
