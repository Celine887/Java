package backend.academy.linktracker.scrapper.service.checker;

import backend.academy.linktracker.scrapper.client.GitHubClient;
import backend.academy.linktracker.scrapper.client.dto.GitHubIssueResponse;
import backend.academy.linktracker.scrapper.client.dto.GitHubPullRequestResponse;
import backend.academy.linktracker.scrapper.model.LinkUpdateInfo;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
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

    @Override
    public List<LinkUpdateInfo> checkForUpdates(String url, Instant lastChecked) {
        Matcher matcher = GITHUB_PATTERN.matcher(url);
        if (!matcher.matches()) {
            return List.of();
        }

        String owner = matcher.group(1);
        String repo = matcher.group(2);
        OffsetDateTime since = OffsetDateTime.ofInstant(lastChecked, ZoneOffset.UTC);

        List<LinkUpdateInfo> updates = new ArrayList<>();

        List<GitHubIssueResponse.IssueItem> issues = gitHubClient.fetchIssues(owner, repo, since);
        for (GitHubIssueResponse.IssueItem issue : issues) {
            if (issue.createdAt().isAfter(since)) {
                String issueUrl = String.format("https://github.com/%s/%s/issues/%d", owner, repo, issue.id());
                updates.add(new LinkUpdateInfo(
                        LinkUpdateInfo.UpdateType.GITHUB_ISSUE,
                        issue.title(),
                        issue.user().login(),
                        issue.createdAt().toInstant(),
                        issue.getPreview(),
                        issueUrl));
            }
        }

        List<GitHubPullRequestResponse.PullRequestItem> pullRequests =
                gitHubClient.fetchPullRequests(owner, repo, since);
        for (GitHubPullRequestResponse.PullRequestItem pr : pullRequests) {
            String prUrl = String.format("https://github.com/%s/%s/pull/%d", owner, repo, pr.id());
            updates.add(new LinkUpdateInfo(
                    LinkUpdateInfo.UpdateType.GITHUB_PULL_REQUEST,
                    pr.title(),
                    pr.user().login(),
                    pr.createdAt().toInstant(),
                    pr.getPreview(),
                    prUrl));
        }

        log.debug("Found {} updates for GitHub repo: {}/{}", updates.size(), owner, repo);
        return updates;
    }
}
