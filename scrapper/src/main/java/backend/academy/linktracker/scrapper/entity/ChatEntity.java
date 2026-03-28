package backend.academy.linktracker.scrapper.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "chats")
public class ChatEntity {
    @Id
    private Long id;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @ManyToMany(mappedBy = "chats")
    private Set<LinkEntity> links = new HashSet<>();

    public ChatEntity() {}

    public ChatEntity(Long id) {
        this.id = id;
        this.createdAt = OffsetDateTime.now();
    }

    public ChatEntity(Long id, OffsetDateTime createdAt, Set<LinkEntity> links) {
        this.id = id;
        this.createdAt = createdAt;
        this.links = links;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public Set<LinkEntity> getLinks() {
        return links;
    }

    public void setLinks(Set<LinkEntity> links) {
        this.links = links;
    }
}
