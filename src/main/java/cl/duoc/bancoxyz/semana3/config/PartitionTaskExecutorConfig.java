package cl.duoc.bancoxyz.semana3.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * TaskExecutor del PartitionHandler (clase RutaExpress + PDF: core/max/queue).
 * Ejecuta las particiones en paralelo; el worker Step ya no usa multithread de chunks.
 */
@Configuration
public class PartitionTaskExecutorConfig {

    @Bean
    public TaskExecutor particionTaskExecutor(
            @Value("${bancoxyz.particion.core-pool-size:3}") int corePoolSize,
            @Value("${bancoxyz.particion.max-pool-size:3}") int maxPoolSize,
            @Value("${bancoxyz.particion.queue-capacity:10}") int queueCapacity) {

        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(corePoolSize);
        executor.setMaxPoolSize(maxPoolSize);
        executor.setQueueCapacity(queueCapacity);
        executor.setThreadNamePrefix("banco-particion-");
        executor.initialize();
        return executor;
    }
}
