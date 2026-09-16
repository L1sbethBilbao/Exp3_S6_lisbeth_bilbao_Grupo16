package cl.duoc.bancoxyz.bff.movil.services;

import java.util.List;

import org.springframework.stereotype.Service;

import cl.duoc.bancoxyz.bff.movil.clients.MsCuentasClient;
import cl.duoc.bancoxyz.bff.movil.dtos.CuentaMovilDetalleDTO;
import cl.duoc.bancoxyz.bff.movil.dtos.CuentaMovilResumenDTO;
import cl.duoc.bancoxyz.bff.movil.dtos.MovimientoMovilDTO;
import cl.duoc.bancoxyz.bff.movil.dtos.core.CuentaCoreDTO;
import cl.duoc.bancoxyz.bff.movil.dtos.core.MovimientoCoreDTO;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class BffMovilService {

    private static final int MAX_MOVIMIENTOS = 3;

    private final MsCuentasClient msCuentasClient;

    public List<CuentaMovilResumenDTO> listarCuentas() {
        return msCuentasClient.obtenerCuentas().stream()
                .map(c -> new CuentaMovilResumenDTO(c.cuentaId(), c.nombre(), c.saldo()))
                .toList();
    }

    public CuentaMovilDetalleDTO obtenerDetalle(Long cuentaId) {
        CuentaCoreDTO cuenta = msCuentasClient.obtenerCuenta(cuentaId);
        List<MovimientoMovilDTO> ultimos = msCuentasClient.obtenerMovimientos(cuentaId).stream()
                .limit(MAX_MOVIMIENTOS)
                .map(m -> new MovimientoMovilDTO(m.fecha().toString(), m.monto(), m.tipo()))
                .toList();
        return new CuentaMovilDetalleDTO(cuenta.cuentaId(), cuenta.nombre(), cuenta.saldo(), ultimos);
    }
}
