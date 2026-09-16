package cl.duoc.bancoxyz.semana3.listeners;

import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class BancoStepListener implements StepExecutionListener {

    @Override
    public void beforeStep(StepExecution stepExecution) {
        log.info(">> Iniciando Step '{}' en hilo {}",
                stepExecution.getStepName(), Thread.currentThread().getName());
    }

    @Override
    public ExitStatus afterStep(StepExecution stepExecution) {
        log.info(">> Step '{}' finalizado con estado {} (leidos={}, escritos={}, saltados={})",
                stepExecution.getStepName(),
                stepExecution.getExitStatus().getExitCode(),
                stepExecution.getReadCount(),
                stepExecution.getWriteCount(),
                stepExecution.getSkipCount());
        return stepExecution.getExitStatus();
    }
}
