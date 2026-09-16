package cl.duoc.bancoxyz.bff.cajero.services;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.stereotype.Service;

import cl.duoc.bancoxyz.bff.cajero.clients.MsCuentasClient;
import cl.duoc.bancoxyz.bff.cajero.dtos.MovimientoCajeroDTO;
import cl.duoc.bancoxyz.bff.cajero.dtos.RetiroRespuestaDTO;
import cl.duoc.bancoxyz.bff.cajero.dtos.SaldoCajeroDTO;
import cl.duoc.bancoxyz.bff.cajero.dtos.core.CuentaCoreDTO;
import cl.duoc.bancoxyz.bff.cajero.exceptions.OperacionRechazadaException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class BffCajeroService {

    private final MsCuentasClient msCuentasClient;

    public SaldoCajeroDTO consultarSaldo(Long cuentaId) {
        CuentaCoreDTO cuenta = msCuentasClient.obtenerCuenta(cuentaId);
        return new SaldoCajeroDTO(cuenta.cuentaId(), cuenta.saldo());
    }

    public List<MovimientoCajeroDTO> ultimosMovimientos(Long cuentaId) {
        return msCuentasClient.obtenerMovimientos(cuentaId).stream()
                .limit(2)
                .map(m -> new MovimientoCajeroDTO(m.fecha().toString(), m.tipo(), m.monto()))
                .toList();
    }

    public RetiroRespuestaDTO retirar(Long cuentaId, BigDecimal monto) {
        if (monto == null || monto.compareTo(BigDecimal.ZERO) <= 0) {
            throw new OperacionRechazadaException("El monto debe ser mayor a 0");
        }
        CuentaCoreDTO actualizada = msCuentasClient.retirar(cuentaId, monto);
        return new RetiroRespuestaDTO(actualizada.cuentaId(), monto, actualizada.saldo());
    }
}
