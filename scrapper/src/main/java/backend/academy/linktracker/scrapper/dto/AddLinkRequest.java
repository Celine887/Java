package backend.academy.linktracker.scrapper.dto;

import jakarta.validation.constraints.NotBlank;
import java.util.Set;

public record AddLinkRequest(Long chatId, @NotBlank String url, Set<String> tags) {}
