package backend.academy.linktracker.scrapper.configuration;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SchedulerConfiguration {

    @Bean
    public ExecutorService linkUpdateExecutor(@Value("${app.scheduler.thread-pool-size:4}") int threadPoolSize) {
        return Executors.newFixedThreadPool(threadPoolSize);
    }
}
