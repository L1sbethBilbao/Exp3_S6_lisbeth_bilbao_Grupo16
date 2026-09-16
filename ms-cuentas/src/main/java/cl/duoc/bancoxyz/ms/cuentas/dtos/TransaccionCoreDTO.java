package cl.duoc.bancoxyz.ms.cuentas.dtos;

import java.math.BigDecimal;
import java.time.LocalDate;

public record TransaccionCoreDTO(
        Long transaccionId,
        LocalDate fecha,
        BigDecimal monto,
        String tipo) {
}
