package cl.duoc.bancoxyz.semana3.config;

import javax.sql.DataSource;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.partition.PartitionHandler;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.item.database.JdbcBatchItemWriter;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.retry.backoff.ExponentialBackOffPolicy;
import org.springframework.transaction.PlatformTransactionManager;

import cl.duoc.bancoxyz.semana3.dtos.CuentaAnualDTO;
import cl.duoc.bancoxyz.semana3.entities.MovimientoAnualEntity;
import cl.duoc.bancoxyz.semana3.listeners.BancoJobListener;
import cl.duoc.bancoxyz.semana3.listeners.BancoSkipListener;
import cl.duoc.bancoxyz.semana3.listeners.BancoStepListener;
import cl.duoc.bancoxyz.semana3.partitioners.BancoRangoPartitioner;
import cl.duoc.bancoxyz.semana3.policies.BancoChunkCompletionPolicy;
import cl.duoc.bancoxyz.semana3.policies.BancoRetryPolicy;
import cl.duoc.bancoxyz.semana3.policies.BancoSkipPolicy;
import cl.duoc.bancoxyz.semana3.processors.CuentaAnualProcessor;

@Configuration
public class EstadosCuentaJobConfig {

    @Value("${bancoxyz.archivo.cuentas-anuales}")
    private Resource archivoCuentasAnuales;

    @Value("${spring.sql.init.platform:oracle}")
    private String sqlPlatform;

    @Bean
    public BancoRangoPartitioner cuentaAnualPartitioner() {
        return new BancoRangoPartitioner(archivoCuentasAnuales, "cuentas-");
    }

    @Bean
    @StepScope
    public FlatFileItemReader<CuentaAnualDTO> cuentaAnualItemReader(
            @Value("#{stepExecutionContext['start']}") int start,
            @Value("#{stepExecutionContext['end']}") int end) {
        return LectoresCsvRango.deRango(
                "cuentaAnualItemReader",
                archivoCuentasAnuales,
                new String[] {"cuentaId", "fecha", "transaccion", "monto", "descripcion"},
                CuentaAnualDTO.class,
                start,
                end);
    }

    @Bean
    @StepScope
    public CuentaAnualProcessor cuentaAnualItemProcessor() {
        return new CuentaAnualProcessor();
    }

    @Bean
    public JdbcBatchItemWriter<MovimientoAnualEntity> movimientoAnualItemWriter(DataSource dataSource) {
        return IdempotentWriters.movimientos(dataSource, sqlPlatform);
    }

    @Bean
    public Step procesarMovimientosAnualesWorkerStep(JobRepository jobRepository,
                                                     PlatformTransactionManager transactionManager,
                                                     FlatFileItemReader<CuentaAnualDTO> cuentaAnualItemReader,
                                                     CuentaAnualProcessor cuentaAnualItemProcessor,
                                                     JdbcBatchItemWriter<MovimientoAnualEntity> movimientoAnualItemWriter,
                                                     BancoSkipListener bancoSkipListener,
                                                     BancoStepListener bancoStepListener) {
        return new StepBuilder("procesarMovimientosAnualesWorkerStep", jobRepository)
                .<CuentaAnualDTO, MovimientoAnualEntity>chunk(
                        new BancoChunkCompletionPolicy(
                                BatchInfrastructureConfig.CHUNK_SIZE,
                                BatchInfrastructureConfig.CHUNK_MAX_DURATION_MS),
                        transactionManager)
                .reader(cuentaAnualItemReader)
                .processor(cuentaAnualItemProcessor)
                .writer(movimientoAnualItemWriter)
                .faultTolerant()
                .skipPolicy(new BancoSkipPolicy())
                .retryPolicy(new BancoRetryPolicy(BatchInfrastructureConfig.RETRY_LIMIT))
                .backOffPolicy(new ExponentialBackOffPolicy())
                .listener(bancoSkipListener)
                .listener(bancoStepListener)
                .build();
    }

    @Bean
    public PartitionHandler cuentaAnualPartitionHandler(
            Step procesarMovimientosAnualesWorkerStep,
            EscaladoPartitionHandlerFactory escaladoPartitionHandlerFactory) {
        return escaladoPartitionHandlerFactory.crear(
                "procesarMovimientosAnualesWorkerStep", procesarMovimientosAnualesWorkerStep);
    }

    @Bean
    public Step procesarMovimientosAnualesMasterStep(JobRepository jobRepository,
                                                     BancoRangoPartitioner cuentaAnualPartitioner,
                                                     PartitionHandler cuentaAnualPartitionHandler) {
        return new StepBuilder("procesarMovimientosAnualesMasterStep", jobRepository)
                .partitioner("procesarMovimientosAnualesWorkerStep", cuentaAnualPartitioner)
                .partitionHandler(cuentaAnualPartitionHandler)
                .build();
    }

    @Bean
    public Tasklet generarEstadosCuentaTasklet(JdbcTemplate jdbcTemplate) {
        return (contribution, chunkContext) -> {
            jdbcTemplate.update("DELETE FROM estado_cuenta_anual");
            jdbcTemplate.update("""
                    INSERT INTO estado_cuenta_anual
                        (cuenta_id, anio, total_depositos, total_retiros, saldo_neto, cantidad_movimientos)
                    SELECT cuenta_id,
                           EXTRACT(YEAR FROM MIN(fecha)),
                           COALESCE(SUM(CASE WHEN transaccion = 'deposito' THEN monto ELSE 0 END), 0),
                           COALESCE(SUM(CASE WHEN transaccion IN ('retiro', 'compra') THEN ABS(monto) ELSE 0 END), 0),
                           COALESCE(SUM(CASE WHEN transaccion = 'deposito' THEN monto ELSE -ABS(monto) END), 0),
                           COUNT(*)
                    FROM movimiento_anual
                    GROUP BY cuenta_id
                    """);
            return RepeatStatus.FINISHED;
        };
    }

    @Bean
    public Step generarEstadosCuentaStep(JobRepository jobRepository,
                                         PlatformTransactionManager transactionManager,
                                         Tasklet generarEstadosCuentaTasklet) {
        return new StepBuilder("generarEstadosCuentaStep", jobRepository)
                .tasklet(generarEstadosCuentaTasklet, transactionManager)
                .build();
    }

    @Bean
    public Job estadosCuentaAnualesJob(JobRepository jobRepository,
                                       Step procesarMovimientosAnualesMasterStep,
                                       Step generarEstadosCuentaStep,
                                       BancoJobListener bancoJobListener) {
        return new JobBuilder("estadosCuentaAnualesJob", jobRepository)
                .incrementer(new RunIdIncrementer())
                .listener(bancoJobListener)
                .start(procesarMovimientosAnualesMasterStep)
                .next(generarEstadosCuentaStep)
                .build();
    }
}
