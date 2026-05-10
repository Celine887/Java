package backend.academy.linktracker.bot.service;

import backend.academy.linktracker.bot.command.Command;
import backend.academy.linktracker.bot.command.UnknownCommand;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.request.SendMessage;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class CommandDispatcher {

    private final List<Command> commands;
    private final UnknownCommand unknownCommand;
    private final backend.academy.linktracker.bot.repository.UserStateRepository userStateRepository;

    public SendMessage dispatch(Update update) {
        if (update.message() == null || update.message().text() == null) {
            log.debug("Update without message or text: updateId={}", update.updateId());
            return null;
        }

        Long chatId = update.message().chat().id();
        String text = update.message().text().trim();

        if (text.startsWith("/")
                && !text.startsWith("/skip")
                && userStateRepository.getContext(chatId).getState()
                        != backend.academy.linktracker.bot.model.UserState.IDLE) {
            userStateRepository.clearContext(chatId);
        }

        Command command = commands.stream()
                .filter(cmd -> cmd.supports(update))
                .findFirst()
                .orElse(unknownCommand);

        log.debug(
                "Command dispatched: updateId={}, command={}, chatId={}",
                update.updateId(),
                command.getName(),
                update.message().chat().id());

        return command.handle(update);
    }
}
