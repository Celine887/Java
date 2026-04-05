package backend.academy.linktracker.scrapper.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import backend.academy.linktracker.scrapper.client.UpdateClient;
import backend.academy.linktracker.scrapper.model.Link;
import backend.academy.linktracker.scrapper.model.LinkUpdateInfo;
import backend.academy.linktracker.scrapper.repository.LinkRepository;
import backend.academy.linktracker.scrapper.service.checker.LinkChecker;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class LinkUpdateServiceTest {

    @Mock
    private LinkChecker linkChecker;

    @Mock
    private LinkRepository linkRepository;

    @Mock
    private UpdateClient updateClient;

    private LinkUpdateService linkUpdateService;
    private ExecutorService executorService;

    @BeforeEach
    void setUp() {
        executorService = Executors.newFixedThreadPool(2);
        linkUpdateService =
                new LinkUpdateService(List.of(linkChecker), linkRepository, updateClient, executorService, 100, 50);
    }

    @Test
    void shouldProcessLinksInBatches() {
        // Given
        Link link1 = new Link("https://github.com/user/repo1", Set.of(), Instant.now(), null);
        Link link2 = new Link("https://github.com/user/repo2", Set.of(), Instant.now(), null);

        when(linkRepository.findLinksForUpdate(100, 0)).thenReturn(List.of(link1, link2));
        when(linkRepository.findLinksForUpdate(100, 100)).thenReturn(List.of());
        when(linkChecker.supports(anyString())).thenReturn(true);
        when(linkChecker.checkForUpdates(anyString(), any(Instant.class))).thenReturn(List.of());

        // When
        linkUpdateService.updateAllLinks();

        // Then
        verify(linkRepository).findLinksForUpdate(100, 0);
        verify(linkRepository).findLinksForUpdate(100, 100);
        verify(linkRepository, times(2)).updateLink(any(Link.class));
    }

    @Test
    void shouldHandleErrorsGracefully() {
        // Given
        Link link1 = new Link("https://github.com/user/repo1", Set.of(), Instant.now(), null);
        Link link2 = new Link("https://github.com/user/repo2", Set.of(), Instant.now(), null);

        when(linkRepository.findLinksForUpdate(100, 0)).thenReturn(List.of(link1, link2));
        when(linkRepository.findLinksForUpdate(100, 100)).thenReturn(List.of());
        when(linkChecker.supports(anyString())).thenReturn(true);
        when(linkChecker.checkForUpdates(eq(link1.url()), any(Instant.class)))
                .thenThrow(new RuntimeException("API error"));
        when(linkChecker.checkForUpdates(eq(link2.url()), any(Instant.class))).thenReturn(List.of());

        // When
        linkUpdateService.updateAllLinks();

        // Then - second link should still be processed
        verify(linkRepository).updateLink(argThat(link -> link.url().equals(link2.url())));
    }

    @Test
    void shouldSendNotificationsForUpdates() {
        // Given
        Link link = new Link("https://github.com/user/repo", Set.of(), Instant.now(), null);
        LinkUpdateInfo update = new LinkUpdateInfo(
                LinkUpdateInfo.UpdateType.GITHUB_ISSUE,
                "Test Issue",
                "testuser",
                Instant.now(),
                "Test preview",
                "https://github.com/user/repo/issues/1");

        when(linkRepository.findLinksForUpdate(100, 0)).thenReturn(List.of(link));
        when(linkRepository.findLinksForUpdate(100, 100)).thenReturn(List.of());
        when(linkChecker.supports(anyString())).thenReturn(true);
        when(linkChecker.checkForUpdates(anyString(), any(Instant.class))).thenReturn(List.of(update));
        when(linkRepository.findChatIdsByUrl(anyString())).thenReturn(List.of(123L));

        // When
        linkUpdateService.updateAllLinks();

        // Then
        verify(updateClient).sendUpdate(any());
        verify(linkRepository).updateLink(any(Link.class));
    }
}
