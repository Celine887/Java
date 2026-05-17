package backend.academy.linktracker.bot.command;

import backend.academy.linktracker.bot.model.TrackedLink;
import backend.academy.linktracker.bot.model.UserContext;
import backend.academy.linktracker.bot.model.UserState;
import backend.academy.linktracker.bot.repository.LinkRepository;
import backend.academy.linktracker.bot.repository.UserStateRepository;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.request.SendMessage;
import java.util.HashSet;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class SkipCommand implements Command {

    private final UserStateRepository userStateRepository;
    private final LinkRepository linkRepository;

    @Override
    public String getName() {
        return "skip";
    }

    @Override
    public String getDescription() {
        return "Пропустить добавление тегов";
    }

    @Override
    public SendMessage handle(Update update) {
        Long chatId = update.message().chat().id();
        UserContext context = userStateRepository.getContext(chatId);

        if (context.getState() != UserState.WAITING_FOR_TAGS) {
            return new SendMessage(chatId, "Нечего пропускать.");
        }

        TrackedLink link = new TrackedLink(context.getPendingUrl(), new HashSet<>());

        linkRepository.save(chatId, link);
        userStateRepository.clearContext(chatId);

        log.info("Link tracked without tags: chatId={}, url={}", chatId, link.url());

        return new SendMessage(chatId, "Ссылка успешно добавлена в отслеживание без тегов!\nURL: " + link.url());
    }
}
