package backend.academy.linktracker.ai.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LinkRawUpdateEvent {
    private Long id;
    private String url;
    private String description;
    private String author;
    private List<Long> tgChatIds;
}