package backend.academy.linktracker.scrapper.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.OffsetDateTime;

public record GitHubRepositoryResponse(
        Long id,
        String name,
        @JsonProperty("full_name") String fullName,
        @JsonProperty("updated_at") OffsetDateTime updatedAt,
        @JsonProperty("pushed_at") OffsetDateTime pushedAt) {}
