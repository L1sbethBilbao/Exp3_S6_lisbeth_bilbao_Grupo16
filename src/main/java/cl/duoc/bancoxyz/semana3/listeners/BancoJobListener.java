package cl.duoc.bancoxyz.semana3.listeners;

import java.time.Duration;
import java.time.LocalDateTime;

import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.batch.core.StepExecution;

import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Resume leidos / escritos / omitidos y publica metricas Micrometer del Job.
 */
@Slf4j
@RequiredArgsConstructor
public class BancoJobListener implements JobExecutionListener {

    private final MeterRegistry meterRegistry;

    @Override
    public void beforeJob(JobExecution jobExecution) {
        log.info(">> Iniciando Job particionado '{}' (ejecucion #{})",
                jobExecution.getJobInstance().getJobName(), jobExecution.getId());
    }

    @Override
    public void afterJob(JobExecution jobExecution) {
        long leidos = jobExecution.getStepExecutions().stream()
                .filter(se -> !se.getStepName().contains(":partition"))
                .mapToLong(StepExecution::getReadCount)
                .sum();
        long escritos = jobExecution.getStepExecutions().stream()
                .filter(se -> !se.getStepName().contains(":partition"))
                .mapToLong(StepExecution::getWriteCount)
                .sum();
        long saltados = jobExecution.getStepExecutions().stream()
                .filter(se -> !se.getStepName().contains(":partition"))
                .mapToLong(StepExecution::getSkipCount)
                .sum();
        long particiones = jobExecution.getStepExecutions().stream()
                .filter(se -> se.getStepName().contains(":partition"))
                .count();

        log.info("==================================================");
        log.info(">> Job '{}' finalizado con estado {}. Particiones={}, Leidos={}, Escritos={}, Saltados={}",
                jobExecution.getJobInstance().getJobName(),
                jobExecution.getStatus(),
                particiones,
                leidos,
                escritos,
                saltados);
        jobExecution.getStepExecutions().forEach(step ->
                log.info("  Step [{}] leidos={} escritos={} omitidos={} estado={}",
                        step.getStepName(),
                        step.getReadCount(),
                        step.getWriteCount(),
                        step.getSkipCount(),
                        step.getStatus()));
        log.info("==================================================");

        publicarMetricas(jobExecution, leidos, escritos, saltados);
    }

    private void publicarMetricas(JobExecution jobExecution, long leidos, long escritos, long saltados) {
        String job = jobExecution.getJobInstance().getJobName();
        String status = jobExecution.getStatus().name();
        LocalDateTime inicio = jobExecution.getStartTime();
        LocalDateTime fin = jobExecution.getEndTime();
        if (inicio != null && fin != null) {
            meterRegistry.timer("bancoxyz.job.duration", "job", job, "status", status)
                    .record(Duration.between(inicio, fin));
        }
        meterRegistry.counter("bancoxyz.job.items.read", "job", job).increment(leidos);
        meterRegistry.counter("bancoxyz.job.items.written", "job", job).increment(escritos);
        meterRegistry.counter("bancoxyz.job.items.skipped", "job", job).increment(saltados);
        meterRegistry.counter("bancoxyz.job.executions", "job", job, "status", status).increment();
    }
}
