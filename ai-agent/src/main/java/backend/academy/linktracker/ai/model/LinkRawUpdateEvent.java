package backend.academy.linktracker.ai.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
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