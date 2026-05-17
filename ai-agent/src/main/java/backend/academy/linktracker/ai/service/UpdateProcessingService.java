package backend.academy.linktracker.ai.service;

import backend.academy.linktracker.ai.model.LinkProcessedUpdateEvent;
import backend.academy.linktracker.ai.model.LinkRawUpdateEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class UpdateProcessingService {

    private final FilteringService filteringService;
    private final SummarizationService summarizationService;

    public LinkProcessedUpdateEvent processUpdate(LinkRawUpdateEvent rawEvent) {
        log.info("Processing update event: id={}, author={}, description length={}",
                rawEvent.getId(), rawEvent.getAuthor(), rawEvent.getDescription().length());

        if (filteringService.shouldFilter(rawEvent)) {
            log.info("Event {} filtered out", rawEvent.getId());
            return null;
        }

        String processedDescription = summarizationService.summarizeIfNeeded(rawEvent.getDescription());

        LinkProcessedUpdateEvent processedEvent = LinkProcessedUpdateEvent.builder()
                .id(rawEvent.getId())
                .description(processedDescription)
                .tgChatIds(rawEvent.getTgChatIds())
                .priority("HIGH")
                .build();

        log.info("Event {} processed successfully. Original length: {}, processed length: {}",
                rawEvent.getId(), rawEvent.getDescription().length(), processedDescription.length());

        return processedEvent;
    }
}
