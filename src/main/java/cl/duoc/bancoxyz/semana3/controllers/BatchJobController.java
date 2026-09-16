package cl.duoc.bancoxyz.semana3.controllers;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.StepExecution;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import cl.duoc.bancoxyz.semana3.exceptions.JobLaunchException;
import cl.duoc.bancoxyz.semana3.services.BatchJobService;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/jobs")
@RequiredArgsConstructor
public class BatchJobController {

    private final BatchJobService batchJobService;

    @PostMapping("/transacciones")
    public ResponseEntity<Map<String, Object>> transacciones() {
        return ResponseEntity.ok(respuesta(batchJobService.ejecutarTransacciones()));
    }

    @PostMapping("/intereses")
    public ResponseEntity<Map<String, Object>> intereses() {
        return ResponseEntity.ok(respuesta(batchJobService.ejecutarIntereses()));
    }

    @PostMapping("/estados-cuenta")
    public ResponseEntity<Map<String, Object>> estadosCuenta() {
        return ResponseEntity.ok(respuesta(batchJobService.ejecutarEstadosCuenta()));
    }

    @PostMapping("/todos")
    public ResponseEntity<Map<String, Object>> todos() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("transaccionesDiariasJob", respuesta(batchJobService.ejecutarTransacciones()));
        body.put("interesesMensualesJob", respuesta(batchJobService.ejecutarIntereses()));
        body.put("estadosCuentaAnualesJob", respuesta(batchJobService.ejecutarEstadosCuenta()));
        return ResponseEntity.ok(body);
    }

    @PostMapping("/{jobExecutionId}/restart")
    public ResponseEntity<Map<String, Object>> reanudar(@PathVariable long jobExecutionId) {
        return ResponseEntity.ok(respuesta(batchJobService.reanudar(jobExecutionId)));
    }

    @GetMapping("/ejecuciones")
    public ResponseEntity<List<Map<String, Object>>> ejecuciones() {
        return ResponseEntity.ok(batchJobService.listarEjecuciones());
    }

    @ExceptionHandler(JobLaunchException.class)
    public ResponseEntity<Map<String, Object>> alNoPoderIniciarElJob(JobLaunchException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", ex.getMessage()));
    }

    private Map<String, Object> respuesta(JobExecution execution) {
        Map<String, Object> steps = new LinkedHashMap<>();
        for (StepExecution step : execution.getStepExecutions()) {
            steps.put(step.getStepName(), Map.of(
                    "estado", step.getStatus().toString(),
                    "leidos", step.getReadCount(),
                    "escritos", step.getWriteCount(),
                    "omitidos", step.getSkipCount()));
        }
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("job", execution.getJobInstance().getJobName());
        body.put("jobExecutionId", execution.getId());
        body.put("estado", execution.getStatus().toString());
        body.put("exitStatus", execution.getExitStatus().getExitCode());
        body.put("steps", steps);
        return body;
    }
}
