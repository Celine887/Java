package backend.academy.linktracker.bot.commands;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.request.SendMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class HelpCommand implements Command{
    private final TelegramBot bot;
    public HelpCommand(TelegramBot bot){
        this.bot = bot;
    }
    @Override
    public String getName() {
        return "/help";
    }

    @Override
    public String description() {
        log.info("Description for /help was executed");
        return "show you help info";
    }

    @Override
    public void execute(Update update) {
        String chatId = update.message().chat().id().toString();
        log.info("Message for /help was sent to {}", chatId);
        bot.execute(new SendMessage(chatId, "This description shows for /help command"));
    }
}
