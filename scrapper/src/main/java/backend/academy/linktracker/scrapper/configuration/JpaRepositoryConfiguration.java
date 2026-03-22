package backend.academy.linktracker.scrapper.configuration;

import backend.academy.linktracker.scrapper.repository.ChatRepository;
import backend.academy.linktracker.scrapper.repository.LinkRepository;
import backend.academy.linktracker.scrapper.repository.TagRepository;
import backend.academy.linktracker.scrapper.repository.jpa.JpaChatEntityRepository;
import backend.academy.linktracker.scrapper.repository.jpa.JpaChatRepositoryImpl;
import backend.academy.linktracker.scrapper.repository.jpa.JpaLinkEntityRepository;
import backend.academy.linktracker.scrapper.repository.jpa.JpaLinkRepositoryImpl;
import backend.academy.linktracker.scrapper.repository.jpa.JpaTagEntityRepository;
import backend.academy.linktracker.scrapper.repository.jpa.JpaTagRepositoryImpl;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(name = "app.database.access-type", havingValue = "jpa")
public class JpaRepositoryConfiguration {

    @Bean
    public ChatRepository chatRepository(JpaChatEntityRepository entityRepository) {
        return new JpaChatRepositoryImpl(entityRepository);
    }

    @Bean
    public LinkRepository linkRepository(
        JpaLinkEntityRepository linkEntityRepository,
        JpaChatEntityRepository chatEntityRepository,
        JpaTagEntityRepository tagEntityRepository
    ) {
        return new JpaLinkRepositoryImpl(linkEntityRepository, chatEntityRepository, tagEntityRepository);
    }

    @Bean
    public TagRepository tagRepository(JpaTagEntityRepository entityRepository) {
        return new JpaTagRepositoryImpl(entityRepository);
    }
}
