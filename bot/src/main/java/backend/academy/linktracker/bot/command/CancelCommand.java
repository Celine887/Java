package backend.academy.linktracker.bot.command;

import backend.academy.linktracker.bot.repository.UserStateRepository;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.request.SendMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class CancelCommand implements Command {

    private final UserStateRepository userStateRepository;

    @Override
    public String getName() {
        return "cancel";
    }

    @Override
    public String getDescription() {
        return "Отменить текущую операцию";
    }

    @Override
    public SendMessage handle(Update update) {
        Long chatId = update.message().chat().id();
        log.info("Cancel command: chatId={}", chatId);

        userStateRepository.clearContext(chatId);
        return new SendMessage(chatId, "Операция отменена.");
    }
}
