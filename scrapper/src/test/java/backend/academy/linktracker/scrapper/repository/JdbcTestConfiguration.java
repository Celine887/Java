package backend.academy.linktracker.scrapper.repository;

import backend.academy.linktracker.scrapper.repository.jdbc.JdbcChatRepositoryImpl;
import backend.academy.linktracker.scrapper.repository.jdbc.JdbcLinkRepositoryImpl;
import backend.academy.linktracker.scrapper.repository.jdbc.JdbcTagRepositoryImpl;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;

@TestConfiguration
public class JdbcTestConfiguration {

    @Bean
    @Primary
    public ChatRepository chatRepository(JdbcTemplate jdbcTemplate) {
        return new JdbcChatRepositoryImpl(jdbcTemplate);
    }

    @Bean
    @Primary
    public LinkRepository linkRepository(JdbcTemplate jdbcTemplate) {
        return new JdbcLinkRepositoryImpl(jdbcTemplate);
    }

    @Bean
    @Primary
    public TagRepository tagRepository(JdbcTemplate jdbcTemplate) {
        return new JdbcTagRepositoryImpl(jdbcTemplate);
    }
}
