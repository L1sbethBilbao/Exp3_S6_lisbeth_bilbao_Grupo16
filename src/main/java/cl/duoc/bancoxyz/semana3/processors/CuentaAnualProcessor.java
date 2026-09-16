package cl.duoc.bancoxyz.semana3.processors;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Set;

import org.springframework.batch.item.ItemProcessor;

import cl.duoc.bancoxyz.semana3.dtos.CuentaAnualDTO;
import cl.duoc.bancoxyz.semana3.entities.MovimientoAnualEntity;
import cl.duoc.bancoxyz.semana3.exceptions.DatoInconsistenteException;
import cl.duoc.bancoxyz.semana3.util.CamposCsv;
import cl.duoc.bancoxyz.semana3.util.FechaLegacyParser;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class CuentaAnualProcessor implements ItemProcessor<CuentaAnualDTO, MovimientoAnualEntity> {

    private static final Set<String> TIPOS = Set.of("deposito", "retiro", "compra");

    @Override
    public MovimientoAnualEntity process(CuentaAnualDTO dto) {
        String cuentaRaw = CamposCsv.limpio(dto.getCuentaId());
        String fechaRaw = CamposCsv.limpio(dto.getFecha());
        String transaccion = CamposCsv.normalizar(dto.getTransaccion());
        String montoRaw = CamposCsv.limpio(dto.getMonto());
        String descripcion = CamposCsv.limpio(dto.getDescripcion());

        if (CamposCsv.vacio(cuentaRaw) || CamposCsv.vacio(fechaRaw) || CamposCsv.vacio(transaccion)
                || CamposCsv.vacio(montoRaw)) {
            throw new DatoInconsistenteException("Movimiento anual con campos obligatorios vacios (cuenta=" + cuentaRaw + ")");
        }

        if (!TIPOS.contains(transaccion)) {
            throw new DatoInconsistenteException("Tipo de movimiento no valido '" + transaccion + "'");
        }

        LocalDate fecha;
        try {
            fecha = FechaLegacyParser.parsear(fechaRaw);
        } catch (DateTimeParseException ex) {
            throw new DatoInconsistenteException("Fecha anual invalida '" + fechaRaw + "'");
        }

        BigDecimal monto;
        try {
            monto = new BigDecimal(montoRaw);
        } catch (NumberFormatException ex) {
            throw new DatoInconsistenteException("Monto anual no numerico '" + montoRaw + "'");
        }

        if (monto.compareTo(BigDecimal.ZERO) == 0) {
            throw new DatoInconsistenteException("Monto cero omitido en cuenta " + cuentaRaw);
        }

        if ("deposito".equals(transaccion) && monto.compareTo(BigDecimal.ZERO) < 0) {
            throw new DatoInconsistenteException("Deposito negativo omitido en cuenta " + cuentaRaw);
        }

        if (CamposCsv.vacio(descripcion)) {
            descripcion = "Sin descripcion - normalizado en migracion";
        }

        log.debug("[{}] Movimiento anual cuenta={} fecha={} tipo={} monto={}",
                Thread.currentThread().getName(), cuentaRaw, fecha, transaccion, monto);
        return MovimientoAnualEntity.builder()
                .cuentaId(Integer.valueOf(cuentaRaw))
                .fecha(fecha)
                .transaccion(transaccion)
                .monto(monto)
                .descripcion(descripcion)
                .estado("AUDITADO")
                .build();
    }
}
