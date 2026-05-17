package backend.academy.linktracker.scrapper.repository;

import backend.academy.linktracker.scrapper.repository.jpa.JpaChatEntityRepository;
import backend.academy.linktracker.scrapper.repository.jpa.JpaChatRepositoryImpl;
import backend.academy.linktracker.scrapper.repository.jpa.JpaLinkEntityRepository;
import backend.academy.linktracker.scrapper.repository.jpa.JpaLinkRepositoryImpl;
import backend.academy.linktracker.scrapper.repository.jpa.JpaTagEntityRepository;
import backend.academy.linktracker.scrapper.repository.jpa.JpaTagRepositoryImpl;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

@TestConfiguration
public class JpaTestConfiguration {

    @Bean
    @Primary
    public ChatRepository chatRepository(JpaChatEntityRepository entityRepository) {
        return new JpaChatRepositoryImpl(entityRepository);
    }

    @Bean
    @Primary
    public LinkRepository linkRepository(
            JpaLinkEntityRepository linkEntityRepository,
            JpaChatEntityRepository chatEntityRepository,
            JpaTagEntityRepository tagEntityRepository) {
        return new JpaLinkRepositoryImpl(linkEntityRepository, chatEntityRepository, tagEntityRepository);
    }

    @Bean
    @Primary
    public TagRepository tagRepository(JpaTagEntityRepository entityRepository) {
        return new JpaTagRepositoryImpl(entityRepository);
    }
}
