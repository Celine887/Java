package backend.academy.linktracker.bot;


import backend.academy.linktracker.bot.commands.Command;
import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.UpdatesListener;
import com.pengrad.telegrambot.model.BotCommand;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.request.SendMessage;
import com.pengrad.telegrambot.request.SetMyCommands;
import com.pengrad.telegrambot.response.SendResponse;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class BotListener {
    public TelegramBot tgbot;
    private final Map<String, Command> commandMap;

    public BotListener(TelegramBot tgbot, List<Command> commandList){
        this.tgbot = tgbot;
        commandMap = commandList.stream().collect(Collectors.toMap(Command::getName, command -> command));
    }
    @PostConstruct
    public void init(){
        BotCommand[] myCommands = commandMap.values().stream().map(c->{
            String name = c.getName().replace("/", "");
            return new BotCommand(name, c.description());
        }).toArray(BotCommand[]::new);
        tgbot.execute(new SetMyCommands(myCommands));
        tgbot.setUpdatesListener(updates ->{
            for (Update update : updates){
                if (update.message() != null && update.message().text() != null){
                    String text = update.message().text().trim();
                    String command = text.split(" ")[0];
                    Command tgCommand = commandMap.get(command);
                    if (tgCommand != null){
                        tgCommand.execute(update);
                    }else {
                        String chatId = update.message().chat().id().toString();
                        SendResponse thereIsNoExpectCommand = tgbot.execute(new SendMessage(chatId, "There is no expect command"));
                    }
                }
            }
            return UpdatesListener.CONFIRMED_UPDATES_ALL;
        }, e->{
            if (e.response() != null){
                e.response().errorCode();
            }else{
                e.printStackTrace();
            }
        });
    }

}
