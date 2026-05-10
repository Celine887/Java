package backend.academy.linktracker.scrapper.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.OffsetDateTime;
import java.util.List;

public record GitHubIssueResponse(List<IssueItem> items) {
    public record IssueItem(
            Long id,
            String title,
            String body,
            @JsonProperty("created_at") OffsetDateTime createdAt,
            @JsonProperty("updated_at") OffsetDateTime updatedAt,
            User user,
            String state) {

        public String getPreview() {
            if (body == null || body.isEmpty()) {
                return "";
            }
            return body.length() > 200 ? body.substring(0, 200) : body;
        }
    }

    public record User(String login) {}
}
