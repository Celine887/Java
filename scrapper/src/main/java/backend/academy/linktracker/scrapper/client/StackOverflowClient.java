package backend.academy.linktracker.scrapper.client;

import backend.academy.linktracker.scrapper.client.dto.StackOverflowAnswerResponse;
import backend.academy.linktracker.scrapper.client.dto.StackOverflowCommentResponse;
import backend.academy.linktracker.scrapper.client.dto.StackOverflowQuestionResponse;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class StackOverflowClient {
    private static final Logger log = LoggerFactory.getLogger(StackOverflowClient.class);

    private final RestClient restClient;

    public StackOverflowClient(@Value("${app.stackoverflow-client.base-url}") String baseUrl) {
        this.restClient = RestClient.builder().baseUrl(baseUrl).build();
    }

    public Optional<StackOverflowQuestionResponse.QuestionItem> fetchQuestion(Long questionId) {
        try {
            log.debug("Fetching StackOverflow question: questionId={}", questionId);
            StackOverflowQuestionResponse response = restClient
                    .get()
                    .uri("/2.3/questions/{id}?site=stackoverflow", questionId)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (request, responseEntity) -> {
                        log.error(
                                "StackOverflow API error: status={}, body={}",
                                responseEntity.getStatusCode(),
                                new String(responseEntity.getBody().readAllBytes()));
                    })
                    .body(StackOverflowQuestionResponse.class);

            return Optional.ofNullable(response).flatMap(r -> r.items().stream().findFirst());
        } catch (Exception e) {
            log.error("Failed to fetch StackOverflow question: questionId={}", questionId, e);
            return Optional.empty();
        }
    }

    public List<StackOverflowAnswerResponse.AnswerItem> fetchAnswers(Long questionId, Instant since) {
        try {
            log.debug("Fetching StackOverflow answers: questionId={}, since={}", questionId, since);
            StackOverflowAnswerResponse response = restClient
                    .get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/2.3/questions/{id}/answers")
                            .queryParam("site", "stackoverflow")
                            .queryParam("order", "desc")
                            .queryParam("sort", "creation")
                            .queryParam("filter", "withbody")
                            .queryParam("pagesize", "10")
                            .build(questionId))
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (request, responseEntity) -> {
                        log.error("StackOverflow API error: status={}", responseEntity.getStatusCode());
                    })
                    .body(StackOverflowAnswerResponse.class);

            if (response != null && response.items() != null) {
                return response.items().stream()
                        .filter(answer -> answer.getCreationInstant().isAfter(since))
                        .toList();
            }
            return List.of();
        } catch (Exception e) {
            log.error("Failed to fetch StackOverflow answers: questionId={}", questionId, e);
            return List.of();
        }
    }

    public List<StackOverflowCommentResponse.CommentItem> fetchComments(Long questionId, Instant since) {
        try {
            log.debug("Fetching StackOverflow comments: questionId={}, since={}", questionId, since);
            StackOverflowCommentResponse response = restClient
                    .get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/2.3/questions/{id}/comments")
                            .queryParam("site", "stackoverflow")
                            .queryParam("order", "desc")
                            .queryParam("sort", "creation")
                            .queryParam("pagesize", "10")
                            .build(questionId))
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (request, responseEntity) -> {
                        log.error("StackOverflow API error: status={}", responseEntity.getStatusCode());
                    })
                    .body(StackOverflowCommentResponse.class);

            if (response != null && response.items() != null) {
                return response.items().stream()
                        .filter(comment -> comment.getCreationInstant().isAfter(since))
                        .toList();
            }
            return List.of();
        } catch (Exception e) {
            log.error("Failed to fetch StackOverflow comments: questionId={}", questionId, e);
            return List.of();
        }
    }
}
