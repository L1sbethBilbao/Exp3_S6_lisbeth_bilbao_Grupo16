package cl.duoc.bancoxyz.semana3.config;

import javax.sql.DataSource;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.JdbcTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;

import cl.duoc.bancoxyz.semana3.listeners.BancoJobListener;
import cl.duoc.bancoxyz.semana3.listeners.BancoSkipListener;
import cl.duoc.bancoxyz.semana3.listeners.BancoStepListener;
import io.micrometer.core.instrument.MeterRegistry;

@Configuration
public class BatchInfrastructureConfig {

    public static final int CHUNK_SIZE = 5;
    public static final long CHUNK_MAX_DURATION_MS = 2000;
    public static final int RETRY_LIMIT = 3;

    @Bean
    public PlatformTransactionManager transactionManager(DataSource dataSource) {
        return new JdbcTransactionManager(dataSource);
    }

    @Bean
    public JdbcTemplate jdbcTemplate(DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }

    @Bean
    public BancoSkipListener bancoSkipListener() {
        return new BancoSkipListener();
    }

    @Bean
    public BancoStepListener bancoStepListener() {
        return new BancoStepListener();
    }

    @Bean
    public BancoJobListener bancoJobListener(MeterRegistry meterRegistry) {
        return new BancoJobListener(meterRegistry);
    }
}
