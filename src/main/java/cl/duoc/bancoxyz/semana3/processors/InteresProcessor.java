package cl.duoc.bancoxyz.semana3.processors;

import java.math.BigDecimal;
import java.math.RoundingMode;

import org.springframework.batch.item.ItemProcessor;

import cl.duoc.bancoxyz.semana3.dtos.InteresDTO;
import cl.duoc.bancoxyz.semana3.entities.InteresEntity;
import cl.duoc.bancoxyz.semana3.exceptions.DatoInconsistenteException;
import cl.duoc.bancoxyz.semana3.services.ServicioTasas;
import cl.duoc.bancoxyz.semana3.util.CamposCsv;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Calcula intereses mensuales. Llama a ServicioTasas para ejercitar RetryPolicy.
 */
@Slf4j
@RequiredArgsConstructor
public class InteresProcessor implements ItemProcessor<InteresDTO, InteresEntity> {

    private static final BigDecimal TASA_AHORRO = new BigDecimal("0.0040");
    private static final BigDecimal TASA_PRESTAMO = new BigDecimal("0.0120");
    private static final int EDAD_MINIMA = 18;
    private static final int EDAD_MAXIMA = 90;

    private final ServicioTasas servicioTasas;

    @Override
    public InteresEntity process(InteresDTO dto) {
        String cuentaRaw = CamposCsv.limpio(dto.getCuentaId());
        String nombre = CamposCsv.limpio(dto.getNombre());
        String saldoRaw = CamposCsv.limpio(dto.getSaldo());
        String edadRaw = CamposCsv.limpio(dto.getEdad());
        String tipo = CamposCsv.normalizar(dto.getTipo());

        if (CamposCsv.vacio(cuentaRaw) || CamposCsv.vacio(nombre) || CamposCsv.vacio(saldoRaw)
                || CamposCsv.vacio(edadRaw) || CamposCsv.vacio(tipo)) {
            throw new DatoInconsistenteException("Campos vacios en cuenta " + cuentaRaw);
        }

        if ("unknown".equalsIgnoreCase(nombre)) {
            throw new DatoInconsistenteException("Nombre Unknown omitido en cuenta " + cuentaRaw);
        }

        int cuentaId;
        int edad;
        BigDecimal saldoInicial;
        try {
            cuentaId = Integer.parseInt(cuentaRaw);
            edad = Integer.parseInt(edadRaw);
            saldoInicial = new BigDecimal(saldoRaw);
        } catch (NumberFormatException ex) {
            throw new DatoInconsistenteException("Valores numericos invalidos en cuenta " + cuentaRaw);
        }

        if (edad < EDAD_MINIMA || edad > EDAD_MAXIMA) {
            throw new DatoInconsistenteException("Edad fuera de rango (" + edad + ") en cuenta " + cuentaId);
        }

        if (saldoInicial.compareTo(BigDecimal.ZERO) <= 0) {
            throw new DatoInconsistenteException("Saldo vacio o no positivo (" + saldoInicial + ") en cuenta " + cuentaId);
        }

        if (!"ahorro".equals(tipo) && !"prestamo".equals(tipo)) {
            throw new DatoInconsistenteException("Tipo de cuenta no elegible '" + tipo + "' en cuenta " + cuentaId);
        }

        servicioTasas.verificarDisponibilidad(cuentaId);

        BigDecimal tasa = "ahorro".equals(tipo) ? TASA_AHORRO : TASA_PRESTAMO;
        BigDecimal interes = saldoInicial.multiply(tasa).setScale(2, RoundingMode.HALF_UP);
        BigDecimal saldoFinal = saldoInicial.add(interes).setScale(2, RoundingMode.HALF_UP);

        log.debug("[{}] Interes cuenta={} tipo={} tasa={} interes={} saldoFinal={}",
                Thread.currentThread().getName(), cuentaId, tipo, tasa, interes, saldoFinal);

        return InteresEntity.builder()
                .cuentaId(cuentaId)
                .nombre(nombre)
                .saldoInicial(saldoInicial)
                .edad(edad)
                .tipo(tipo)
                .tasaAplicada(tasa)
                .interesCalculado(interes)
                .saldoFinal(saldoFinal)
                .estado("ACTUALIZADO")
                .build();
    }
}
