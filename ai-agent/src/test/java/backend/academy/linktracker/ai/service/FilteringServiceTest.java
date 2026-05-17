package backend.academy.linktracker.ai.service;

import backend.academy.linktracker.ai.model.LinkRawUpdateEvent;
import backend.academy.linktracker.ai.configuration.AiAgentProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FilteringServiceTest {

    @Mock
    private AiAgentProperties properties;
    
    @Mock
    private AiAgentProperties.FilteringProperties filteringProperties;
    
    private FilteringService filteringService;
    
    @BeforeEach
    void setUp() {
        when(properties.getFiltering()).thenReturn(filteringProperties);
        filteringService = new FilteringService(properties);
    }
    
    @Test
    void shouldFilterWhenContainsStopWords() {
        // Given
        List<String> stopWords = Arrays.asList("spam", "ads", "promo");
        when(filteringProperties.getStopWords()).thenReturn(stopWords);
        when(filteringProperties.getMinLength()).thenReturn(10);
        
        LinkRawUpdateEvent event = LinkRawUpdateEvent.builder()
                .id(1L)
                .url("http://example.com")
                .description("This is a spam message")
                .author("user1")
                .tgChatIds(Arrays.asList(123L))
                .build();
        
        // When
        boolean shouldFilter = filteringService.shouldFilter(event);
        
        // Then
        assertThat(shouldFilter).isTrue();
    }
    
    @Test
    void shouldFilterWhenAuthorExcluded() {
        // Given
        List<String> excludedAuthors = Arrays.asList("bot-user", "spammer");
        when(filteringProperties.getExcludedAuthors()).thenReturn(excludedAuthors);
        when(filteringProperties.getStopWords()).thenReturn(List.of());
        when(filteringProperties.getMinLength()).thenReturn(10);
        
        LinkRawUpdateEvent event = LinkRawUpdateEvent.builder()
                .id(2L)
                .url("http://example.com")
                .description("Valid message without stop words")
                .author("bot-user")
                .tgChatIds(Arrays.asList(123L))
                .build();
        
        // When
        boolean shouldFilter = filteringService.shouldFilter(event);
        
        // Then
        assertThat(shouldFilter).isTrue();
    }
    
    @Test
    void shouldFilterWhenTextTooShort() {
        // Given
        when(filteringProperties.getStopWords()).thenReturn(List.of());
        when(filteringProperties.getExcludedAuthors()).thenReturn(List.of());
        when(filteringProperties.getMinLength()).thenReturn(20);
        
        LinkRawUpdateEvent event = LinkRawUpdateEvent.builder()
                .id(3L)
                .url("http://example.com")
                .description("Short text")
                .author("user1")
                .tgChatIds(Arrays.asList(123L))
                .build();
        
        // When
        boolean shouldFilter = filteringService.shouldFilter(event);
        
        // Then
        assertThat(shouldFilter).isTrue();
    }
    
    @Test
    void shouldNotFilterWhenAllConditionsMet() {
        // Given
        List<String> stopWords = Arrays.asList("spam", "ads");
        List<String> excludedAuthors = Arrays.asList("bot-user");
        when(filteringProperties.getStopWords()).thenReturn(stopWords);
        when(filteringProperties.getExcludedAuthors()).thenReturn(excludedAuthors);
        when(filteringProperties.getMinLength()).thenReturn(10);
        
        LinkRawUpdateEvent event = LinkRawUpdateEvent.builder()
                .id(4L)
                .url("http://example.com")
                .description("This is a valid message without any issues")
                .author("regular-user")
                .tgChatIds(Arrays.asList(123L))
                .build();
        
        // When
        boolean shouldFilter = filteringService.shouldFilter(event);
        
        // Then
        assertThat(shouldFilter).isFalse();
    }
    
    @Test
    void shouldHandleCaseInsensitiveStopWords() {
        // Given
        List<String> stopWords = Arrays.asList("SPAM", "ADS");
        when(filteringProperties.getStopWords()).thenReturn(stopWords);
        when(filteringProperties.getMinLength()).thenReturn(10);
        
        LinkRawUpdateEvent event = LinkRawUpdateEvent.builder()
                .id(5L)
                .url("http://example.com")
                .description("This is a SpAm message")
                .author("user1")
                .tgChatIds(Arrays.asList(123L))
                .build();
        
        // When
        boolean shouldFilter = filteringService.shouldFilter(event);
        
        // Then
        assertThat(shouldFilter).isTrue();
    }
}