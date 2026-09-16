package cl.duoc.bancoxyz.bff.cajero.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class OperacionRechazadaException extends RuntimeException {

    public OperacionRechazadaException(String mensaje) {
        super(mensaje);
    }
}
