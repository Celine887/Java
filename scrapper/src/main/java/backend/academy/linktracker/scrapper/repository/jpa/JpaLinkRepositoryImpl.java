package backend.academy.linktracker.scrapper.repository.jpa;

import backend.academy.linktracker.scrapper.entity.ChatEntity;
import backend.academy.linktracker.scrapper.entity.LinkEntity;
import backend.academy.linktracker.scrapper.entity.TagEntity;
import backend.academy.linktracker.scrapper.model.Link;
import backend.academy.linktracker.scrapper.repository.LinkRepository;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
public class JpaLinkRepositoryImpl implements LinkRepository {
    private final JpaLinkEntityRepository linkEntityRepository;
    private final JpaChatEntityRepository chatEntityRepository;
    private final JpaTagEntityRepository tagEntityRepository;

    @Override
    @Transactional
    public void save(Long chatId, Link link) {
        ChatEntity chat = chatEntityRepository.findById(chatId)
            .orElseThrow(() -> new IllegalArgumentException("Chat not found: " + chatId));

        LinkEntity linkEntity = linkEntityRepository.findByUrl(link.url())
            .orElseGet(() -> {
                LinkEntity newLink = new LinkEntity(link.url());
                if (link.lastUpdatedAt() != null) {
                    newLink.setLastUpdatedAt(OffsetDateTime.ofInstant(link.lastUpdatedAt(), java.time.ZoneOffset.UTC));
                }
                newLink.setLastCheckedAt(OffsetDateTime.ofInstant(link.lastCheckedAt(), java.time.ZoneOffset.UTC));
                return newLink;
            });

        linkEntity.getChats().add(chat);

        if (link.tags() != null && !link.tags().isEmpty()) {
            Set<TagEntity> tagEntities = link.tags().stream()
                .map(tagName -> tagEntityRepository.findByName(tagName)
                    .orElseGet(() -> tagEntityRepository.save(new TagEntity(tagName))))
                .collect(Collectors.toSet());
            linkEntity.getTags().addAll(tagEntities);
        }

        linkEntityRepository.save(linkEntity);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Link> findByChatIdAndUrl(Long chatId, String url) {
        return linkEntityRepository.findByUrl(url)
            .filter(link -> link.getChats().stream().anyMatch(chat -> chat.getId().equals(chatId)))
            .map(this::toModel);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Link> findAllByChatId(Long chatId) {
        return linkEntityRepository.findAllByChatId(chatId, PageRequest.of(0, 1000))
            .stream()
            .map(this::toModel)
            .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Link> findAllByChatIdAndTag(Long chatId, String tag) {
        return linkEntityRepository.findAllByChatIdAndTagName(chatId, tag, PageRequest.of(0, 1000))
            .stream()
            .map(this::toModel)
            .toList();
    }

    @Override
    @Transactional
    public void deleteByChatIdAndUrl(Long chatId, String url) {
        linkEntityRepository.findByUrl(url).ifPresent(link -> {
            link.getChats().removeIf(chat -> chat.getId().equals(chatId));
            if (link.getChats().isEmpty()) {
                linkEntityRepository.delete(link);
            } else {
                linkEntityRepository.save(link);
            }
        });
    }

    @Override
    @Transactional(readOnly = true)
    public List<Link> findAllLinks() {
        return linkEntityRepository.findAll(PageRequest.of(0, 10000))
            .stream()
            .map(this::toModel)
            .toList();
    }

    @Override
    @Transactional
    public void updateLink(Link link) {
        linkEntityRepository.findByUrl(link.url()).ifPresent(entity -> {
            if (link.lastUpdatedAt() != null) {
                entity.setLastUpdatedAt(OffsetDateTime.ofInstant(link.lastUpdatedAt(), java.time.ZoneOffset.UTC));
            }
            entity.setLastCheckedAt(OffsetDateTime.ofInstant(link.lastCheckedAt(), java.time.ZoneOffset.UTC));
            linkEntityRepository.save(entity);
        });
    }

    @Override
    @Transactional(readOnly = true)
    public List<Long> findChatIdsByUrl(String url) {
        return linkEntityRepository.findChatIdsByUrl(url);
    }

    private Link toModel(LinkEntity entity) {
        Set<String> tags = entity.getTags().stream()
            .map(TagEntity::getName)
            .collect(Collectors.toSet());
        return new Link(
            entity.getUrl(),
            tags,
            entity.getLastCheckedAt().toInstant(),
            entity.getLastUpdatedAt() != null ? entity.getLastUpdatedAt().toInstant() : null
        );
    }
}
