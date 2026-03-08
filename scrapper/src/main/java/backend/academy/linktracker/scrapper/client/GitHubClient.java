package backend.academy.linktracker.scrapper.client;

import backend.academy.linktracker.scrapper.client.dto.GitHubRepositoryResponse;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
@Slf4j
public class GitHubClient {

    private final RestClient restClient;

    public GitHubClient(@Value("${app.github-client.base-url}") String baseUrl) {
        this.restClient = RestClient.builder().baseUrl(baseUrl).build();
    }

    public Optional<GitHubRepositoryResponse> fetchRepository(String owner, String repo) {
        try {
            log.debug("Fetching GitHub repository: owner={}, repo={}", owner, repo);
            GitHubRepositoryResponse response = restClient
                    .get()
                    .uri("/repos/{owner}/{repo}", owner, repo)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (request, responseEntity) -> {
                        log.error(
                                "GitHub API error: status={}, body={}",
                                responseEntity.getStatusCode(),
                                new String(responseEntity.getBody().readAllBytes()));
                    })
                    .body(GitHubRepositoryResponse.class);
            return Optional.ofNullable(response);
        } catch (Exception e) {
            log.error("Failed to fetch GitHub repository: owner={}, repo={}", owner, repo, e);
            return Optional.empty();
        }
    }
}
