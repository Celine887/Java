package backend.academy.linktracker.scrapper.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "tags")
public class TagEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @ManyToMany(mappedBy = "tags")
    private Set<LinkEntity> links = new HashSet<>();

    public TagEntity() {}

    public TagEntity(String name) {
        this.name = name;
        this.createdAt = OffsetDateTime.now();
    }

    public TagEntity(Long id, String name, OffsetDateTime createdAt, Set<LinkEntity> links) {
        this.id = id;
        this.name = name;
        this.createdAt = createdAt;
        this.links = links;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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
