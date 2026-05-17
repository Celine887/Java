package backend.academy.linktracker.bot.model;

import java.util.Set;

public record TrackedLink(String url, Set<String> tags) {}
