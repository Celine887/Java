package backend.academy.linktracker.bot.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import backend.academy.linktracker.bot.model.TrackedLink;
import backend.academy.linktracker.bot.model.UserContext;
import backend.academy.linktracker.bot.model.UserState;
import backend.academy.linktracker.bot.repository.LinkRepository;
import backend.academy.linktracker.bot.repository.UserStateRepository;
import com.pengrad.telegrambot.model.Chat;
import com.pengrad.telegrambot.model.Message;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.request.SendMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class StateMachineProcessorTest {

    @Mock
    private UserStateRepository userStateRepository;

    @Mock
    private LinkRepository linkRepository;

    @Mock
    private LinkValidator linkValidator;

    @Mock
    private Update update;

    @Mock
    private Message message;

    @Mock
    private Chat chat;

    private StateMachineProcessor processor;

    private static final Long CHAT_ID = 12345L;

    @BeforeEach
    void setUp() {
        processor = new StateMachineProcessor(userStateRepository, linkRepository, linkValidator);

        when(update.message()).thenReturn(message);
        when(message.chat()).thenReturn(chat);
        when(chat.id()).thenReturn(CHAT_ID);
    }

    @Test
    void process_shouldHandleValidLinkInput() {
        // Given
        String url = "https://github.com/user/repo";
        when(message.text()).thenReturn(url);
        when(linkValidator.isValid(url)).thenReturn(true);
        when(linkRepository.existsByChatIdAndUrl(CHAT_ID, url)).thenReturn(false);

        UserContext context = new UserContext(UserState.WAITING_FOR_LINK, null);
        when(userStateRepository.getContext(CHAT_ID)).thenReturn(context);

        // When
        SendMessage result = processor.process(update);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getParameters().get("text").toString()).contains("теги");

        ArgumentCaptor<UserContext> contextCaptor = ArgumentCaptor.forClass(UserContext.class);
        verify(userStateRepository).saveContext(eq(CHAT_ID), contextCaptor.capture());

        UserContext savedContext = contextCaptor.getValue();
        assertThat(savedContext.getState()).isEqualTo(UserState.WAITING_FOR_TAGS);
        assertThat(savedContext.getPendingUrl()).isEqualTo(url);
    }

    @Test
    void process_shouldRejectInvalidLinkInput() {
        // Given
        String invalidUrl = "not-a-url";
        when(message.text()).thenReturn(invalidUrl);
        when(linkValidator.isValid(invalidUrl)).thenReturn(false);

        UserContext context = new UserContext(UserState.WAITING_FOR_LINK, null);
        when(userStateRepository.getContext(CHAT_ID)).thenReturn(context);

        // When
        SendMessage result = processor.process(update);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getParameters().get("text").toString()).contains("Неверная ссылка");
    }

    @Test
    void process_shouldRejectDuplicateLink() {
        // Given
        String url = "https://github.com/user/repo";
        when(message.text()).thenReturn(url);
        when(linkValidator.isValid(url)).thenReturn(true);
        when(linkRepository.existsByChatIdAndUrl(CHAT_ID, url)).thenReturn(true);

        UserContext context = new UserContext(UserState.WAITING_FOR_LINK, null);
        when(userStateRepository.getContext(CHAT_ID)).thenReturn(context);

        // When
        SendMessage result = processor.process(update);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getParameters().get("text").toString()).contains("уже отслеживается");
        verify(userStateRepository).clearContext(CHAT_ID);
    }

    @Test
    void process_shouldHandleTagsInput() {
        // Given
        String tags = "java, spring, test";
        String url = "https://github.com/user/repo";
        when(message.text()).thenReturn(tags);

        UserContext context = new UserContext(UserState.WAITING_FOR_TAGS, url);
        when(userStateRepository.getContext(CHAT_ID)).thenReturn(context);

        // When
        SendMessage result = processor.process(update);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getParameters().get("text").toString()).contains("успешно добавлена");

        ArgumentCaptor<TrackedLink> linkCaptor = ArgumentCaptor.forClass(TrackedLink.class);
        verify(linkRepository).save(eq(CHAT_ID), linkCaptor.capture());

        TrackedLink savedLink = linkCaptor.getValue();
        assertThat(savedLink.url()).isEqualTo(url);
        assertThat(savedLink.tags()).containsExactlyInAnyOrder("java", "spring", "test");

        verify(userStateRepository).clearContext(CHAT_ID);
    }

    @Test
    void process_shouldHandleSkipCommand() {
        // Given
        String url = "https://github.com/user/repo";
        when(message.text()).thenReturn("/skip");

        UserContext context = new UserContext(UserState.WAITING_FOR_TAGS, url);
        when(userStateRepository.getContext(CHAT_ID)).thenReturn(context);

        // When
        SendMessage result = processor.process(update);

        // Then
        assertThat(result).isNotNull();

        ArgumentCaptor<TrackedLink> linkCaptor = ArgumentCaptor.forClass(TrackedLink.class);
        verify(linkRepository).save(eq(CHAT_ID), linkCaptor.capture());

        TrackedLink savedLink = linkCaptor.getValue();
        assertThat(savedLink.tags()).isEmpty();
    }

    @Test
    void shouldProcess_shouldReturnTrueWhenNotIdle() {
        // Given
        when(message.text()).thenReturn("some text");
        UserContext context = new UserContext(UserState.WAITING_FOR_LINK, null);
        when(userStateRepository.getContext(CHAT_ID)).thenReturn(context);

        // When
        boolean result = processor.shouldProcess(update);

        // Then
        assertThat(result).isTrue();
    }

    @Test
    void shouldProcess_shouldReturnFalseWhenIdle() {
        // Given
        when(message.text()).thenReturn("some text");
        UserContext context = new UserContext(UserState.IDLE, null);
        when(userStateRepository.getContext(CHAT_ID)).thenReturn(context);

        // When
        boolean result = processor.shouldProcess(update);

        // Then
        assertThat(result).isFalse();
    }
}
