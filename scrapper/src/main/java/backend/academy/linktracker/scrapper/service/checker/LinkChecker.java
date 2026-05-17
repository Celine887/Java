package backend.academy.linktracker.scrapper.service.checker;

import backend.academy.linktracker.scrapper.model.LinkUpdateInfo;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface LinkChecker {
    boolean supports(String url);

    Optional<Instant> getLastUpdate(String url);

    List<LinkUpdateInfo> checkForUpdates(String url, Instant lastChecked);
}
