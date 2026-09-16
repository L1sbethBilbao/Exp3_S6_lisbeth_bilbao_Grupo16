package cl.duoc.bancoxyz.semana3.policies;

import org.springframework.batch.core.step.skip.SkipLimitExceededException;
import org.springframework.batch.core.step.skip.SkipPolicy;
import org.springframework.batch.item.file.FlatFileParseException;
import org.springframework.dao.DataIntegrityViolationException;

import cl.duoc.bancoxyz.semana3.exceptions.DatoInconsistenteException;

/**
 * SkipPolicy (equivalente a EnvioSkipPolicy de RutaExpress).
 * Omite registros sucios del CSV legacy sin detener el Job.
 * El limite es por particion (worker Step), no por Job: los CSV de
 * semana_3 tienen ~1000 filas sucias, por eso el tope es holgado.
 */
public class BancoSkipPolicy implements SkipPolicy {

    private static final int LIMITE_SKIPS = 2000;

    @Override
    public boolean shouldSkip(Throwable throwable, long skipCount) throws SkipLimitExceededException {
        if (skipCount >= LIMITE_SKIPS) {
            return false;
        }
        return throwable instanceof DatoInconsistenteException
                || throwable instanceof FlatFileParseException
                || throwable instanceof DataIntegrityViolationException;
    }
}
