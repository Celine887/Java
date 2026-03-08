package backend.academy.linktracker.bot.service;

import backend.academy.linktracker.bot.model.TrackedLink;
import backend.academy.linktracker.bot.model.UserContext;
import backend.academy.linktracker.bot.model.UserState;
import backend.academy.linktracker.bot.repository.LinkRepository;
import backend.academy.linktracker.bot.repository.UserStateRepository;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.request.SendMessage;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class StateMachineProcessor {

    private final UserStateRepository userStateRepository;
    private final LinkRepository linkRepository;
    private final LinkValidator linkValidator;

    public SendMessage process(Update update) {
        Long chatId = update.message().chat().id();
        String text = update.message().text();
        UserContext context = userStateRepository.getContext(chatId);

        log.debug("Processing state machine: chatId={}, state={}, text={}", chatId, context.getState(), text);

        return switch (context.getState()) {
            case WAITING_FOR_LINK -> handleLinkInput(chatId, text, context);
            case WAITING_FOR_TAGS -> handleTagsInput(chatId, text, context);
            default -> null;
        };
    }

    private SendMessage handleLinkInput(Long chatId, String url, UserContext context) {
        if (!linkValidator.isValid(url)) {
            return new SendMessage(
                    chatId, "Неверная ссылка. Пожалуйста, отправьте корректную ссылку GitHub или StackOverflow.");
        }

        if (linkRepository.existsByChatIdAndUrl(chatId, url)) {
            userStateRepository.clearContext(chatId);
            return new SendMessage(chatId, "Ссылка уже отслеживается");
        }

        context.setPendingUrl(url);
        context.setState(UserState.WAITING_FOR_TAGS);
        userStateRepository.saveContext(chatId, context);

        return new SendMessage(
                chatId,
                "Ссылка принята! Теперь отправьте теги (через запятую) или отправьте /skip для продолжения без тегов.");
    }

    private SendMessage handleTagsInput(Long chatId, String text, UserContext context) {
        Set<String> tags = new HashSet<>();
        if (!text.equals("/skip")) {
            tags = new HashSet<>(Arrays.asList(text.split(",\\s*")));
        }

        TrackedLink link = new TrackedLink(context.getPendingUrl(), tags);

        linkRepository.save(chatId, link);
        userStateRepository.clearContext(chatId);

        log.info("Link tracked: chatId={}, url={}, tags={}", chatId, link.url(), tags);

        return new SendMessage(
                chatId,
                "Ссылка успешно добавлена в отслеживание!\n" + "URL: " + link.url() + "\n" + "Теги: "
                        + (tags.isEmpty() ? "нет" : String.join(", ", tags)));
    }

    public boolean shouldProcess(Update update) {
        if (update.message() == null || update.message().text() == null) {
            return false;
        }
        Long chatId = update.message().chat().id();
        UserContext context = userStateRepository.getContext(chatId);
        return context.getState() != UserState.IDLE;
    }
}
