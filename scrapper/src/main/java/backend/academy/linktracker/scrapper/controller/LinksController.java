package backend.academy.linktracker.scrapper.controller;

import backend.academy.linktracker.scrapper.dto.AddLinkRequest;
import backend.academy.linktracker.scrapper.dto.LinkResponse;
import backend.academy.linktracker.scrapper.dto.ListLinksResponse;
import backend.academy.linktracker.scrapper.dto.RemoveLinkRequest;
import backend.academy.linktracker.scrapper.service.LinkService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/links")
public class LinksController {
    private static final Logger log = LoggerFactory.getLogger(LinksController.class);

    private final LinkService linkService;

    public LinksController(LinkService linkService) {
        this.linkService = linkService;
    }

    @GetMapping
    public ResponseEntity<ListLinksResponse> getLinks(
            @RequestHeader("Tg-Chat-Id") Long chatId, @RequestParam(required = false) String tag) {
        log.info("Getting links: chatId={}, tag={}", chatId, tag);
        ListLinksResponse response = linkService.listAll(chatId, tag);
        return ResponseEntity.ok(response);
    }

    @PostMapping
    public ResponseEntity<LinkResponse> addLink(
            @RequestHeader("Tg-Chat-Id") Long chatId, @Valid @RequestBody AddLinkRequest request) {
        log.info("Adding link: chatId={}, url={}, tags={}", chatId, request.url(), request.tags());
        AddLinkRequest fullRequest = new AddLinkRequest(chatId, request.url(), request.tags());
        LinkResponse response = linkService.add(fullRequest);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping
    public ResponseEntity<LinkResponse> deleteLink(
            @RequestHeader("Tg-Chat-Id") Long chatId, @Valid @RequestBody RemoveLinkRequest request) {
        log.info("Removing link: chatId={}, url={}", chatId, request.url());
        RemoveLinkRequest fullRequest = new RemoveLinkRequest(chatId, request.url());
        LinkResponse response = linkService.remove(fullRequest);
        return ResponseEntity.ok(response);
    }
}
