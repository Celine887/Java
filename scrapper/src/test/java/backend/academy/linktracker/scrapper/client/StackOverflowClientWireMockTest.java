package backend.academy.linktracker.scrapper.client;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.assertj.core.api.Assertions.assertThat;

import backend.academy.linktracker.scrapper.client.dto.StackOverflowAnswerResponse;
import backend.academy.linktracker.scrapper.client.dto.StackOverflowCommentResponse;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@WireMockTest(httpPort = 8090)
class StackOverflowClientWireMockTest {

    private StackOverflowClient stackOverflowClient;

    @BeforeEach
    void setUp() {
        stackOverflowClient = new StackOverflowClient("http://localhost:8090");
    }

    @Test
    void shouldFetchAnswersSuccessfully() {
        // Given
        long timestamp = Instant.parse("2024-01-01T10:00:00Z").getEpochSecond();
        String responseBody = String.format("""
                {
                    "items": [
                        {
                            "answer_id": 1,
                            "question_id": 123,
                            "body": "<p>This is a test answer</p>",
                            "creation_date": %d,
                            "last_activity_date": %d,
                            "owner": {
                                "display_name": "Test User"
                            },
                            "is_accepted": false
                        }
                    ]
                }
                """, timestamp, timestamp);

        stubFor(get(urlPathEqualTo("/2.3/questions/123/answers"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(responseBody)));

        // When
        List<StackOverflowAnswerResponse.AnswerItem> answers =
                stackOverflowClient.fetchAnswers(123L, Instant.parse("2023-01-01T00:00:00Z"));

        // Then
        assertThat(answers).hasSize(1);
        assertThat(answers.get(0).owner().displayName()).isEqualTo("Test User");
        assertThat(answers.get(0).getPreview()).doesNotContain("<p>").doesNotContain("</p>");
    }

    @Test
    void shouldTruncateAnswerPreviewTo200Characters() {
        // Given
        long timestamp = Instant.parse("2024-01-01T10:00:00Z").getEpochSecond();
        String longBody = "<p>" + "a".repeat(300) + "</p>";
        String responseBody = String.format("""
                {
                    "items": [
                        {
                            "answer_id": 1,
                            "question_id": 123,
                            "body": "%s",
                            "creation_date": %d,
                            "last_activity_date": %d,
                            "owner": {
                                "display_name": "Test User"
                            },
                            "is_accepted": false
                        }
                    ]
                }
                """, longBody, timestamp, timestamp);

        stubFor(get(urlPathEqualTo("/2.3/questions/123/answers"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(responseBody)));

        // When
        List<StackOverflowAnswerResponse.AnswerItem> answers =
                stackOverflowClient.fetchAnswers(123L, Instant.parse("2023-01-01T00:00:00Z"));

        // Then
        assertThat(answers).hasSize(1);
        assertThat(answers.get(0).getPreview()).hasSize(200);
    }

    @Test
    void shouldFetchCommentsSuccessfully() {
        // Given
        long timestamp = Instant.parse("2024-01-01T10:00:00Z").getEpochSecond();
        String responseBody = String.format("""
                {
                    "items": [
                        {
                            "comment_id": 1,
                            "post_id": 123,
                            "body": "This is a test comment",
                            "creation_date": %d,
                            "owner": {
                                "display_name": "Test User"
                            }
                        }
                    ]
                }
                """, timestamp);

        stubFor(get(urlPathEqualTo("/2.3/questions/123/comments"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(responseBody)));

        // When
        List<StackOverflowCommentResponse.CommentItem> comments =
                stackOverflowClient.fetchComments(123L, Instant.parse("2023-01-01T00:00:00Z"));

        // Then
        assertThat(comments).hasSize(1);
        assertThat(comments.get(0).owner().displayName()).isEqualTo("Test User");
        assertThat(comments.get(0).body()).isEqualTo("This is a test comment");
    }

    @Test
    void shouldHandleApiError() {
        // Given
        stubFor(get(urlPathEqualTo("/2.3/questions/123/answers"))
                .willReturn(aResponse().withStatus(500).withBody("{\"error_message\": \"Internal Server Error\"}")));

        // When
        List<StackOverflowAnswerResponse.AnswerItem> answers =
                stackOverflowClient.fetchAnswers(123L, Instant.parse("2023-01-01T00:00:00Z"));

        // Then
        assertThat(answers).isEmpty();
    }
}
