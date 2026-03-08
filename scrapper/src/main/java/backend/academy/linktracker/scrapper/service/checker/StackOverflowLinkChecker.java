package backend.academy.linktracker.scrapper.service.checker;

import backend.academy.linktracker.scrapper.client.StackOverflowClient;
import java.time.Instant;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class StackOverflowLinkChecker implements LinkChecker {

    private static final Pattern STACKOVERFLOW_PATTERN =
            Pattern.compile("https://stackoverflow\\.com/questions/(\\d+)/[\\w-]+/?");

    private final StackOverflowClient stackOverflowClient;

    @Override
    public boolean supports(String url) {
        return STACKOVERFLOW_PATTERN.matcher(url).matches();
    }

    @Override
    public Optional<Instant> getLastUpdate(String url) {
        Matcher matcher = STACKOVERFLOW_PATTERN.matcher(url);
        if (!matcher.matches()) {
            return Optional.empty();
        }

        Long questionId = Long.parseLong(matcher.group(1));

        return stackOverflowClient.fetchQuestion(questionId).map(question -> question.getLastActivityInstant());
    }
}
