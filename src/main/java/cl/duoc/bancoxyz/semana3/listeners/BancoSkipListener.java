package cl.duoc.bancoxyz.semana3.listeners;

import org.springframework.batch.core.SkipListener;

import lombok.extern.slf4j.Slf4j;

/**
 * Registra cada registro omitido por la SkipPolicy (lectura, proceso o escritura).
 */
@Slf4j
public class BancoSkipListener implements SkipListener<Object, Object> {

    @Override
    public void onSkipInRead(Throwable t) {
        log.warn("SKIP en lectura: {}", t.getMessage());
    }

    @Override
    public void onSkipInProcess(Object item, Throwable t) {
        log.warn("SKIP en procesamiento. Item={} Motivo={}", item, t.getMessage());
    }

    @Override
    public void onSkipInWrite(Object item, Throwable t) {
        log.warn("SKIP en escritura. Item={} Motivo={}", item, t.getMessage());
    }
}
