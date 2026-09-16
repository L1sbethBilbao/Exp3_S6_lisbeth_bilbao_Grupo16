package cl.duoc.bancoxyz.bff.web.clients;

import java.util.List;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import cl.duoc.bancoxyz.bff.web.dtos.core.CuentaCoreDTO;
import cl.duoc.bancoxyz.bff.web.dtos.core.MovimientoCoreDTO;
import cl.duoc.bancoxyz.bff.web.dtos.core.TransaccionCoreDTO;
import cl.duoc.bancoxyz.bff.web.exceptions.RecursoNoEncontradoException;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class MsCuentasClient {

    private final RestClient msCuentasRestClient;

    public List<CuentaCoreDTO> obtenerCuentas() {
        return msCuentasRestClient.get().uri("/core/cuentas").retrieve()
                .body(new ParameterizedTypeReference<>() {
                });
    }

    public CuentaCoreDTO obtenerCuenta(Long cuentaId) {
        try {
            return msCuentasRestClient.get().uri("/core/cuentas/{id}", cuentaId).retrieve()
                    .body(CuentaCoreDTO.class);
        } catch (HttpClientErrorException.NotFound ex) {
            throw new RecursoNoEncontradoException("No existe la cuenta " + cuentaId);
        }
    }

    public List<MovimientoCoreDTO> obtenerMovimientos(Long cuentaId) {
        try {
            return msCuentasRestClient.get().uri("/core/cuentas/{id}/movimientos", cuentaId).retrieve()
                    .body(new ParameterizedTypeReference<>() {
                    });
        } catch (HttpClientErrorException.NotFound ex) {
            throw new RecursoNoEncontradoException("No existe la cuenta " + cuentaId);
        }
    }

    public List<TransaccionCoreDTO> obtenerTransacciones() {
        return msCuentasRestClient.get().uri("/core/transacciones").retrieve()
                .body(new ParameterizedTypeReference<>() {
                });
    }
}
