package backend.academy.linktracker.bot.command;

import backend.academy.linktracker.bot.repository.LinkRepository;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.request.SendMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class UntrackCommand implements Command {

    private final LinkRepository linkRepository;

    @Override
    public String getName() {
        return "untrack";
    }

    @Override
    public String getDescription() {
        return "Прекратить отслеживание ссылки";
    }

    @Override
    public SendMessage handle(Update update) {
        Long chatId = update.message().chat().id();
        String text = update.message().text().trim();
        String[] parts = text.split("\\s+", 2);

        if (parts.length < 2) {
            return new SendMessage(chatId, "Использование: /untrack <url>");
        }

        String url = parts[1];
        log.info("Untrack command: chatId={}, url={}", chatId, url);

        if (!linkRepository.existsByChatIdAndUrl(chatId, url)) {
            return new SendMessage(chatId, "Вы не отслеживаете эту ссылку.");
        }

        linkRepository.deleteByChatIdAndUrl(chatId, url);
        return new SendMessage(chatId, "Ссылка удалена из отслеживания: " + url);
    }
}
