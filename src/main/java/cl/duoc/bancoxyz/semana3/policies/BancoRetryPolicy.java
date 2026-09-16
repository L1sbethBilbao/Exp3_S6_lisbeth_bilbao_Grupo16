package cl.duoc.bancoxyz.semana3.policies;

import java.util.Map;

import org.springframework.dao.TransientDataAccessException;
import org.springframework.retry.RetryContext;
import org.springframework.retry.policy.SimpleRetryPolicy;

import cl.duoc.bancoxyz.semana3.exceptions.TasaNoDisponibleException;
import lombok.extern.slf4j.Slf4j;

/**
 * RetryPolicy personalizada de la Semana 2: reintenta fallas transitorias
 * (servicio de tasas o BD) y deja trazabilidad de cada intento en el log.
 */
@Slf4j
public class BancoRetryPolicy extends SimpleRetryPolicy {

    public BancoRetryPolicy(int maxAttempts) {
        super(maxAttempts, Map.of(
                TasaNoDisponibleException.class, true,
                TransientDataAccessException.class, true));
    }

    @Override
    public boolean canRetry(RetryContext context) {
        boolean puedeReintentar = super.canRetry(context);
        if (puedeReintentar && context.getLastThrowable() != null) {
            log.warn("Reintento {}/{} tras falla transitoria: {}",
                    context.getRetryCount(), getMaxAttempts(), context.getLastThrowable().getMessage());
        }
        return puedeReintentar;
    }
}
