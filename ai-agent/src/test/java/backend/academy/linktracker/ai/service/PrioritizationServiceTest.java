package backend.academy.linktracker.ai.service;

import backend.academy.linktracker.ai.configuration.AiAgentProperties;
import backend.academy.linktracker.ai.model.Priority;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PrioritizationServiceTest {

    private PrioritizationService prioritizationService;
    private AiAgentProperties properties;

    @BeforeEach
    void setUp() {
        properties = new AiAgentProperties();
        
        AiAgentProperties.PrioritizationProperties prioritization = new AiAgentProperties.PrioritizationProperties();
        prioritization.setHighKeywords(List.of("critical", "urgent", "breaking", "security"));
        prioritization.setLowKeywords(List.of("minor", "typo", "chore", "docs"));
        properties.setPrioritization(prioritization);
        
        prioritizationService = new PrioritizationService(properties);
    }

    @Test
    @DisplayName("TC-1.1: Обновление высокой значимости - содержит critical")
    void testHighPriority_Critical() {
        // Given
        String description = "This is a critical bug fix that needs immediate attention";
        
        // When
        Priority priority = prioritizationService.determinePriority(description);
        
        // Then
        assertThat(priority).isEqualTo(Priority.HIGH);
    }

    @Test
    @DisplayName("TC-1.1: Обновление высокой значимости - содержит urgent")
    void testHighPriority_Urgent() {
        // Given
        String description = "Urgent: security vulnerability detected";
        
        // When
        Priority priority = prioritizationService.determinePriority(description);
        
        // Then
        assertThat(priority).isEqualTo(Priority.HIGH);
    }

    @Test
    @DisplayName("TC-1.1: Обновление высокой значимости - содержит breaking")
    void testHighPriority_Breaking() {
        // Given
        String description = "Breaking change in API v2";
        
        // When
        Priority priority = prioritizationService.determinePriority(description);
        
        // Then
        assertThat(priority).isEqualTo(Priority.HIGH);
    }

    @Test
    @DisplayName("TC-1.1: Обновление высокой значимости - содержит security")
    void testHighPriority_Security() {
        // Given
        String description = "Security patch for authentication module";
        
        // When
        Priority priority = prioritizationService.determinePriority(description);
        
        // Then
        assertThat(priority).isEqualTo(Priority.HIGH);
    }

    @Test
    @DisplayName("TC-1.2: Обновление обычной значимости - без ключевых слов")
    void testMediumPriority_NoKeywords() {
        // Given
        String description = "Added new feature for user profile management";
        
        // When
        Priority priority = prioritizationService.determinePriority(description);
        
        // Then
        assertThat(priority).isEqualTo(Priority.MEDIUM);
    }

    @Test
    @DisplayName("TC-1.3: Обновление низкой значимости - содержит typo")
    void testLowPriority_Typo() {
        // Given
        String description = "Fix typo in readme file";
        
        // When
        Priority priority = prioritizationService.determinePriority(description);
        
        // Then
        assertThat(priority).isEqualTo(Priority.LOW);
    }

    @Test
    @DisplayName("TC-1.3: Обновление низкой значимости - содержит minor")
    void testLowPriority_Minor() {
        // Given
        String description = "Minor refactoring of utility class";
        
        // When
        Priority priority = prioritizationService.determinePriority(description);
        
        // Then
        assertThat(priority).isEqualTo(Priority.LOW);
    }

    @Test
    @DisplayName("TC-1.3: Обновление низкой значимости - содержит chore")
    void testLowPriority_Chore() {
        // Given
        String description = "Chore: update dependencies";
        
        // When
        Priority priority = prioritizationService.determinePriority(description);
        
        // Then
        assertThat(priority).isEqualTo(Priority.LOW);
    }

    @Test
    @DisplayName("TC-1.3: Обновление низкой значимости - содержит docs")
    void testLowPriority_Docs() {
        // Given
        String description = "Update docs for installation guide";
        
        // When
        Priority priority = prioritizationService.determinePriority(description);
        
        // Then
        assertThat(priority).isEqualTo(Priority.LOW);
    }

    @Test
    @DisplayName("Пустое описание возвращает MEDIUM")
    void testEmptyDescription() {
        // When
        Priority priority = prioritizationService.determinePriority("");
        
        // Then
        assertThat(priority).isEqualTo(Priority.MEDIUM);
    }

    @Test
    @DisplayName("Null описание возвращает MEDIUM")
    void testNullDescription() {
        // When
        Priority priority = prioritizationService.determinePriority(null);
        
        // Then
        assertThat(priority).isEqualTo(Priority.MEDIUM);
    }

    @Test
    @DisplayName("Регистронезависимая проверка ключевых слов")
    void testCaseInsensitiveKeywords() {
        // Given
        String description = "CRITICAL issue with UPPERCASE keywords";
        
        // When
        Priority priority = prioritizationService.determinePriority(description);
        
        // Then
        assertThat(priority).isEqualTo(Priority.HIGH);
    }
}
