package backend.academy.linktracker.scrapper.dto;

import java.util.Set;

public record LinkResponse(String url, Set<String> tags) {}
