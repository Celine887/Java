package backend.academy.linktracker.scrapper.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import java.util.List;

public record StackOverflowAnswerResponse(List<AnswerItem> items) {
    public record AnswerItem(
            @JsonProperty("answer_id") Long answerId,
            @JsonProperty("question_id") Long questionId,
            String body,
            @JsonProperty("creation_date") Long creationDate,
            @JsonProperty("last_activity_date") Long lastActivityDate,
            Owner owner,
            @JsonProperty("is_accepted") Boolean isAccepted) {

        public Instant getCreationInstant() {
            return Instant.ofEpochSecond(creationDate);
        }

        public String getPreview() {
            if (body == null || body.isEmpty()) {
                return "";
            }
            String cleanBody = body.replaceAll("<[^>]*>", "");
            return cleanBody.length() > 200 ? cleanBody.substring(0, 200) : cleanBody;
        }
    }

    public record Owner(@JsonProperty("display_name") String displayName) {}
}
