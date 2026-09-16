package cl.duoc.bancoxyz.semana3.services;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.stereotype.Component;

import cl.duoc.bancoxyz.semana3.exceptions.TasaNoDisponibleException;

/**
 * Simula un servicio externo de tasas con falla transitoria, igual que
 * TarifadorService de RutaExpress. La falla es deterministica por cuenta
 * para que el retry funcione con el Step multihilo.
 */
@Component
public class ServicioTasas {

    private static final int FALLA_CADA_N_CUENTAS = 3;

    private final ConcurrentHashMap<Integer, AtomicInteger> intentosPorCuenta = new ConcurrentHashMap<>();

    public void verificarDisponibilidad(int cuentaId) {
        int intento = intentosPorCuenta
                .computeIfAbsent(cuentaId, id -> new AtomicInteger(0))
                .incrementAndGet();

        if (intento == 1 && cuentaId % FALLA_CADA_N_CUENTAS == 0) {
            throw new TasaNoDisponibleException(
                    "Servicio de tasas no disponible para cuenta " + cuentaId + " (intento #" + intento + ")");
        }
    }
}
