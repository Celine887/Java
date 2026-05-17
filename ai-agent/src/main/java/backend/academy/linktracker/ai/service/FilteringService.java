package backend.academy.linktracker.ai.service;

import backend.academy.linktracker.ai.model.LinkRawUpdateEvent;
import backend.academy.linktracker.ai.configuration.AiAgentProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;

@Slf4j
@Service
@RequiredArgsConstructor
public class FilteringService {

    private final AiAgentProperties properties;

    public boolean shouldFilter(LinkRawUpdateEvent event) {
        String description = event.getDescription();
        String author = event.getAuthor();

        if (containsStopWords(description)) {
            log.debug("Filtering event {}: contains stop words", event.getId());
            return true;
        }

        if (isExcludedAuthor(author)) {
            log.debug("Filtering event {}: author {} is excluded", event.getId(), author);
            return true;
        }

        if (description.length() < properties.getFiltering().getMinLength()) {
            log.debug("Filtering event {}: description too short ({} < {})",
                event.getId(), description.length(), properties.getFiltering().getMinLength());
            return true;
        }

        log.debug("Event {} passed all filters", event.getId());
        return false;
    }

    private boolean containsStopWords(String text) {
        List<String> stopWords = properties.getFiltering().getStopWords();
        if (stopWords == null || stopWords.isEmpty()) {
            return false;
        }

        String lowerText = text.toLowerCase(Locale.ROOT);
        return stopWords.stream()
                .anyMatch(stopWord -> lowerText.contains(stopWord.toLowerCase(Locale.ROOT)));
    }

    private boolean isExcludedAuthor(String author) {
        List<String> excludedAuthors = properties.getFiltering().getExcludedAuthors();
        if (excludedAuthors == null || excludedAuthors.isEmpty()) {
            return false;
        }

        return excludedAuthors.contains(author);
    }
}
