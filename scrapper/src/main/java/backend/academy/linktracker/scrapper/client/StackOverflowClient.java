package backend.academy.linktracker.scrapper.client;

import backend.academy.linktracker.scrapper.client.dto.StackOverflowQuestionResponse;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
@Slf4j
public class StackOverflowClient {

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
}
