package backend.academy.linktracker.scrapper.scheduler;

import backend.academy.linktracker.scrapper.service.LinkUpdateService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "app.scheduler", name = "enabled", havingValue = "true", matchIfMissing = true)
public class LinkUpdateScheduler {
    private static final Logger log = LoggerFactory.getLogger(LinkUpdateScheduler.class);

    private final LinkUpdateService linkUpdateService;

    public LinkUpdateScheduler(LinkUpdateService linkUpdateService) {
        this.linkUpdateService = linkUpdateService;
    }

    @Scheduled(fixedDelayString = "${app.scheduler.interval}")
    public void update() {
        log.debug("Scheduled link update triggered");
        linkUpdateService.updateAllLinks();
    }
}
