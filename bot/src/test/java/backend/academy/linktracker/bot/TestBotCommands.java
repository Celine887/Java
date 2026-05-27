package backend.academy.linktracker.bot;

import backend.academy.linktracker.bot.commands.Command;
import backend.academy.linktracker.bot.commands.StartCommand;
import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.Chat;
import com.pengrad.telegrambot.model.Message;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.request.SendMessage;
import net.datafaker.providers.base.Company;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.ArrayList;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;

@ExtendWith(MockitoExtension.class)
public class TestBotCommands {
    @Mock
    TelegramBot fakeBot;
    @Mock
    Update fakeUpdate = new Update();
    @Mock
    Message fakeMessage = new Message();
    @Mock
    Chat fakeChat = new Chat();

    @Test
    public void TestStartCommand(){
        // when
        Mockito.when(fakeUpdate.message()).thenReturn(fakeMessage);
        Mockito.when(fakeMessage.chat()).thenReturn(fakeChat);
        Mockito.when(fakeChat.id()).thenReturn(1l);

        //then
        StartCommand command = new StartCommand(fakeBot);
        command.execute(fakeUpdate);

        // assert
        Mockito.verify(fakeBot, Mockito.times(1)).execute(any(SendMessage.class));

    }
}
