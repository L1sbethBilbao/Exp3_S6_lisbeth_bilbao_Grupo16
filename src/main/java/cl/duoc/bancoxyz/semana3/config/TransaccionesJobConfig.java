package cl.duoc.bancoxyz.semana3.config;

import java.time.LocalDateTime;

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
import org.springframework.batch.integration.chunk.ChunkMessageChannelItemWriter;
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

import cl.duoc.bancoxyz.semana3.dtos.TransaccionDTO;
import cl.duoc.bancoxyz.semana3.entities.TransaccionEntity;
import cl.duoc.bancoxyz.semana3.listeners.BancoJobListener;
import cl.duoc.bancoxyz.semana3.listeners.BancoSkipListener;
import cl.duoc.bancoxyz.semana3.listeners.BancoStepListener;
import cl.duoc.bancoxyz.semana3.partitioners.BancoRangoPartitioner;
import cl.duoc.bancoxyz.semana3.policies.BancoChunkCompletionPolicy;
import cl.duoc.bancoxyz.semana3.policies.BancoRetryPolicy;
import cl.duoc.bancoxyz.semana3.policies.BancoSkipPolicy;
import cl.duoc.bancoxyz.semana3.processors.TransaccionProcessor;

@Configuration
public class TransaccionesJobConfig {

    @Value("${bancoxyz.archivo.transacciones}")
    private Resource archivoTransacciones;

    @Value("${spring.sql.init.platform:oracle}")
    private String sqlPlatform;

    @Bean
    public BancoRangoPartitioner transaccionPartitioner() {
        return new BancoRangoPartitioner(archivoTransacciones, "transacciones-");
    }

    @Bean
    @StepScope
    public FlatFileItemReader<TransaccionDTO> transaccionItemReader(
            @Value("#{stepExecutionContext['start']}") int start,
            @Value("#{stepExecutionContext['end']}") int end) {
        return LectoresCsvRango.deRango(
                "transaccionItemReader",
                archivoTransacciones,
                new String[] {"id", "fecha", "monto", "tipo"},
                TransaccionDTO.class,
                start,
                end);
    }

    @Bean
    @StepScope
    public TransaccionProcessor transaccionItemProcessor() {
        return new TransaccionProcessor();
    }

    @Bean
    public JdbcBatchItemWriter<TransaccionEntity> transaccionItemWriter(DataSource dataSource) {
        return IdempotentWriters.transacciones(dataSource, sqlPlatform);
    }

    @Bean
    public FlatFileItemReader<TransaccionDTO> transaccionItemReaderCompleto() {
        return LectoresCsvRango.completo(
                "transaccionItemReaderCompleto",
                archivoTransacciones,
                new String[] {"id", "fecha", "monto", "tipo"},
                TransaccionDTO.class);
    }

    @Bean
    public Step procesarTransaccionesWorkerStep(JobRepository jobRepository,
                                                PlatformTransactionManager transactionManager,
                                                FlatFileItemReader<TransaccionDTO> transaccionItemReader,
                                                TransaccionProcessor transaccionItemProcessor,
                                                JdbcBatchItemWriter<TransaccionEntity> transaccionItemWriter,
                                                BancoSkipListener bancoSkipListener,
                                                BancoStepListener bancoStepListener) {
        return new StepBuilder("procesarTransaccionesWorkerStep", jobRepository)
                .<TransaccionDTO, TransaccionEntity>chunk(
                        new BancoChunkCompletionPolicy(
                                BatchInfrastructureConfig.CHUNK_SIZE,
                                BatchInfrastructureConfig.CHUNK_MAX_DURATION_MS),
                        transactionManager)
                .reader(transaccionItemReader)
                .processor(transaccionItemProcessor)
                .writer(transaccionItemWriter)
                .faultTolerant()
                .skipPolicy(new BancoSkipPolicy())
                .retryPolicy(new BancoRetryPolicy(BatchInfrastructureConfig.RETRY_LIMIT))
                .backOffPolicy(new ExponentialBackOffPolicy())
                .listener(bancoSkipListener)
                .listener(bancoStepListener)
                .build();
    }

    @Bean
    public PartitionHandler transaccionPartitionHandler(
            Step procesarTransaccionesWorkerStep,
            EscaladoPartitionHandlerFactory escaladoPartitionHandlerFactory) {
        return escaladoPartitionHandlerFactory.crear(
                "procesarTransaccionesWorkerStep", procesarTransaccionesWorkerStep);
    }

    @Bean
    public Step procesarTransaccionesChunkMasterStep(JobRepository jobRepository,
                                                     PlatformTransactionManager transactionManager,
                                                     FlatFileItemReader<TransaccionDTO> transaccionItemReaderCompleto,
                                                     ChunkMessageChannelItemWriter<TransaccionDTO> transaccionChunkMessageWriter,
                                                     BancoStepListener bancoStepListener) {
        return new StepBuilder("procesarTransaccionesChunkMasterStep", jobRepository)
                .<TransaccionDTO, TransaccionDTO>chunk(
                        BatchInfrastructureConfig.CHUNK_SIZE, transactionManager)
                .reader(transaccionItemReaderCompleto)
                .writer(transaccionChunkMessageWriter)
                .listener(bancoStepListener)
                .build();
    }

    @Bean
    public Step procesarTransaccionesMasterStep(JobRepository jobRepository,
                                                BancoRangoPartitioner transaccionPartitioner,
                                                PartitionHandler transaccionPartitionHandler) {
        return new StepBuilder("procesarTransaccionesMasterStep", jobRepository)
                .partitioner("procesarTransaccionesWorkerStep", transaccionPartitioner)
                .partitionHandler(transaccionPartitionHandler)
                .build();
    }

    @Bean
    public Tasklet resumenTransaccionesTasklet(JdbcTemplate jdbcTemplate) {
        return (contribution, chunkContext) -> {
            Long jobExecutionId = chunkContext.getStepContext().getStepExecution().getJobExecutionId();
            jdbcTemplate.update("DELETE FROM resumen_transacciones WHERE job_execution_id = ?", jobExecutionId);
            jdbcTemplate.update("""
                    INSERT INTO resumen_transacciones
                        (fecha_proceso, total_validas, total_debitos, total_creditos,
                         cantidad_debitos, cantidad_creditos, job_execution_id)
                    SELECT ?,
                           COUNT(*),
                           COALESCE(SUM(CASE WHEN tipo = 'debito' THEN monto ELSE 0 END), 0),
                           COALESCE(SUM(CASE WHEN tipo = 'credito' THEN monto ELSE 0 END), 0),
                           SUM(CASE WHEN tipo = 'debito' THEN 1 ELSE 0 END),
                           SUM(CASE WHEN tipo = 'credito' THEN 1 ELSE 0 END),
                           ?
                    FROM transaccion_procesada
                    """, LocalDateTime.now(), jobExecutionId);
            return RepeatStatus.FINISHED;
        };
    }

    @Bean
    public Step generarResumenTransaccionesStep(JobRepository jobRepository,
                                                PlatformTransactionManager transactionManager,
                                                Tasklet resumenTransaccionesTasklet) {
        return new StepBuilder("generarResumenTransaccionesStep", jobRepository)
                .tasklet(resumenTransaccionesTasklet, transactionManager)
                .build();
    }

    @Bean
    public Job transaccionesDiariasJob(JobRepository jobRepository,
                                       Step procesarTransaccionesMasterStep,
                                       Step procesarTransaccionesChunkMasterStep,
                                       Step generarResumenTransaccionesStep,
                                       BancoJobListener bancoJobListener,
                                       EscaladoPartitionHandlerFactory escaladoPartitionHandlerFactory) {
        Step primerStep = escaladoPartitionHandlerFactory.usaChunkingRemoto()
                ? procesarTransaccionesChunkMasterStep
                : procesarTransaccionesMasterStep;
        return new JobBuilder("transaccionesDiariasJob", jobRepository)
                .incrementer(new RunIdIncrementer())
                .listener(bancoJobListener)
                .start(primerStep)
                .next(generarResumenTransaccionesStep)
                .build();
    }
}
