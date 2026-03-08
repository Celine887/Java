package backend.academy.linktracker.bot.command;

import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.request.SendMessage;

public interface Command {

    String getName();

    String getDescription();

    SendMessage handle(Update update);

    default boolean supports(Update update) {
        if (update.message() == null || update.message().text() == null) {
            return false;
        }
        String text = update.message().text().trim();
        return text.equals("/" + getName()) || text.startsWith("/" + getName() + " ");
    }
}
