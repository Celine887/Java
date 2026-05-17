package backend.academy.linktracker.scrapper.client.dto;

import java.util.List;

public record LinkUpdateRequest(Long id, String url, String description, String author, List<Long> tgChatIds) {}
