package backend.academy.linktracker.scrapper.service;

import backend.academy.linktracker.scrapper.dto.AddLinkRequest;
import backend.academy.linktracker.scrapper.dto.LinkResponse;
import backend.academy.linktracker.scrapper.dto.ListLinksResponse;
import backend.academy.linktracker.scrapper.dto.RemoveLinkRequest;
import backend.academy.linktracker.scrapper.exception.ChatNotFoundException;
import backend.academy.linktracker.scrapper.exception.InvalidUrlException;
import backend.academy.linktracker.scrapper.exception.LinkAlreadyTrackedException;
import backend.academy.linktracker.scrapper.exception.LinkNotFoundException;
import backend.academy.linktracker.scrapper.model.Link;
import backend.academy.linktracker.scrapper.repository.LinkRepository;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LinkService {

    private final LinkRepository linkRepository;
    private final ChatService chatService;
    private final LinkValidator linkValidator;

    public LinkResponse add(AddLinkRequest request) {
        if (!chatService.exists(request.chatId())) {
            throw new ChatNotFoundException(request.chatId());
        }

        if (!linkValidator.isValid(request.url())) {
            throw new InvalidUrlException(request.url());
        }

        if (linkRepository.findByChatIdAndUrl(request.chatId(), request.url()).isPresent()) {
            throw new LinkAlreadyTrackedException(request.url());
        }

        Link link = new Link(
                request.url(),
                request.tags() != null ? new HashSet<>(request.tags()) : new HashSet<>(),
                Instant.now(),
                Instant.now());

        linkRepository.save(request.chatId(), link);
        return new LinkResponse(link.url(), link.tags());
    }

    public LinkResponse remove(RemoveLinkRequest request) {
        if (!chatService.exists(request.chatId())) {
            throw new ChatNotFoundException(request.chatId());
        }

        Link link = linkRepository
                .findByChatIdAndUrl(request.chatId(), request.url())
                .orElseThrow(() -> new LinkNotFoundException(request.url()));

        linkRepository.deleteByChatIdAndUrl(request.chatId(), request.url());
        return new LinkResponse(link.url(), link.tags());
    }

    public ListLinksResponse listAll(Long chatId, String tag) {
        if (!chatService.exists(chatId)) {
            throw new ChatNotFoundException(chatId);
        }

        List<Link> links = tag != null
                ? linkRepository.findAllByChatIdAndTag(chatId, tag)
                : linkRepository.findAllByChatId(chatId);

        List<LinkResponse> responses = links.stream()
                .map(link -> new LinkResponse(link.url(), link.tags()))
                .toList();

        return new ListLinksResponse(responses, responses.size());
    }
}
