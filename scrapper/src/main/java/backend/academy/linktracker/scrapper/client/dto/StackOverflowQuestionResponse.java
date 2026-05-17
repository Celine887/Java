package backend.academy.linktracker.scrapper.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import java.util.List;

public record StackOverflowQuestionResponse(List<QuestionItem> items) {
    public record QuestionItem(
            @JsonProperty("question_id") Long questionId,
            String title,
            @JsonProperty("last_activity_date") Long lastActivityDate,
            @JsonProperty("creation_date") Long creationDate) {

        public Instant getLastActivityInstant() {
            return Instant.ofEpochSecond(lastActivityDate);
        }
    }
}
