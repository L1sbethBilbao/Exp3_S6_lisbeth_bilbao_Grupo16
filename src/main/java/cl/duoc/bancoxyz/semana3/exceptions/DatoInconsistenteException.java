package cl.duoc.bancoxyz.semana3.exceptions;

/**
 * Dato inconsistente del sistema legacy: se omite el registro (SkipPolicy)
 * y el Job continua con el resto del lote.
 */
public class DatoInconsistenteException extends RuntimeException {

    public DatoInconsistenteException(String message) {
        super(message);
    }
}
