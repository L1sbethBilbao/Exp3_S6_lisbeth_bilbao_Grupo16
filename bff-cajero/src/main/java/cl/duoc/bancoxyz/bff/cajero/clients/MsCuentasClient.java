package cl.duoc.bancoxyz.bff.cajero.clients;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import cl.duoc.bancoxyz.bff.cajero.dtos.core.CuentaCoreDTO;
import cl.duoc.bancoxyz.bff.cajero.dtos.core.MovimientoCoreDTO;
import cl.duoc.bancoxyz.bff.cajero.exceptions.OperacionRechazadaException;
import cl.duoc.bancoxyz.bff.cajero.exceptions.RecursoNoEncontradoException;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class MsCuentasClient {

    private final RestClient msCuentasRestClient;

    public CuentaCoreDTO obtenerCuenta(Long cuentaId) {
        try {
            return msCuentasRestClient.get().uri("/core/cuentas/{id}", cuentaId).retrieve().body(CuentaCoreDTO.class);
        } catch (HttpClientErrorException.NotFound ex) {
            throw new RecursoNoEncontradoException("Cuenta no encontrada");
        }
    }

    public List<MovimientoCoreDTO> obtenerMovimientos(Long cuentaId) {
        try {
            return msCuentasRestClient.get().uri("/core/cuentas/{id}/movimientos", cuentaId).retrieve()
                    .body(new ParameterizedTypeReference<>() {
                    });
        } catch (HttpClientErrorException.NotFound ex) {
            throw new RecursoNoEncontradoException("Cuenta no encontrada");
        }
    }

    public CuentaCoreDTO retirar(Long cuentaId, BigDecimal monto) {
        try {
            return msCuentasRestClient.post()
                    .uri("/core/cuentas/{id}/retiro", cuentaId)
                    .body(Map.of("monto", monto))
                    .retrieve()
                    .body(CuentaCoreDTO.class);
        } catch (HttpClientErrorException.NotFound ex) {
            throw new RecursoNoEncontradoException("Cuenta no encontrada");
        } catch (HttpClientErrorException.Conflict ex) {
            throw new OperacionRechazadaException("Saldo insuficiente");
        } catch (HttpClientErrorException.BadRequest ex) {
            throw new OperacionRechazadaException("Monto de retiro no valido");
        }
    }
}
