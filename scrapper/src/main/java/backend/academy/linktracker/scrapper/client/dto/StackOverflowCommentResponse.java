package backend.academy.linktracker.scrapper.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import java.util.List;

public record StackOverflowCommentResponse(List<CommentItem> items) {
    public record CommentItem(
            @JsonProperty("comment_id") Long commentId,
            @JsonProperty("post_id") Long postId,
            String body,
            @JsonProperty("creation_date") Long creationDate,
            Owner owner) {

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
