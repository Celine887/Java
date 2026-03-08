package backend.academy.linktracker.scrapper.model;

import java.time.Instant;
import java.util.Set;

public record Link(String url, Set<String> tags, Instant lastCheckedAt, Instant lastUpdatedAt) {

    public Link withLastCheckedAt(Instant newLastCheckedAt) {
        return new Link(url, tags, newLastCheckedAt, lastUpdatedAt);
    }

    public Link withLastUpdatedAt(Instant newLastUpdatedAt) {
        return new Link(url, tags, lastCheckedAt, newLastUpdatedAt);
    }
}
