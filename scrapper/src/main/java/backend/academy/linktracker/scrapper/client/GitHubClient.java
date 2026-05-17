package backend.academy.linktracker.scrapper.client;

import backend.academy.linktracker.scrapper.client.dto.GitHubIssueResponse;
import backend.academy.linktracker.scrapper.client.dto.GitHubPullRequestResponse;
import backend.academy.linktracker.scrapper.client.dto.GitHubRepositoryResponse;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryRegistry;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class GitHubClient {
    private static final Logger log = LoggerFactory.getLogger(GitHubClient.class);

    private final RestClient restClient;
    private final CircuitBreaker circuitBreaker;
    private final Retry retry;

    public GitHubClient(
            @Value("${app.github-client.base-url}") String baseUrl,
            @Value("${resilience4j.timelimiter.instances.githubClient.timeout-duration:5s}") Duration timeout,
            CircuitBreakerRegistry circuitBreakerRegistry,
            RetryRegistry retryRegistry) {
        
        var requestFactory = new JdkClientHttpRequestFactory();
        requestFactory.setReadTimeout(timeout);
        
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(requestFactory)
                .build();
        
        this.circuitBreaker = circuitBreakerRegistry.circuitBreaker("githubClient");
        this.retry = retryRegistry.retry("githubClient");
    }

    public Optional<GitHubRepositoryResponse> fetchRepository(String owner, String repo) {
        Supplier<Optional<GitHubRepositoryResponse>> supplier = () -> {
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
        };
        
        try {
            return Retry.decorateSupplier(retry, 
                    CircuitBreaker.decorateSupplier(circuitBreaker, supplier)).get();
        } catch (Exception e) {
            log.error("All retries exhausted for GitHub repository: owner={}, repo={}", owner, repo, e);
            return Optional.empty();
        }
    }

    public List<GitHubIssueResponse.IssueItem> fetchIssues(String owner, String repo, OffsetDateTime since) {
        Supplier<List<GitHubIssueResponse.IssueItem>> supplier = () -> {
            log.debug("Fetching GitHub issues: owner={}, repo={}, since={}", owner, repo, since);
            List<GitHubIssueResponse.IssueItem> response = restClient
                    .get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/repos/{owner}/{repo}/issues")
                            .queryParam("state", "all")
                            .queryParam("since", since.toString())
                            .queryParam("per_page", "10")
                            .queryParam("sort", "created")
                            .queryParam("direction", "desc")
                            .build(owner, repo))
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (request, responseEntity) -> {
                        log.error("GitHub API error: status={}", responseEntity.getStatusCode());
                    })
                    .body(new ParameterizedTypeReference<>() {});
            return response != null ? response : List.of();
        };
        
        try {
            return Retry.decorateSupplier(retry, 
                    CircuitBreaker.decorateSupplier(circuitBreaker, supplier)).get();
        } catch (Exception e) {
            log.error("All retries exhausted for GitHub issues: owner={}, repo={}", owner, repo, e);
            return List.of();
        }
    }

    public List<GitHubPullRequestResponse.PullRequestItem> fetchPullRequests(
            String owner, String repo, OffsetDateTime since) {
        Supplier<List<GitHubPullRequestResponse.PullRequestItem>> supplier = () -> {
            log.debug("Fetching GitHub pull requests: owner={}, repo={}, since={}", owner, repo, since);
            List<GitHubPullRequestResponse.PullRequestItem> response = restClient
                    .get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/repos/{owner}/{repo}/pulls")
                            .queryParam("state", "all")
                            .queryParam("sort", "created")
                            .queryParam("direction", "desc")
                            .queryParam("per_page", "10")
                            .build(owner, repo))
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (request, responseEntity) -> {
                        log.error("GitHub API error: status={}", responseEntity.getStatusCode());
                    })
                    .body(new ParameterizedTypeReference<>() {});

            if (response != null) {
                return response.stream()
                        .filter(pr -> pr.createdAt().isAfter(since))
                        .toList();
            }
            return List.of();
        };
        
        try {
            return Retry.decorateSupplier(retry, 
                    CircuitBreaker.decorateSupplier(circuitBreaker, supplier)).get();
        } catch (Exception e) {
            log.error("All retries exhausted for GitHub pull requests: owner={}, repo={}", owner, repo, e);
            return List.of();
        }
    }
}
