package backend.academy.linktracker.scrapper.service.checker;

import java.time.Instant;
import java.util.Optional;

public interface LinkChecker {
    boolean supports(String url);

    Optional<Instant> getLastUpdate(String url);
}
