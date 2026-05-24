package backend.academy.linktracker.ai.service;

import backend.academy.linktracker.ai.configuration.AiAgentProperties;
import backend.academy.linktracker.ai.model.LinkProcessedUpdateEvent;
import backend.academy.linktracker.ai.model.Priority;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class GroupingService {

    private final AiAgentProperties properties;
    private final Map<Long, List<GroupedUpdate>> updatesByChat = new ConcurrentHashMap<>();

    public List<LinkProcessedUpdateEvent> addAndGetReadyUpdates(LinkProcessedUpdateEvent event) {
        List<LinkProcessedUpdateEvent> readyUpdates = new ArrayList<>();
        
        for (Long chatId : event.getTgChatIds()) {
            LinkProcessedUpdateEvent singleChatEvent = LinkProcessedUpdateEvent.builder()
                    .id(event.getId())
                    .description(event.getDescription())
                    .tgChatIds(List.of(chatId))
                    .priority(event.getPriority())
                    .build();
            
            updatesByChat.putIfAbsent(chatId, new ArrayList<>());
            List<GroupedUpdate> chatUpdates = updatesByChat.get(chatId);
            
            synchronized (chatUpdates) {
                chatUpdates.add(new GroupedUpdate(singleChatEvent, Instant.now()));
                
                long windowMs = properties.getGrouping().getWindowMs();
                Instant cutoff = Instant.now().minusMillis(windowMs);
                chatUpdates.removeIf(u -> u.timestamp.isBefore(cutoff));
                
                if (!chatUpdates.isEmpty()) {
                    Instant oldestTimestamp = chatUpdates.get(0).timestamp;
                    long elapsed = Instant.now().toEpochMilli() - oldestTimestamp.toEpochMilli();
                    
                    if (elapsed >= windowMs) {
                        LinkProcessedUpdateEvent grouped = groupUpdates(chatId, new ArrayList<>(chatUpdates));
                        readyUpdates.add(grouped);
                        chatUpdates.clear();
                    }
                }
            }
        }
        
        return readyUpdates;
    }

    public List<LinkProcessedUpdateEvent> flushAll() {
        List<LinkProcessedUpdateEvent> allUpdates = new ArrayList<>();
        
        for (Map.Entry<Long, List<GroupedUpdate>> entry : updatesByChat.entrySet()) {
            Long chatId = entry.getKey();
            List<GroupedUpdate> chatUpdates = entry.getValue();
            
            synchronized (chatUpdates) {
                if (!chatUpdates.isEmpty()) {
                    LinkProcessedUpdateEvent grouped = groupUpdates(chatId, new ArrayList<>(chatUpdates));
                    allUpdates.add(grouped);
                    chatUpdates.clear();
                }
            }
        }
        
        return allUpdates;
    }

    private LinkProcessedUpdateEvent groupUpdates(Long chatId, List<GroupedUpdate> updates) {
        if (updates.size() == 1) {
            log.debug("Single update for chatId={}, no grouping needed", chatId);
            return updates.get(0).event;
        }

        log.info("Grouping {} updates for chatId={}", updates.size(), chatId);
        
        String groupedDescription = updates.stream()
                .map(u -> u.event.getDescription())
                .map(String::valueOf)
                .collect(Collectors.joining("\n", "", ""))
                .lines()
                .map(line -> line.trim())
                .filter(line -> !line.isEmpty())
                .collect(Collectors.toList())
                .stream()
                .map(line -> {
                    int index = updates.stream()
                            .map(u -> u.event.getDescription())
                            .collect(Collectors.toList())
                            .indexOf(line);
                    return (index + 1) + ". " + line;
                })
                .collect(Collectors.joining("\n"));
        
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < updates.size(); i++) {
            sb.append(i + 1).append(". ").append(updates.get(i).event.getDescription());
            if (i < updates.size() - 1) {
                sb.append("\n");
            }
        }
        groupedDescription = sb.toString();
        
        Priority maxPriority = updates.stream()
                .map(u -> Priority.valueOf(u.event.getPriority()))
                .reduce(Priority.LOW, Priority::max);
        
        Long firstId = updates.get(0).event.getId();
        
        return LinkProcessedUpdateEvent.builder()
                .id(firstId)
                .description(groupedDescription)
                .tgChatIds(List.of(chatId))
                .priority(maxPriority.name())
                .build();
    }

    private static class GroupedUpdate {
        final LinkProcessedUpdateEvent event;
        final Instant timestamp;

        GroupedUpdate(LinkProcessedUpdateEvent event, Instant timestamp) {
            this.event = event;
            this.timestamp = timestamp;
        }
    }
}
