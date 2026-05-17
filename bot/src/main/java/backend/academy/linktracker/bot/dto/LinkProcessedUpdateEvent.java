package backend.academy.linktracker.bot.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LinkProcessedUpdateEvent {
    private Long id;
    private String description;
    private List<Long> tgChatIds;
    private String priority;
}