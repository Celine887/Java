package backend.academy.linktracker.scrapper.service;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import backend.academy.linktracker.scrapper.BaseIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class ChatServiceIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private ChatService chatService;

    @Test
    void register_shouldRegisterChatSuccessfully() {
        // Given
        Long chatId = 100001L;

        // When & Then
        assertThatCode(() -> chatService.register(chatId)).doesNotThrowAnyException();
    }

    @Test
    void register_shouldThrowExceptionOnDuplicateRegistration() {
        // Given
        Long chatId = 100002L;
        chatService.register(chatId);

        // When & Then - should throw exception
        assertThatThrownBy(() -> chatService.register(chatId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Chat already registered");
    }

    @Test
    void unregister_shouldUnregisterChatSuccessfully() {
        // Given
        Long chatId = 100003L;
        chatService.register(chatId);

        // When & Then
        assertThatCode(() -> chatService.unregister(chatId)).doesNotThrowAnyException();
    }

    @Test
    void unregister_shouldThrowExceptionWhenChatNotFound() {
        // Given
        Long nonExistentChatId = 999999L;

        // When & Then
        assertThatThrownBy(() -> chatService.unregister(nonExistentChatId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Chat not found");
    }
}
