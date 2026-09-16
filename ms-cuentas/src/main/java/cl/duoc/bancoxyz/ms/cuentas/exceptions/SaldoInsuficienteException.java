package cl.duoc.bancoxyz.ms.cuentas.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class SaldoInsuficienteException extends RuntimeException {

    public SaldoInsuficienteException(Long cuentaId) {
        super("Saldo insuficiente en la cuenta " + cuentaId);
    }
}
