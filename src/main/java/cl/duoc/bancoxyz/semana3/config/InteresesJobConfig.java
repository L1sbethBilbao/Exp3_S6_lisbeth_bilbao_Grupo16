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
import org.springframework.batch.item.database.JdbcBatchItemWriter;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.retry.backoff.ExponentialBackOffPolicy;
import org.springframework.transaction.PlatformTransactionManager;

import cl.duoc.bancoxyz.semana3.dtos.InteresDTO;
import cl.duoc.bancoxyz.semana3.entities.InteresEntity;
import cl.duoc.bancoxyz.semana3.listeners.BancoJobListener;
import cl.duoc.bancoxyz.semana3.listeners.BancoSkipListener;
import cl.duoc.bancoxyz.semana3.listeners.BancoStepListener;
import cl.duoc.bancoxyz.semana3.partitioners.BancoRangoPartitioner;
import cl.duoc.bancoxyz.semana3.policies.BancoChunkCompletionPolicy;
import cl.duoc.bancoxyz.semana3.policies.BancoRetryPolicy;
import cl.duoc.bancoxyz.semana3.policies.BancoSkipPolicy;
import cl.duoc.bancoxyz.semana3.processors.InteresProcessor;
import cl.duoc.bancoxyz.semana3.services.ServicioTasas;

@Configuration
public class InteresesJobConfig {

    @Value("${bancoxyz.archivo.intereses}")
    private Resource archivoIntereses;

    @Value("${spring.sql.init.platform:oracle}")
    private String sqlPlatform;

    @Bean
    public BancoRangoPartitioner interesPartitioner() {
        return new BancoRangoPartitioner(archivoIntereses, "intereses-");
    }

    @Bean
    @StepScope
    public FlatFileItemReader<InteresDTO> interesItemReader(
            @Value("#{stepExecutionContext['start']}") int start,
            @Value("#{stepExecutionContext['end']}") int end) {
        return LectoresCsvRango.deRango(
                "interesItemReader",
                archivoIntereses,
                new String[] {"cuentaId", "nombre", "saldo", "edad", "tipo"},
                InteresDTO.class,
                start,
                end);
    }

    @Bean
    @StepScope
    public InteresProcessor interesItemProcessor(ServicioTasas servicioTasas) {
        return new InteresProcessor(servicioTasas);
    }

    @Bean
    public JdbcBatchItemWriter<InteresEntity> interesItemWriter(DataSource dataSource) {
        return IdempotentWriters.intereses(dataSource, sqlPlatform);
    }

    @Bean
    public Step calcularInteresesWorkerStep(JobRepository jobRepository,
                                            PlatformTransactionManager transactionManager,
                                            FlatFileItemReader<InteresDTO> interesItemReader,
                                            InteresProcessor interesItemProcessor,
                                            JdbcBatchItemWriter<InteresEntity> interesItemWriter,
                                            BancoSkipListener bancoSkipListener,
                                            BancoStepListener bancoStepListener) {
        return new StepBuilder("calcularInteresesWorkerStep", jobRepository)
                .<InteresDTO, InteresEntity>chunk(
                        new BancoChunkCompletionPolicy(
                                BatchInfrastructureConfig.CHUNK_SIZE,
                                BatchInfrastructureConfig.CHUNK_MAX_DURATION_MS),
                        transactionManager)
                .reader(interesItemReader)
                .processor(interesItemProcessor)
                .writer(interesItemWriter)
                .faultTolerant()
                .skipPolicy(new BancoSkipPolicy())
                .retryPolicy(new BancoRetryPolicy(BatchInfrastructureConfig.RETRY_LIMIT))
                .backOffPolicy(new ExponentialBackOffPolicy())
                .listener(bancoSkipListener)
                .listener(bancoStepListener)
                .build();
    }

    @Bean
    public PartitionHandler interesPartitionHandler(
            Step calcularInteresesWorkerStep,
            EscaladoPartitionHandlerFactory escaladoPartitionHandlerFactory) {
        return escaladoPartitionHandlerFactory.crear(
                "calcularInteresesWorkerStep", calcularInteresesWorkerStep);
    }

    @Bean
    public Step calcularInteresesMasterStep(JobRepository jobRepository,
                                            BancoRangoPartitioner interesPartitioner,
                                            PartitionHandler interesPartitionHandler) {
        return new StepBuilder("calcularInteresesMasterStep", jobRepository)
                .partitioner("calcularInteresesWorkerStep", interesPartitioner)
                .partitionHandler(interesPartitionHandler)
                .build();
    }

    @Bean
    public Job interesesMensualesJob(JobRepository jobRepository,
                                     Step calcularInteresesMasterStep,
                                     BancoJobListener bancoJobListener) {
        return new JobBuilder("interesesMensualesJob", jobRepository)
                .incrementer(new RunIdIncrementer())
                .listener(bancoJobListener)
                .start(calcularInteresesMasterStep)
                .build();
    }
}
