package backend.academy.linktracker.ai.service;

import backend.academy.linktracker.ai.model.LinkProcessedUpdateEvent;
import backend.academy.linktracker.ai.model.LinkRawUpdateEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UpdateProcessingServiceTest {

    @Mock
    private FilteringService filteringService;
    
    @Mock
    private SummarizationService summarizationService;
    
    @Test
    void shouldReturnNullWhenEventFiltered() {
        // Given
        when(filteringService.shouldFilter(any())).thenReturn(true);
        
        UpdateProcessingService service = new UpdateProcessingService(filteringService, summarizationService);
        
        LinkRawUpdateEvent event = LinkRawUpdateEvent.builder()
                .id(1L)
                .url("http://example.com")
                .description("Test description")
                .author("user1")
                .tgChatIds(Arrays.asList(123L))
                .build();
        
        // When
        LinkProcessedUpdateEvent result = service.processUpdate(event);
        
        // Then
        assertThat(result).isNull();
        verify(filteringService).shouldFilter(event);
        verifyNoInteractions(summarizationService);
    }
    
    @Test
    void shouldProcessEventWhenNotFiltered() {
        // Given
        when(filteringService.shouldFilter(any())).thenReturn(false);
        when(summarizationService.summarizeIfNeeded(any())).thenReturn("Processed description");
        
        UpdateProcessingService service = new UpdateProcessingService(filteringService, summarizationService);
        
        LinkRawUpdateEvent rawEvent = LinkRawUpdateEvent.builder()
                .id(2L)
                .url("http://example.com")
                .description("Original description")
                .author("user2")
                .tgChatIds(Arrays.asList(456L, 789L))
                .build();
        
        // When
        LinkProcessedUpdateEvent result = service.processUpdate(rawEvent);
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(2L);
        assertThat(result.getDescription()).isEqualTo("Processed description");
        assertThat(result.getTgChatIds()).containsExactly(456L, 789L);
        assertThat(result.getPriority()).isEqualTo("HIGH");
        
        verify(filteringService).shouldFilter(rawEvent);
        verify(summarizationService).summarizeIfNeeded("Original description");
    }
    
    @Test
    void shouldUseOriginalDescriptionWhenNoSummarizationNeeded() {
        // Given
        when(filteringService.shouldFilter(any())).thenReturn(false);
        when(summarizationService.summarizeIfNeeded(any())).thenReturn("Original description");
        
        UpdateProcessingService service = new UpdateProcessingService(filteringService, summarizationService);
        
        LinkRawUpdateEvent rawEvent = LinkRawUpdateEvent.builder()
                .id(3L)
                .url("http://example.com")
                .description("Original description")
                .author("user3")
                .tgChatIds(Arrays.asList(111L))
                .build();
        
        // When
        LinkProcessedUpdateEvent result = service.processUpdate(rawEvent);
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.getDescription()).isEqualTo("Original description");
    }
}