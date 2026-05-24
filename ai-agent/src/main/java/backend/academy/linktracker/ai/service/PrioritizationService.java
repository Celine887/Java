package backend.academy.linktracker.ai.service;

import backend.academy.linktracker.ai.configuration.AiAgentProperties;
import backend.academy.linktracker.ai.model.Priority;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PrioritizationService {

    private final AiAgentProperties properties;

    public Priority determinePriority(String description) {
        if (description == null || description.isBlank()) {
            return Priority.MEDIUM;
        }

        String lowerDescription = description.toLowerCase();
        
        List<String> highKeywords = properties.getPrioritization().getHighKeywords();
        if (highKeywords != null && containsAnyKeyword(lowerDescription, highKeywords)) {
            log.debug("High priority detected in description");
            return Priority.HIGH;
        }

        List<String> lowKeywords = properties.getPrioritization().getLowKeywords();
        if (lowKeywords != null && containsAnyKeyword(lowerDescription, lowKeywords)) {
            log.debug("Low priority detected in description");
            return Priority.LOW;
        }

        log.debug("Medium priority assigned by default");
        return Priority.MEDIUM;
    }

    private boolean containsAnyKeyword(String text, List<String> keywords) {
        return keywords.stream()
                .anyMatch(keyword -> text.contains(keyword.toLowerCase()));
    }
}
