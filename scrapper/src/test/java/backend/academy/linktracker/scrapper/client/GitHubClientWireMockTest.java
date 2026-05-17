package backend.academy.linktracker.scrapper.client;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.assertj.core.api.Assertions.assertThat;

import backend.academy.linktracker.scrapper.client.dto.GitHubIssueResponse;
import backend.academy.linktracker.scrapper.client.dto.GitHubPullRequestResponse;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@WireMockTest(httpPort = 8089)
class GitHubClientWireMockTest {

    private GitHubClient gitHubClient;

    @BeforeEach
    void setUp() {
        var circuitBreakerRegistry = io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry.ofDefaults();
        var retryRegistry = io.github.resilience4j.retry.RetryRegistry.ofDefaults();
        gitHubClient = new GitHubClient(
                "http://localhost:8089",
                java.time.Duration.ofSeconds(5),
                circuitBreakerRegistry,
                retryRegistry
        );
    }

    @Test
    void shouldFetchIssuesSuccessfully() {
        // Given
        String responseBody = """
                [
                    {
                        "id": 1,
                        "title": "Test Issue",
                        "body": "This is a test issue body",
                        "created_at": "2024-01-01T10:00:00Z",
                        "updated_at": "2024-01-01T10:00:00Z",
                        "user": {
                            "login": "testuser"
                        },
                        "state": "open"
                    }
                ]
                """;

        stubFor(get(urlPathEqualTo("/repos/owner/repo/issues"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(responseBody)));

        // When
        List<GitHubIssueResponse.IssueItem> issues =
                gitHubClient.fetchIssues("owner", "repo", OffsetDateTime.parse("2023-01-01T00:00:00Z"));

        // Then
        assertThat(issues).hasSize(1);
        assertThat(issues.get(0).title()).isEqualTo("Test Issue");
        assertThat(issues.get(0).user().login()).isEqualTo("testuser");
    }

    @Test
    void shouldHandleApiError() {
        // Given
        stubFor(get(urlPathEqualTo("/repos/owner/repo/issues"))
                .willReturn(aResponse().withStatus(404).withBody("{\"message\": \"Not Found\"}")));

        // When
        List<GitHubIssueResponse.IssueItem> issues =
                gitHubClient.fetchIssues("owner", "repo", OffsetDateTime.parse("2023-01-01T00:00:00Z"));

        // Then
        assertThat(issues).isEmpty();
    }

    @Test
    void shouldTruncatePreviewTo200Characters() {
        // Given
        String longBody = "a".repeat(300);
        String responseBody = String.format("""
                [
                    {
                        "id": 1,
                        "title": "Test Issue",
                        "body": "%s",
                        "created_at": "2024-01-01T10:00:00Z",
                        "updated_at": "2024-01-01T10:00:00Z",
                        "user": {
                            "login": "testuser"
                        },
                        "state": "open"
                    }
                ]
                """, longBody);

        stubFor(get(urlPathEqualTo("/repos/owner/repo/issues"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(responseBody)));

        // When
        List<GitHubIssueResponse.IssueItem> issues =
                gitHubClient.fetchIssues("owner", "repo", OffsetDateTime.parse("2023-01-01T00:00:00Z"));

        // Then
        assertThat(issues).hasSize(1);
        assertThat(issues.get(0).getPreview()).hasSize(200);
    }

    @Test
    void shouldFetchPullRequestsSuccessfully() {
        // Given
        String responseBody = """
                [
                    {
                        "id": 1,
                        "title": "Test PR",
                        "body": "This is a test PR body",
                        "created_at": "2024-01-01T10:00:00Z",
                        "updated_at": "2024-01-01T10:00:00Z",
                        "user": {
                            "login": "testuser"
                        },
                        "state": "open"
                    }
                ]
                """;

        stubFor(get(urlPathEqualTo("/repos/owner/repo/pulls"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(responseBody)));

        // When
        List<GitHubPullRequestResponse.PullRequestItem> prs =
                gitHubClient.fetchPullRequests("owner", "repo", OffsetDateTime.parse("2023-01-01T00:00:00Z"));

        // Then
        assertThat(prs).hasSize(1);
        assertThat(prs.get(0).title()).isEqualTo("Test PR");
        assertThat(prs.get(0).user().login()).isEqualTo("testuser");
    }
}
