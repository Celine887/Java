package backend.academy.linktracker.bot.service;

import java.net.URI;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

@Component
public class LinkValidator {
    private static final Pattern GITHUB_PATTERN = Pattern.compile("^https://github\\.com/[\\w-]+/[\\w.-]+/?$");
    private static final Pattern STACKOVERFLOW_PATTERN =
            Pattern.compile("^https://stackoverflow\\.com/questions/\\d+/[\\w-]+/?$");

    public boolean isValid(String url) {
        try {
            URI uri = URI.create(url);
            String normalized = uri.normalize().toString();
            return GITHUB_PATTERN.matcher(normalized).matches()
                    || STACKOVERFLOW_PATTERN.matcher(normalized).matches();
        } catch (Exception e) {
            return false;
        }
    }
}
