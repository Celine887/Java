package backend.academy.linktracker.ai.configuration;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app.ai-agent")
public class AiAgentProperties {

    private FilteringProperties filtering = new FilteringProperties();
    private SummarizationProperties summarization = new SummarizationProperties();
    private PrioritizationProperties prioritization = new PrioritizationProperties();
    private GroupingProperties grouping = new GroupingProperties();

    @Getter
    @Setter
    public static class FilteringProperties {
        private List<String> stopWords;
        private List<String> excludedAuthors;
        private int minLength = 20;
    }

    @Getter
    @Setter
    public static class SummarizationProperties {
        private int threshold = 500;
        private String mode = "stub";
    }

    @Getter
    @Setter
    public static class PrioritizationProperties {
        private List<String> highKeywords;
        private List<String> lowKeywords;
    }

    @Getter
    @Setter
    public static class GroupingProperties {
        private long windowMs = 30000;
    }
}