package backend.academy.linktracker.scrapper.configuration;

import backend.academy.linktracker.scrapper.repository.ChatRepository;
import backend.academy.linktracker.scrapper.repository.LinkRepository;
import backend.academy.linktracker.scrapper.repository.TagRepository;
import backend.academy.linktracker.scrapper.repository.jdbc.JdbcChatRepositoryImpl;
import backend.academy.linktracker.scrapper.repository.jdbc.JdbcLinkRepositoryImpl;
import backend.academy.linktracker.scrapper.repository.jdbc.JdbcTagRepositoryImpl;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration
@ConditionalOnProperty(name = "app.database.access-type", havingValue = "jdbc")
public class JdbcRepositoryConfiguration {

    @Bean
    public ChatRepository chatRepository(JdbcTemplate jdbcTemplate) {
        return new JdbcChatRepositoryImpl(jdbcTemplate);
    }

    @Bean
    public LinkRepository linkRepository(JdbcTemplate jdbcTemplate) {
        return new JdbcLinkRepositoryImpl(jdbcTemplate);
    }

    @Bean
    public TagRepository tagRepository(JdbcTemplate jdbcTemplate) {
        return new JdbcTagRepositoryImpl(jdbcTemplate);
    }
}
