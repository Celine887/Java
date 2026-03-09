package backend.academy.linktracker.scrapper.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import backend.academy.linktracker.scrapper.BaseIntegrationTest;
import backend.academy.linktracker.scrapper.dto.AddLinkRequest;
import backend.academy.linktracker.scrapper.dto.LinkResponse;
import backend.academy.linktracker.scrapper.dto.ListLinksResponse;
import backend.academy.linktracker.scrapper.exception.ChatNotFoundException;
import backend.academy.linktracker.scrapper.exception.LinkAlreadyTrackedException;
import backend.academy.linktracker.scrapper.exception.LinkNotFoundException;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class LinkServiceIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private LinkService linkService;

    @Autowired
    private ChatService chatService;

    private static final Long TEST_CHAT_ID = 12345L;
    private static final String TEST_URL = "https://github.com/sanyarnd/tinkoff-java-course-2023";

    @BeforeEach
    void setUp() {
        // Register chat before each test with unique ID to avoid conflicts
        Long uniqueChatId = TEST_CHAT_ID + (long) (Math.random() * 100000);
        try {
            chatService.register(uniqueChatId);
        } catch (Exception e) {
            // Chat might already exist
        }
    }

    @Test
    void addLink_shouldAddLinkSuccessfully() {
        // Given
        Long chatId = 10001L;
        chatService.register(chatId);
        AddLinkRequest request = new AddLinkRequest(
                chatId, "https://github.com/spring-projects/spring-boot", Set.of("test", "integration"));

        // When
        LinkResponse response = linkService.add(request);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.url()).isEqualTo("https://github.com/spring-projects/spring-boot");
        assertThat(response.tags()).containsExactlyInAnyOrder("test", "integration");
    }

    @Test
    void addLink_shouldThrowExceptionWhenChatNotFound() {
        // Given
        Long nonExistentChatId = 99999L;
        AddLinkRequest request = new AddLinkRequest(nonExistentChatId, TEST_URL, Set.of());

        // When & Then
        assertThatThrownBy(() -> linkService.add(request)).isInstanceOf(ChatNotFoundException.class);
    }

    @Test
    void addLink_shouldThrowExceptionWhenLinkAlreadyTracked() {
        // Given
        Long chatId = 10003L;
        chatService.register(chatId);
        String url = "https://github.com/apache/kafka";
        AddLinkRequest request = new AddLinkRequest(chatId, url, Set.of());
        linkService.add(request);

        // When & Then
        assertThatThrownBy(() -> linkService.add(request)).isInstanceOf(LinkAlreadyTrackedException.class);
    }

    @Test
    void listAll_shouldReturnEmptyListWhenNoLinks() {
        // Given
        Long newChatId = 54321L;
        chatService.register(newChatId);

        // When
        ListLinksResponse response = linkService.listAll(newChatId, null);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.links()).isEmpty();
    }

    @Test
    void listAll_shouldReturnAllLinks() {
        // Given
        Long chatId = 11111L;
        chatService.register(chatId);
        linkService.add(new AddLinkRequest(chatId, "https://github.com/google/guava", Set.of("tag1")));
        linkService.add(new AddLinkRequest(chatId, "https://github.com/ReactiveX/RxJava", Set.of("tag2")));

        // When
        ListLinksResponse response = linkService.listAll(chatId, null);

        // Then
        assertThat(response.links()).hasSize(2);
    }

    @Test
    void listAll_shouldFilterByTag() {
        // Given
        Long chatId = 22222L;
        chatService.register(chatId);
        linkService.add(new AddLinkRequest(chatId, "https://github.com/square/okhttp", Set.of("java")));
        linkService.add(new AddLinkRequest(chatId, "https://github.com/psf/requests", Set.of("python")));

        // When
        ListLinksResponse response = linkService.listAll(chatId, "java");

        // Then
        assertThat(response.links()).hasSize(1);
        assertThat(response.links().getFirst().tags()).contains("java");
    }

    @Test
    void removeLink_shouldRemoveLinkSuccessfully() {
        // Given
        Long chatId = 33333L;
        chatService.register(chatId);
        String url = "https://github.com/junit-team/junit5";
        linkService.add(new AddLinkRequest(chatId, url, Set.of()));

        // When
        LinkResponse response =
                linkService.remove(new backend.academy.linktracker.scrapper.dto.RemoveLinkRequest(chatId, url));

        // Then
        assertThat(response).isNotNull();
        assertThat(response.url()).isEqualTo(url);
    }

    @Test
    void removeLink_shouldThrowExceptionWhenLinkNotFound() {
        // Given
        Long chatId = 44444L;
        chatService.register(chatId);
        String nonExistentUrl = "https://github.com/non-existent/repository";

        // When & Then
        assertThatThrownBy(() -> linkService.remove(
                        new backend.academy.linktracker.scrapper.dto.RemoveLinkRequest(chatId, nonExistentUrl)))
                .isInstanceOf(LinkNotFoundException.class);
    }
}
