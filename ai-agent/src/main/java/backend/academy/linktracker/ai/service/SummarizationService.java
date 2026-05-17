package backend.academy.linktracker.ai.service;

import backend.academy.linktracker.ai.configuration.AiAgentProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class SummarizationService {

    private final AiAgentProperties properties;

    public String summarizeIfNeeded(String text) {
        int threshold = properties.getSummarization().getThreshold();

        if (text.length() <= threshold) {
            log.debug("Text length {} <= threshold {}, no summarization needed", text.length(), threshold);
            return text;
        }

        String mode = properties.getSummarization().getMode();
        log.debug("Text length {} > threshold {}, summarizing with mode: {}", text.length(), threshold, mode);

        return switch (mode.toLowerCase()) {
            case "stub" -> summarizeWithStub(text, threshold);
            case "ai-api" -> summarizeWithAiApi(text);
            default -> {
                log.warn("Unknown summarization mode: {}, using stub", mode);
                yield summarizeWithStub(text, threshold);
            }
        };
    }

    private String summarizeWithStub(String text, int threshold) {
        if (text.length() <= threshold) {
            return text;
        }

        int cutIndex = threshold;
        for (int i = threshold; i > threshold - 50 && i > 0; i--) {
            if (text.charAt(i) == '.' || text.charAt(i) == '!' || text.charAt(i) == '?') {
                cutIndex = i + 1;
                break;
            }
        }

        String summarized = text.substring(0, Math.min(cutIndex, text.length()));
        if (summarized.length() < text.length()) {
            summarized += "...";
        }

        log.debug("Stub summarization: {} -> {} characters", text.length(), summarized.length());
        return summarized;
    }

    private String summarizeWithAiApi(String text) {
        log.warn("AI API summarization not implemented yet, using stub");
        return summarizeWithStub(text, properties.getSummarization().getThreshold());
    }
}
