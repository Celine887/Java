package backend.academy.linktracker.ai.service;

import backend.academy.linktracker.ai.configuration.AiAgentProperties;
import backend.academy.linktracker.ai.model.LinkProcessedUpdateEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

class GroupingServiceTest {

    private GroupingService groupingService;
    private AiAgentProperties properties;

    @BeforeEach
    void setUp() {
        properties = new AiAgentProperties();
        
        AiAgentProperties.GroupingProperties grouping = new AiAgentProperties.GroupingProperties();
        grouping.setWindowMs(1000); // 1 секунда для тестов
        properties.setGrouping(grouping);
        
        groupingService = new GroupingService(properties);
    }

    @Test
    @DisplayName("TC-2.2: Одиночное обновление - группировка не применяется")
    void testSingleUpdate_NoGrouping() {
        // Given
        LinkProcessedUpdateEvent event = LinkProcessedUpdateEvent.builder()
                .id(1L)
                .description("Single update")
                .tgChatIds(List.of(100L))
                .priority("MEDIUM")
                .build();
        
        // When
        List<LinkProcessedUpdateEvent> readyUpdates = groupingService.addAndGetReadyUpdates(event);
        
        // Then - сразу после добавления не должно быть готовых обновлений
        assertThat(readyUpdates).isEmpty();
        
        await().pollDelay(java.time.Duration.ofMillis(1100))
                .untilAsserted(() -> {
                    List<LinkProcessedUpdateEvent> flushed = groupingService.flushAll();
                    assertThat(flushed).hasSize(1);
                    assertThat(flushed.get(0).getDescription()).isEqualTo("Single update");
                    assertThat(flushed.get(0).getPriority()).isEqualTo("MEDIUM");
                });
    }

    @Test
    @DisplayName("TC-2.1: Группировка нескольких обновлений")
    void testMultipleUpdates_Grouping() throws InterruptedException {
        // Given
        LinkProcessedUpdateEvent event1 = LinkProcessedUpdateEvent.builder()
                .id(1L)
                .description("First update")
                .tgChatIds(List.of(100L))
                .priority("MEDIUM")
                .build();
        
        LinkProcessedUpdateEvent event2 = LinkProcessedUpdateEvent.builder()
                .id(2L)
                .description("Second update")
                .tgChatIds(List.of(100L))
                .priority("HIGH")
                .build();
        
        LinkProcessedUpdateEvent event3 = LinkProcessedUpdateEvent.builder()
                .id(3L)
                .description("Third update")
                .tgChatIds(List.of(100L))
                .priority("LOW")
                .build();
        
        // When
        groupingService.addAndGetReadyUpdates(event1);
        Thread.sleep(100);
        groupingService.addAndGetReadyUpdates(event2);
        Thread.sleep(100);
        groupingService.addAndGetReadyUpdates(event3);
        
        Thread.sleep(1100);
        List<LinkProcessedUpdateEvent> flushed = groupingService.flushAll();
        
        // Then
        assertThat(flushed).hasSize(1);
        LinkProcessedUpdateEvent grouped = flushed.get(0);
        
        assertThat(grouped.getTgChatIds()).containsExactly(100L);
        assertThat(grouped.getPriority()).isEqualTo("HIGH"); // Максимальный приоритет
        assertThat(grouped.getDescription())
                .contains("1. First update")
                .contains("2. Second update")
                .contains("3. Third update");
    }

    @Test
    @DisplayName("TC-2.1: Группировка с максимальным приоритетом HIGH")
    void testGrouping_MaxPriorityHigh() throws InterruptedException {
        // Given
        LinkProcessedUpdateEvent event1 = LinkProcessedUpdateEvent.builder()
                .id(1L)
                .description("Low priority update")
                .tgChatIds(List.of(100L))
                .priority("LOW")
                .build();
        
        LinkProcessedUpdateEvent event2 = LinkProcessedUpdateEvent.builder()
                .id(2L)
                .description("High priority update")
                .tgChatIds(List.of(100L))
                .priority("HIGH")
                .build();
        
        // When
        groupingService.addAndGetReadyUpdates(event1);
        groupingService.addAndGetReadyUpdates(event2);
        
        Thread.sleep(1100);
        List<LinkProcessedUpdateEvent> flushed = groupingService.flushAll();
        
        // Then
        assertThat(flushed).hasSize(1);
        assertThat(flushed.get(0).getPriority()).isEqualTo("HIGH");
    }

    @Test
    @DisplayName("TC-2.1: Группировка с максимальным приоритетом MEDIUM")
    void testGrouping_MaxPriorityMedium() throws InterruptedException {
        // Given
        LinkProcessedUpdateEvent event1 = LinkProcessedUpdateEvent.builder()
                .id(1L)
                .description("Low priority update")
                .tgChatIds(List.of(100L))
                .priority("LOW")
                .build();
        
        LinkProcessedUpdateEvent event2 = LinkProcessedUpdateEvent.builder()
                .id(2L)
                .description("Medium priority update")
                .tgChatIds(List.of(100L))
                .priority("MEDIUM")
                .build();
        
        // When
        groupingService.addAndGetReadyUpdates(event1);
        groupingService.addAndGetReadyUpdates(event2);
        
        Thread.sleep(1100);
        List<LinkProcessedUpdateEvent> flushed = groupingService.flushAll();
        
        // Then
        assertThat(flushed).hasSize(1);
        assertThat(flushed.get(0).getPriority()).isEqualTo("MEDIUM");
    }

    @Test
    @DisplayName("Обновления для разных chatId не группируются")
    void testDifferentChatIds_NoGrouping() throws InterruptedException {
        // Given
        LinkProcessedUpdateEvent event1 = LinkProcessedUpdateEvent.builder()
                .id(1L)
                .description("Update for chat 100")
                .tgChatIds(List.of(100L))
                .priority("MEDIUM")
                .build();
        
        LinkProcessedUpdateEvent event2 = LinkProcessedUpdateEvent.builder()
                .id(2L)
                .description("Update for chat 200")
                .tgChatIds(List.of(200L))
                .priority("HIGH")
                .build();
        
        // When
        groupingService.addAndGetReadyUpdates(event1);
        groupingService.addAndGetReadyUpdates(event2);
        
        Thread.sleep(1100);
        List<LinkProcessedUpdateEvent> flushed = groupingService.flushAll();
        
        // Then
        assertThat(flushed).hasSize(2);
        assertThat(flushed).extracting(LinkProcessedUpdateEvent::getDescription)
                .containsExactlyInAnyOrder("Update for chat 100", "Update for chat 200");
    }

    @Test
    @DisplayName("Обновление с несколькими chatId создает отдельные группы")
    void testMultipleChatIds() throws InterruptedException {
        // Given
        LinkProcessedUpdateEvent event = LinkProcessedUpdateEvent.builder()
                .id(1L)
                .description("Update for multiple chats")
                .tgChatIds(List.of(100L, 200L))
                .priority("MEDIUM")
                .build();
        
        // When
        groupingService.addAndGetReadyUpdates(event);
        
        Thread.sleep(1100);
        List<LinkProcessedUpdateEvent> flushed = groupingService.flushAll();
        
        // Then
        assertThat(flushed).hasSize(2);
        assertThat(flushed).extracting(e -> e.getTgChatIds().get(0))
                .containsExactlyInAnyOrder(100L, 200L);
    }
}
