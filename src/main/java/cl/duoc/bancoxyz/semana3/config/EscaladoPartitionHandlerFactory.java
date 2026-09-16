package cl.duoc.bancoxyz.semana3.config;

import org.springframework.batch.core.Step;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.partition.PartitionHandler;
import org.springframework.batch.core.partition.support.TaskExecutorPartitionHandler;
import org.springframework.batch.integration.partition.MessageChannelPartitionHandler;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.task.TaskExecutor;
import org.springframework.integration.core.MessagingTemplate;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.PollableChannel;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

/**
 * Semana 3 extra: el maestro puede repartir con hilos locales o enviando
 * el rango start/end por un canal (patron de particionamiento remoto).
 */
@Slf4j
@Component
public class EscaladoPartitionHandlerFactory {

    private final TaskExecutor particionTaskExecutor;
    private final MessageChannel partitionRequests;
    private final PollableChannel partitionReplies;
    private final JobExplorer jobExplorer;
    private final int gridSize;
    private final String modo;

    public EscaladoPartitionHandlerFactory(
            @Qualifier("particionTaskExecutor") TaskExecutor particionTaskExecutor,
            @Qualifier("partitionRequests") MessageChannel partitionRequests,
            @Qualifier("partitionReplies") PollableChannel partitionReplies,
            JobExplorer jobExplorer,
            @Value("${bancoxyz.particion.grid-size:3}") int gridSize,
            @Value("${bancoxyz.escalado.modo:local}") String modo) {
        this.particionTaskExecutor = particionTaskExecutor;
        this.partitionRequests = partitionRequests;
        this.partitionReplies = partitionReplies;
        this.jobExplorer = jobExplorer;
        this.gridSize = gridSize;
        this.modo = modo;
    }

    public PartitionHandler crear(String workerStepName, Step workerStep) {
        if (usaParticionRemota()) {
            log.info("Escalado modo={} -> MessageChannelPartitionHandler ({})", modo, workerStepName);
            return remoto(workerStepName);
        }
        log.info("Escalado modo={} -> TaskExecutorPartitionHandler ({})", modo, workerStepName);
        return local(workerStep);
    }

    public boolean usaChunkingRemoto() {
        return "remoto-chunk".equalsIgnoreCase(modo);
    }

    private boolean usaParticionRemota() {
        return "remoto-particion".equalsIgnoreCase(modo);
    }

    private PartitionHandler local(Step workerStep) {
        TaskExecutorPartitionHandler handler = new TaskExecutorPartitionHandler();
        handler.setStep(workerStep);
        handler.setTaskExecutor(particionTaskExecutor);
        handler.setGridSize(gridSize);
        return handler;
    }

    private PartitionHandler remoto(String workerStepName) {
        try {
            MessageChannelPartitionHandler handler = new MessageChannelPartitionHandler();
            handler.setStepName(workerStepName);
            handler.setGridSize(gridSize);
            handler.setReplyChannel(partitionReplies);
            MessagingTemplate template = new MessagingTemplate();
            template.setDefaultChannel(partitionRequests);
            template.setReceiveTimeout(180_000);
            handler.setMessagingOperations(template);
            handler.setJobExplorer(jobExplorer);
            handler.afterPropertiesSet();
            return handler;
        } catch (Exception ex) {
            throw new IllegalStateException("No se pudo crear el PartitionHandler remoto", ex);
        }
    }
}
