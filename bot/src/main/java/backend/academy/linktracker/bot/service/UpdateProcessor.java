package backend.academy.linktracker.bot.service;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.UpdatesListener;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.request.SendMessage;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class UpdateProcessor {

    private final TelegramBot telegramBot;
    private final CommandDispatcher commandDispatcher;
    private final StateMachineProcessor stateMachineProcessor;

    @PostConstruct
    public void start() {
        log.info("Starting updates listener");

        telegramBot.setUpdatesListener(updates -> {
            processUpdates(updates);
            return UpdatesListener.CONFIRMED_UPDATES_ALL;
        });

        log.info("Updates listener started successfully");
    }

    @PreDestroy
    public void stop() {
        log.info("Stopping updates listener");
        telegramBot.removeGetUpdatesListener();
        log.info("Updates listener stopped");
    }

    private void processUpdates(List<Update> updates) {
        for (Update update : updates) {
            try {
                processUpdate(update);
            } catch (Exception e) {
                log.error("Error processing update: updateId={}", update.updateId(), e);
            }
        }
    }

    private void processUpdate(Update update) {
        log.debug("Processing update: updateId={}", update.updateId());

        SendMessage response;

        if (stateMachineProcessor.shouldProcess(update)) {
            response = stateMachineProcessor.process(update);
        } else {
            response = commandDispatcher.dispatch(update);
        }

        if (response != null) {
            var result = telegramBot.execute(response);
            if (!result.isOk()) {
                log.error(
                        "Failed to send message: updateId={}, errorCode={}, description={}",
                        update.updateId(),
                        result.errorCode(),
                        result.description());
            }
        }
    }
}
