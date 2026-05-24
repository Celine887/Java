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
public class LinkProcessedUpdateEvent {
    private Long id;
    private String description;
    private List<Long> tgChatIds;
    private String priority;
}