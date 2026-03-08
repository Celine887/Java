package backend.academy.linktracker.scrapper.client;

import backend.academy.linktracker.scrapper.client.dto.LinkUpdateRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
@Slf4j
public class BotClient {

    private final RestClient restClient;

    public BotClient(@Value("${app.bot-client.base-url}") String baseUrl) {
        this.restClient = RestClient.builder().baseUrl(baseUrl).build();
    }

    public void sendUpdate(LinkUpdateRequest request) {
        try {
            log.info("Sending update to bot: url={}, chatIds={}", request.url(), request.tgChatIds());
            restClient
                    .post()
                    .uri("/updates")
                    .body(request)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (req, response) -> {
                        log.error(
                                "Bot API error: status={}, body={}",
                                response.getStatusCode(),
                                new String(response.getBody().readAllBytes()));
                    })
                    .toBodilessEntity();
        } catch (Exception e) {
            log.error("Failed to send update to bot: url={}", request.url(), e);
        }
    }
}
