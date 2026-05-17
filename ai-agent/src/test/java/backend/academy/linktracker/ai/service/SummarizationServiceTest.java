package backend.academy.linktracker.ai.service;

import backend.academy.linktracker.ai.configuration.AiAgentProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SummarizationServiceTest {

    @Mock
    private AiAgentProperties properties;

    @Mock
    private AiAgentProperties.SummarizationProperties summarizationProperties;

    private SummarizationService summarizationService;

    @BeforeEach
    void setUp() {
        when(properties.getSummarization()).thenReturn(summarizationProperties);
        summarizationService = new SummarizationService(properties);
    }

    @Test
    void shouldNotSummarizeWhenTextShorterThanThreshold() {
        // Given
        when(summarizationProperties.getThreshold()).thenReturn(500);
        when(summarizationProperties.getMode()).thenReturn("stub");

        String text = "This is a short text that should not be summarized.";

        // When
        String result = summarizationService.summarizeIfNeeded(text);

        // Then
        assertThat(result).isEqualTo(text);
    }

    @Test
    void shouldSummarizeWithStubWhenTextLongerThanThreshold() {
        // Given
        when(summarizationProperties.getThreshold()).thenReturn(50);
        when(summarizationProperties.getMode()).thenReturn("stub");

        String text = "This is a very long text that should be summarized because it exceeds the threshold of 50 characters by a significant margin.";

        // When
        String result = summarizationService.summarizeIfNeeded(text);

        // Then
        assertThat(result).isNotEqualTo(text);
        assertThat(result.length()).isLessThanOrEqualTo(text.length());
        assertThat(result).contains("...");
    }

    @Test
    void shouldCutAtSentenceBoundaryWhenPossible() {
        // Given
        when(summarizationProperties.getThreshold()).thenReturn(60);
        when(summarizationProperties.getMode()).thenReturn("stub");

        String text = "This is the first sentence. This is the second sentence that is quite long and should be cut.";

        // When
        String result = summarizationService.summarizeIfNeeded(text);

        // Then
        assertThat(result).isEqualTo("This is the first sentence...");
    }

    @Test
    void shouldHandleAiApiMode() {
        // Given
        when(summarizationProperties.getThreshold()).thenReturn(50);
        when(summarizationProperties.getMode()).thenReturn("ai-api");

        String text = "This is a very long text that should be summarized because it exceeds the threshold.";

        // When
        String result = summarizationService.summarizeIfNeeded(text);

        // Then
        assertThat(result).isNotEqualTo(text);
        assertThat(result).contains("...");
    }

    @Test
    void shouldHandleUnknownMode() {
        // Given
        when(summarizationProperties.getThreshold()).thenReturn(50);
        when(summarizationProperties.getMode()).thenReturn("unknown");

        String text = "This is a very long text that should be summarized.";

        // When
        String result = summarizationService.summarizeIfNeeded(text);

        // Then
        assertThat(result).isNotEqualTo(text);
        // Should fall back to stub
        assertThat(result).contains("...");
    }

    @Test
    void shouldNotAddEllipsisWhenTextFitsExactly() {
        // Given
        when(summarizationProperties.getThreshold()).thenReturn(20);
        when(summarizationProperties.getMode()).thenReturn("stub");

        String text = "Exactly 20 chars.";

        // When
        String result = summarizationService.summarizeIfNeeded(text);

        // Then
        assertThat(result).isEqualTo(text);
        assertThat(result).doesNotContain("...");
    }
}
