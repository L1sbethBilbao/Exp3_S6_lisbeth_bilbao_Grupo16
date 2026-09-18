package cl.duoc.bancoxyz.bff.web.clients;

import java.util.Collections;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import cl.duoc.bancoxyz.bff.web.dtos.core.CuentaCoreDTO;
import cl.duoc.bancoxyz.bff.web.dtos.core.MovimientoCoreDTO;
import cl.duoc.bancoxyz.bff.web.dtos.core.TransaccionCoreDTO;
import cl.duoc.bancoxyz.bff.web.exceptions.RecursoNoEncontradoException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Consume ms-cuentas por nombre Eureka (MS-CUENTAS), no por URL fija.
 * Resilience4j protege las llamadas salientes (CircuitBreaker + Retry + Fallback).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MsCuentasClient {

    private final RestTemplate restTemplate;

    @Value("${ms-cuentas.service-id:MS-CUENTAS}")
    private String msCuentasServiceId;

    @CircuitBreaker(name = "msCuentas", fallbackMethod = "fallbackCuentas")
    @Retry(name = "msCuentas")
    public List<CuentaCoreDTO> obtenerCuentas() {
        ResponseEntity<List<CuentaCoreDTO>> response = restTemplate.exchange(
                base() + "/core/cuentas",
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<>() {
                });
        return response.getBody();
    }

    @CircuitBreaker(name = "msCuentas", fallbackMethod = "fallbackCuenta")
    @Retry(name = "msCuentas")
    public CuentaCoreDTO obtenerCuenta(Long cuentaId) {
        try {
            return restTemplate.getForObject(base() + "/core/cuentas/{id}", CuentaCoreDTO.class, cuentaId);
        } catch (HttpClientErrorException.NotFound ex) {
            throw new RecursoNoEncontradoException("No existe la cuenta " + cuentaId);
        }
    }

    @CircuitBreaker(name = "msCuentas", fallbackMethod = "fallbackMovimientos")
    @Retry(name = "msCuentas")
    public List<MovimientoCoreDTO> obtenerMovimientos(Long cuentaId) {
        try {
            ResponseEntity<List<MovimientoCoreDTO>> response = restTemplate.exchange(
                    base() + "/core/cuentas/{id}/movimientos",
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<>() {
                    },
                    cuentaId);
            return response.getBody();
        } catch (HttpClientErrorException.NotFound ex) {
            throw new RecursoNoEncontradoException("No existe la cuenta " + cuentaId);
        }
    }

    @CircuitBreaker(name = "msCuentas", fallbackMethod = "fallbackTransacciones")
    @Retry(name = "msCuentas")
    public List<TransaccionCoreDTO> obtenerTransacciones() {
        ResponseEntity<List<TransaccionCoreDTO>> response = restTemplate.exchange(
                base() + "/core/transacciones",
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<>() {
                });
        return response.getBody();
    }

    private String base() {
        return "http://" + msCuentasServiceId;
    }

    @SuppressWarnings("unused")
    private List<CuentaCoreDTO> fallbackCuentas(Exception ex) {
        log.warn("FALLBACK listar cuentas: {}", ex.getMessage());
        return Collections.emptyList();
    }

    @SuppressWarnings("unused")
    private CuentaCoreDTO fallbackCuenta(Long cuentaId, Exception ex) {
        log.warn("FALLBACK cuenta {}: {}", cuentaId, ex.getMessage());
        throw new IllegalStateException(
                "ms-cuentas no disponible (fallback). Intente mas tarde. Causa: " + ex.getMessage());
    }

    @SuppressWarnings("unused")
    private List<MovimientoCoreDTO> fallbackMovimientos(Long cuentaId, Exception ex) {
        log.warn("FALLBACK movimientos {}: {}", cuentaId, ex.getMessage());
        return Collections.emptyList();
    }

    @SuppressWarnings("unused")
    private List<TransaccionCoreDTO> fallbackTransacciones(Exception ex) {
        log.warn("FALLBACK transacciones: {}", ex.getMessage());
        return Collections.emptyList();
    }
}
