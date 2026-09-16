package cl.duoc.bancoxyz.semana3.config;

import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.integration.chunk.ChunkMessageChannelItemWriter;
import org.springframework.batch.integration.chunk.ChunkProcessorChunkHandler;
import org.springframework.batch.integration.config.annotation.EnableBatchIntegration;
import org.springframework.batch.integration.partition.BeanFactoryStepLocator;
import org.springframework.batch.integration.partition.StepExecutionRequestHandler;
import org.springframework.batch.core.step.item.SimpleChunkProcessor;
import org.springframework.batch.item.database.JdbcBatchItemWriter;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.channel.ExecutorChannel;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.core.MessagingTemplate;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.PollableChannel;

import cl.duoc.bancoxyz.semana3.dtos.TransaccionDTO;
import cl.duoc.bancoxyz.semana3.entities.TransaccionEntity;
import cl.duoc.bancoxyz.semana3.processors.TransaccionProcessor;

/**
 * Canales de Spring Integration para particionamiento remoto y remote chunking.
 * En esta entrega los canales viven en la misma JVM (sin RabbitMQ) para poder
 * demostrar el patron; en produccion el canal se cambia por una cola AMQP.
 */
@Configuration
@EnableIntegration
@EnableBatchIntegration
public class RemoteEscaladoConfig {

    @Bean(name = "partitionRequests")
    public MessageChannel partitionRequests(
            @Qualifier("particionTaskExecutor") TaskExecutor particionTaskExecutor) {
        return new ExecutorChannel(particionTaskExecutor);
    }

    @Bean(name = "partitionReplies")
    public PollableChannel partitionReplies() {
        return new QueueChannel();
    }

    @Bean
    @ServiceActivator(inputChannel = "partitionRequests", outputChannel = "partitionReplies")
    public StepExecutionRequestHandler partitionRequestHandler(JobExplorer jobExplorer, BeanFactory beanFactory) {
        BeanFactoryStepLocator stepLocator = new BeanFactoryStepLocator();
        stepLocator.setBeanFactory(beanFactory);
        StepExecutionRequestHandler handler = new StepExecutionRequestHandler();
        handler.setStepLocator(stepLocator);
        handler.setJobExplorer(jobExplorer);
        return handler;
    }

    @Bean(name = "chunkRequests")
    public MessageChannel chunkRequests(
            @Qualifier("particionTaskExecutor") TaskExecutor particionTaskExecutor) {
        return new ExecutorChannel(particionTaskExecutor);
    }

    @Bean(name = "chunkReplies")
    public PollableChannel chunkReplies() {
        return new QueueChannel();
    }

    @Bean
    public ChunkMessageChannelItemWriter<TransaccionDTO> transaccionChunkMessageWriter(
            @Qualifier("chunkRequests") MessageChannel chunkRequests,
            @Qualifier("chunkReplies") PollableChannel chunkReplies) {
        MessagingTemplate template = new MessagingTemplate();
        template.setDefaultChannel(chunkRequests);
        template.setReceiveTimeout(180_000);
        ChunkMessageChannelItemWriter<TransaccionDTO> writer = new ChunkMessageChannelItemWriter<>();
        writer.setMessagingOperations(template);
        writer.setReplyChannel(chunkReplies);
        return writer;
    }

    @Bean
    @ServiceActivator(inputChannel = "chunkRequests", outputChannel = "chunkReplies")
    public ChunkProcessorChunkHandler<TransaccionDTO> transaccionChunkProcessorHandler(
            JdbcBatchItemWriter<TransaccionEntity> transaccionItemWriter) {
        SimpleChunkProcessor<TransaccionDTO, TransaccionEntity> processor =
                new SimpleChunkProcessor<>(new TransaccionProcessor(), transaccionItemWriter);
        ChunkProcessorChunkHandler<TransaccionDTO> handler = new ChunkProcessorChunkHandler<>();
        handler.setChunkProcessor(processor);
        return handler;
    }
}
