package cl.duoc.bancoxyz.bff.cajero.clients;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import cl.duoc.bancoxyz.bff.cajero.dtos.core.CuentaCoreDTO;
import cl.duoc.bancoxyz.bff.cajero.dtos.core.MovimientoCoreDTO;
import cl.duoc.bancoxyz.bff.cajero.exceptions.OperacionRechazadaException;
import cl.duoc.bancoxyz.bff.cajero.exceptions.RecursoNoEncontradoException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class MsCuentasClient {

    private final RestTemplate restTemplate;

    @Value("${ms-cuentas.service-id:MS-CUENTAS}")
    private String msCuentasServiceId;

    @CircuitBreaker(name = "msCuentas", fallbackMethod = "fallbackCuenta")
    @Retry(name = "msCuentas")
    public CuentaCoreDTO obtenerCuenta(Long cuentaId) {
        try {
            return restTemplate.getForObject(base() + "/core/cuentas/{id}", CuentaCoreDTO.class, cuentaId);
        } catch (HttpClientErrorException.NotFound ex) {
            throw new RecursoNoEncontradoException("Cuenta no encontrada");
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
            throw new RecursoNoEncontradoException("Cuenta no encontrada");
        }
    }

    @CircuitBreaker(name = "msCuentas", fallbackMethod = "fallbackRetiro")
    @Retry(name = "msCuentas")
    public CuentaCoreDTO retirar(Long cuentaId, BigDecimal monto) {
        try {
            ResponseEntity<CuentaCoreDTO> response = restTemplate.exchange(
                    base() + "/core/cuentas/{id}/retiro",
                    HttpMethod.POST,
                    new HttpEntity<>(Map.of("monto", monto)),
                    CuentaCoreDTO.class,
                    cuentaId);
            return response.getBody();
        } catch (HttpClientErrorException.NotFound ex) {
            throw new RecursoNoEncontradoException("Cuenta no encontrada");
        } catch (HttpClientErrorException.Conflict ex) {
            throw new OperacionRechazadaException("Saldo insuficiente");
        } catch (HttpClientErrorException.BadRequest ex) {
            throw new OperacionRechazadaException("Monto de retiro no valido");
        }
    }

    private String base() {
        return "http://" + msCuentasServiceId;
    }

    @SuppressWarnings("unused")
    private CuentaCoreDTO fallbackCuenta(Long cuentaId, Exception ex) {
        log.warn("FALLBACK cajero cuenta {}: {}", cuentaId, ex.getMessage());
        throw new IllegalStateException("ms-cuentas no disponible (fallback cajero). Causa: " + ex.getMessage());
    }

    @SuppressWarnings("unused")
    private List<MovimientoCoreDTO> fallbackMovimientos(Long cuentaId, Exception ex) {
        log.warn("FALLBACK cajero movimientos {}: {}", cuentaId, ex.getMessage());
        return Collections.emptyList();
    }

    @SuppressWarnings("unused")
    private CuentaCoreDTO fallbackRetiro(Long cuentaId, BigDecimal monto, Exception ex) {
        log.warn("FALLBACK cajero retiro {} monto {}: {}", cuentaId, monto, ex.getMessage());
        throw new OperacionRechazadaException("Retiro no disponible: ms-cuentas temporalmente caido");
    }
}
