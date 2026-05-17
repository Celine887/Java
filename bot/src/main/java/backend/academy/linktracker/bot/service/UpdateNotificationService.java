package backend.academy.linktracker.bot.service;

import backend.academy.linktracker.bot.dto.LinkUpdateRequest;
import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.request.SendMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class UpdateNotificationService {

    private final TelegramBot telegramBot;

    public void sendNotifications(LinkUpdateRequest request) {
        String message =
                String.format("🔔 Update detected!%n%nLink: %s%nDescription: %s", request.url(), request.description());

        for (Long chatId : request.tgChatIds()) {
            try {
                SendMessage sendMessage = new SendMessage(chatId, message);
                var result = telegramBot.execute(sendMessage);

                if (!result.isOk()) {
                    log.error(
                            "Failed to send notification: chatId={}, errorCode={}, description={}",
                            chatId,
                            result.errorCode(),
                            result.description());
                }
            } catch (Exception e) {
                log.error("Error sending notification to chatId={}", chatId, e);
            }
        }
    }
}
