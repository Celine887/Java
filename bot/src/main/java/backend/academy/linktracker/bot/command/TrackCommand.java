package backend.academy.linktracker.bot.command;

import backend.academy.linktracker.bot.model.UserContext;
import backend.academy.linktracker.bot.model.UserState;
import backend.academy.linktracker.bot.repository.UserStateRepository;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.request.SendMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class TrackCommand implements Command {

    private final UserStateRepository userStateRepository;

    @Override
    public String getName() {
        return "track";
    }

    @Override
    public String getDescription() {
        return "Начать отслеживание ссылки";
    }

    @Override
    public SendMessage handle(Update update) {
        Long chatId = update.message().chat().id();
        log.info("Track command initiated: chatId={}", chatId);

        userStateRepository.saveContext(chatId, new UserContext(UserState.WAITING_FOR_LINK, null));

        return new SendMessage(
                chatId,
                "Отправьте мне ссылку для отслеживания (GitHub или StackOverflow).\n"
                        + "Отправьте /cancel для отмены.");
    }
}
