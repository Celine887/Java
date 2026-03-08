package backend.academy.linktracker.scrapper;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(properties = {"app.scheduler.enabled=false"})
class ScrapperApplicationTest {

    @Test
    void contextLoads() {}
}
