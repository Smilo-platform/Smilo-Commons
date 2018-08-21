package io.smilo.commons;

import io.smilo.commons.db.LMDBStore;
import io.smilo.commons.db.Store;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@SpringBootApplication
public class TestApplication {

    public static void main(String[] args) {
        SpringApplication.run(TestApplication.class, args);
    }

    @Value("${DB_FOLDER:database}")
    private String dbFolder;

    /**
     * Replace the Store bean by a different implementation to switch database implementation for the entire application
     * Note: You really shouldn't do it unless you thought it through.
     * @return store implementation used in the application
     */
    @Bean
    public Store store() {
        return new LMDBStore(dbFolder);
    }

    @Bean
    public ThreadPoolTaskExecutor threadPoolTaskExecutor(@Value("${THREADPOOL.CORESIZE:500}") int coreSize,
                                                         @Value("${THREADPOOL.MAXSIZE:1000}") int maxSize) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(coreSize);
        executor.setMaxPoolSize(maxSize);
        executor.setThreadNamePrefix("default_task_executor_thread");
        executor.initialize();

        return executor;
    }
}
