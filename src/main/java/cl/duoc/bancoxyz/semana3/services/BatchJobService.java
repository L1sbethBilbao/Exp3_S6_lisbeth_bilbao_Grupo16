package cl.duoc.bancoxyz.semana3.services;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobInstance;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.JobParametersInvalidException;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.batch.core.launch.NoSuchJobException;
import org.springframework.batch.core.launch.NoSuchJobExecutionException;
import org.springframework.batch.core.repository.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.repository.JobInstanceAlreadyCompleteException;
import org.springframework.batch.core.repository.JobRestartException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import cl.duoc.bancoxyz.semana3.exceptions.JobLaunchException;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class BatchJobService {

    private static final List<String> NOMBRES_JOB = List.of(
            "transaccionesDiariasJob",
            "interesesMensualesJob",
            "estadosCuentaAnualesJob");

    private final JobLauncher jobLauncher;
    private final JobExplorer jobExplorer;
    private final JobOperator jobOperator;
    private final Job transaccionesDiariasJob;
    private final Job interesesMensualesJob;
    private final Job estadosCuentaAnualesJob;

    public BatchJobService(JobLauncher jobLauncher,
                           JobExplorer jobExplorer,
                           JobOperator jobOperator,
                           @Qualifier("transaccionesDiariasJob") Job transaccionesDiariasJob,
                           @Qualifier("interesesMensualesJob") Job interesesMensualesJob,
                           @Qualifier("estadosCuentaAnualesJob") Job estadosCuentaAnualesJob) {
        this.jobLauncher = jobLauncher;
        this.jobExplorer = jobExplorer;
        this.jobOperator = jobOperator;
        this.transaccionesDiariasJob = transaccionesDiariasJob;
        this.interesesMensualesJob = interesesMensualesJob;
        this.estadosCuentaAnualesJob = estadosCuentaAnualesJob;
    }

    public JobExecution ejecutarTransacciones() {
        return lanzar(transaccionesDiariasJob);
    }

    public JobExecution ejecutarIntereses() {
        return lanzar(interesesMensualesJob);
    }

    public JobExecution ejecutarEstadosCuenta() {
        return lanzar(estadosCuentaAnualesJob);
    }

    /**
     * Reanuda una ejecucion FAILED/STOPPED desde el ultimo chunk confirmado
     * (JobRepository). No vuelve a procesar lo que ya quedo COMPLETED.
     */
    public JobExecution reanudar(long jobExecutionId) {
        try {
            log.info("Reanudando JobExecution #{}", jobExecutionId);
            Long nuevaEjecucion = jobOperator.restart(jobExecutionId);
            JobExecution execution = jobExplorer.getJobExecution(nuevaEjecucion);
            if (execution == null) {
                throw new JobLaunchException("No se encontro la ejecucion reanudada #" + nuevaEjecucion);
            }
            return execution;
        } catch (JobInstanceAlreadyCompleteException | NoSuchJobExecutionException
                | NoSuchJobException | JobRestartException | JobParametersInvalidException ex) {
            log.error("No fue posible reanudar la ejecucion #{}", jobExecutionId, ex);
            throw new JobLaunchException(
                    "No fue posible reanudar la ejecucion #" + jobExecutionId + ": " + ex.getMessage(), ex);
        }
    }

    public List<Map<String, Object>> listarEjecuciones() {
        List<Map<String, Object>> resultado = new ArrayList<>();
        for (String nombre : NOMBRES_JOB) {
            for (JobInstance instancia : jobExplorer.getJobInstances(nombre, 0, 8)) {
                for (JobExecution execution : jobExplorer.getJobExecutions(instancia)) {
                    Map<String, Object> fila = new LinkedHashMap<>();
                    fila.put("job", nombre);
                    fila.put("jobInstanceId", instancia.getInstanceId());
                    fila.put("jobExecutionId", execution.getId());
                    fila.put("estado", execution.getStatus().toString());
                    fila.put("exitStatus", execution.getExitStatus().getExitCode());
                    fila.put("reanudable", execution.getStatus() == BatchStatus.FAILED
                            || execution.getStatus() == BatchStatus.STOPPED);
                    resultado.add(fila);
                }
            }
        }
        return resultado;
    }

    private JobExecution lanzar(Job job) {
        try {
            JobParametersBuilder parametersBuilder = new JobParametersBuilder(jobExplorer)
                    .getNextJobParameters(job);
            log.info("Lanzando Job {}", job.getName());
            return jobLauncher.run(job, parametersBuilder.toJobParameters());
        } catch (JobExecutionAlreadyRunningException | JobRestartException
                | JobInstanceAlreadyCompleteException | JobParametersInvalidException ex) {
            log.error("No fue posible iniciar el Job {}", job.getName(), ex);
            throw new JobLaunchException("No fue posible iniciar el Job " + job.getName() + ": " + ex.getMessage(), ex);
        }
    }
}
