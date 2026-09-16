package cl.duoc.bancoxyz.bff.web.dtos;

import java.math.BigDecimal;
import java.time.LocalDate;

public record MovimientoWebDTO(LocalDate fecha, String tipo, BigDecimal monto, String descripcion) {
}
