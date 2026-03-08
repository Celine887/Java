package backend.academy.linktracker.scrapper.controller;

import backend.academy.linktracker.scrapper.service.ChatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/tg-chat")
@RequiredArgsConstructor
@Slf4j
public class ChatController {

    private final ChatService chatService;

    @PostMapping("/{id}")
    public ResponseEntity<Void> registerChat(@PathVariable Long id) {
        log.info("Registering chat: chatId={}", id);
        chatService.register(id);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteChat(@PathVariable Long id) {
        log.info("Deleting chat: chatId={}", id);
        chatService.unregister(id);
        return ResponseEntity.ok().build();
    }
}
