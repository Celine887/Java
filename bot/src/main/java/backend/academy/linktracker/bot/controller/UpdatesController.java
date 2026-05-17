package backend.academy.linktracker.bot.controller;

import backend.academy.linktracker.bot.dto.LinkUpdateRequest;
import backend.academy.linktracker.bot.service.UpdateNotificationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/updates")
@RequiredArgsConstructor
@Slf4j
public class UpdatesController {

    private final UpdateNotificationService updateNotificationService;

    @PostMapping
    public ResponseEntity<Void> processUpdate(@Valid @RequestBody LinkUpdateRequest request) {
        log.info("Received update: url={}, chatIds={}", request.url(), request.tgChatIds());
        updateNotificationService.sendNotifications(request);
        return ResponseEntity.ok().build();
    }
}
