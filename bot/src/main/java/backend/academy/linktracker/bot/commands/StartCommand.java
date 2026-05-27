package backend.academy.linktracker.bot.commands;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.request.SendMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
@Slf4j
@Component
public class StartCommand implements Command{
    private TelegramBot bot;
    public StartCommand(TelegramBot bot){
        this.bot = bot;
    }
    @Override
    public String getName() {
        return "/start";
    }

    @Override
    public String description() {
        log.info("Description for /start was executed");
        return "click for start";
    }

    @Override
    public void execute(Update update) {
        long chatId = update.message().chat().id();
        String name = update.message().chat().username();
        log.info("Message for /start was sent to {}, {}", chatId, name);
        SendMessage message = new SendMessage(chatId, "Hello " + name + ", this is testBot.");
        bot.execute(message);
    }
}
