package cl.duoc.bancoxyz.ms.cuentas.services;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import cl.duoc.bancoxyz.ms.cuentas.dtos.CuentaCoreDTO;
import cl.duoc.bancoxyz.ms.cuentas.dtos.MovimientoCoreDTO;
import cl.duoc.bancoxyz.ms.cuentas.dtos.TransaccionCoreDTO;
import cl.duoc.bancoxyz.ms.cuentas.entities.CuentaEntity;
import cl.duoc.bancoxyz.ms.cuentas.entities.MovimientoEntity;
import cl.duoc.bancoxyz.ms.cuentas.entities.TransaccionEntity;
import cl.duoc.bancoxyz.ms.cuentas.exceptions.CuentaNoEncontradaException;
import cl.duoc.bancoxyz.ms.cuentas.exceptions.SaldoInsuficienteException;
import cl.duoc.bancoxyz.ms.cuentas.repositories.CuentaRepository;
import cl.duoc.bancoxyz.ms.cuentas.repositories.MovimientoRepository;
import cl.duoc.bancoxyz.ms.cuentas.repositories.TransaccionRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class BancoCoreService {

    private final CuentaRepository cuentaRepository;
    private final MovimientoRepository movimientoRepository;
    private final TransaccionRepository transaccionRepository;

    public List<CuentaCoreDTO> listarCuentas() {
        return cuentaRepository.findAll().stream().map(this::aCuenta).toList();
    }

    public CuentaCoreDTO obtenerCuenta(Long cuentaId) {
        return aCuenta(buscarCuenta(cuentaId));
    }

    public List<MovimientoCoreDTO> listarMovimientos(Long cuentaId) {
        buscarCuenta(cuentaId);
        return movimientoRepository.findByCuentaIdOrderByFechaDesc(cuentaId).stream()
                .map(this::aMovimiento)
                .toList();
    }

    public List<TransaccionCoreDTO> listarTransacciones() {
        return transaccionRepository.findAll().stream().map(this::aTransaccion).toList();
    }

    @Transactional
    public CuentaCoreDTO retirar(Long cuentaId, BigDecimal monto) {
        if (monto == null || monto.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("El monto del retiro debe ser mayor a 0");
        }
        CuentaEntity cuenta = buscarCuenta(cuentaId);
        if (cuenta.getSaldo().compareTo(monto) < 0) {
            throw new SaldoInsuficienteException(cuentaId);
        }
        cuenta.setSaldo(cuenta.getSaldo().subtract(monto));
        cuentaRepository.save(cuenta);
        movimientoRepository.save(new MovimientoEntity(
                null, cuentaId, LocalDate.now(), "retiro", monto, "Retiro en cajero"));
        return aCuenta(cuenta);
    }

    private CuentaEntity buscarCuenta(Long cuentaId) {
        return cuentaRepository.findById(cuentaId)
                .orElseThrow(() -> new CuentaNoEncontradaException(cuentaId));
    }

    private CuentaCoreDTO aCuenta(CuentaEntity e) {
        return new CuentaCoreDTO(e.getCuentaId(), e.getNombre(), e.getSaldo(), e.getEdad(), e.getTipo());
    }

    private MovimientoCoreDTO aMovimiento(MovimientoEntity e) {
        return new MovimientoCoreDTO(e.getCuentaId(), e.getFecha(), e.getTipo(), e.getMonto(), e.getDescripcion());
    }

    private TransaccionCoreDTO aTransaccion(TransaccionEntity e) {
        return new TransaccionCoreDTO(e.getTransaccionId(), e.getFecha(), e.getMonto(), e.getTipo());
    }
}
