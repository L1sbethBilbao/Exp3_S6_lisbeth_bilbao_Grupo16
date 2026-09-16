package cl.duoc.bancoxyz.semana3.processors;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;

import org.springframework.batch.item.ItemProcessor;

import cl.duoc.bancoxyz.semana3.dtos.TransaccionDTO;
import cl.duoc.bancoxyz.semana3.entities.TransaccionEntity;
import cl.duoc.bancoxyz.semana3.exceptions.DatoInconsistenteException;
import cl.duoc.bancoxyz.semana3.util.CamposCsv;
import cl.duoc.bancoxyz.semana3.util.FechaLegacyParser;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class TransaccionProcessor implements ItemProcessor<TransaccionDTO, TransaccionEntity> {

    @Override
    public TransaccionEntity process(TransaccionDTO dto) {
        String id = CamposCsv.limpio(dto.getId());
        String fechaRaw = CamposCsv.limpio(dto.getFecha());
        String montoRaw = CamposCsv.limpio(dto.getMonto());
        String tipo = CamposCsv.normalizar(dto.getTipo());

        if (CamposCsv.vacio(id) || CamposCsv.vacio(fechaRaw) || CamposCsv.vacio(montoRaw) || CamposCsv.vacio(tipo)) {
            throw new DatoInconsistenteException("Campos obligatorios vacios en transaccion id=" + id);
        }

        LocalDate fecha;
        try {
            fecha = FechaLegacyParser.parsear(fechaRaw);
        } catch (DateTimeParseException ex) {
            throw new DatoInconsistenteException("Fecha invalida '" + fechaRaw + "' en transaccion id=" + id);
        }

        BigDecimal monto;
        try {
            monto = new BigDecimal(montoRaw);
        } catch (NumberFormatException ex) {
            throw new DatoInconsistenteException("Monto no numerico '" + montoRaw + "' en transaccion id=" + id);
        }

        if (!"debito".equals(tipo) && !"credito".equals(tipo)) {
            throw new DatoInconsistenteException("Tipo de transaccion no valido '" + tipo + "' en id=" + id);
        }

        if (monto.compareTo(BigDecimal.ZERO) <= 0) {
            throw new DatoInconsistenteException(
                    "Anomalia de monto (" + monto + ") en transaccion id=" + id + ": se requiere monto > 0");
        }

        int transaccionId;
        try {
            transaccionId = Integer.parseInt(id);
        } catch (NumberFormatException ex) {
            throw new DatoInconsistenteException("Id de transaccion no numerico '" + id + "'");
        }

        log.debug("[{}] Transaccion valida id={} fecha={} monto={} tipo={}",
                Thread.currentThread().getName(), id, fecha, monto, tipo);
        return TransaccionEntity.builder()
                .transaccionId(transaccionId)
                .fecha(fecha)
                .monto(monto)
                .tipo(tipo)
                .estado("VALIDA")
                .observacion("Procesada sin anomalias")
                .build();
    }
}
