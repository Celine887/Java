package backend.academy.linktracker.bot.command;

import backend.academy.linktracker.bot.model.TrackedLink;
import backend.academy.linktracker.bot.repository.LinkRepository;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.request.SendMessage;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class ListCommand implements Command {

    private final LinkRepository linkRepository;

    @Override
    public String getName() {
        return "list";
    }

    @Override
    public String getDescription() {
        return "Показать все отслеживаемые ссылки";
    }

    @Override
    public SendMessage handle(Update update) {
        Long chatId = update.message().chat().id();
        String text = update.message().text().trim();
        String[] parts = text.split("\\s+", 2);

        String tag = parts.length > 1 ? parts[1] : null;
        log.info("List command: chatId={}, tag={}", chatId, tag);

        List<TrackedLink> links = tag != null
                ? linkRepository.findAllByChatIdAndTag(chatId, tag)
                : linkRepository.findAllByChatId(chatId);

        if (links.isEmpty()) {
            String message = tag != null
                    ? "У вас нет отслеживаемых ссылок с тегом: " + tag
                    : "У вас пока нет отслеживаемых ссылок. Используйте /track для добавления.";
            return new SendMessage(chatId, message);
        }

        StringBuilder response = new StringBuilder("Ваши отслеживаемые ссылки");
        if (tag != null) {
            response.append(" (тег: ").append(tag).append(")");
        }
        response.append(":\n\n");

        for (TrackedLink link : links) {
            response.append("• ").append(link.url());
            if (link.tags() != null && !link.tags().isEmpty()) {
                response.append("\n  Теги: ").append(String.join(", ", link.tags()));
            }
            response.append("\n\n");
        }

        return new SendMessage(chatId, response.toString());
    }
}
