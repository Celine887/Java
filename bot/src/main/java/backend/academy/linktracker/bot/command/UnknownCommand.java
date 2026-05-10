package backend.academy.linktracker.bot.command;

import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.request.SendMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class UnknownCommand implements Command {

    @Override
    public String getName() {
        return "unknown";
    }

    @Override
    public String getDescription() {
        return "Обработка неизвестных команд";
    }

    @Override
    public SendMessage handle(Update update) {
        Long chatId = update.message().chat().id();
        String text = update.message().text();

        log.warn("Unknown command received: chatId={}, command={}", chatId, text);

        String errorMessage = """
                ❌ Неизвестная команда.

                Воспользуйтесь /help, чтобы посмотреть список доступных команд.
                """;

        return new SendMessage(chatId, errorMessage);
    }

    @Override
    public boolean supports(Update update) {
        return update.message() != null && update.message().text() != null;
    }
}
