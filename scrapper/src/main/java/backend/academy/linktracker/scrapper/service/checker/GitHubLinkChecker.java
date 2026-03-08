package backend.academy.linktracker.scrapper.service.checker;

import backend.academy.linktracker.scrapper.client.GitHubClient;
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
public class GitHubLinkChecker implements LinkChecker {

    private static final Pattern GITHUB_PATTERN = Pattern.compile("https://github\\.com/([\\w-]+)/([\\w.-]+)/?");

    private final GitHubClient gitHubClient;

    @Override
    public boolean supports(String url) {
        return GITHUB_PATTERN.matcher(url).matches();
    }

    @Override
    public Optional<Instant> getLastUpdate(String url) {
        Matcher matcher = GITHUB_PATTERN.matcher(url);
        if (!matcher.matches()) {
            return Optional.empty();
        }

        String owner = matcher.group(1);
        String repo = matcher.group(2);

        return gitHubClient.fetchRepository(owner, repo).map(response -> response.pushedAt()
                .toInstant());
    }
}
