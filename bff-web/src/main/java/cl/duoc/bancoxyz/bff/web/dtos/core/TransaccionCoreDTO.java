package cl.duoc.bancoxyz.bff.web.dtos.core;

import java.math.BigDecimal;
import java.time.LocalDate;

public record TransaccionCoreDTO(Long transaccionId, LocalDate fecha, BigDecimal monto, String tipo) {
}
