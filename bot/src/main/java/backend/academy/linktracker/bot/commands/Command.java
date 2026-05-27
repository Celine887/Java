package backend.academy.linktracker.bot.commands;

import com.pengrad.telegrambot.model.Update;

public interface Command {
    String getName();
    String description();
    void execute(Update update);
}
