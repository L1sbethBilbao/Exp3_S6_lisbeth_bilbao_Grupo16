package cl.duoc.bancoxyz.bff.cajero.dtos;

import java.math.BigDecimal;

public record MovimientoCajeroDTO(String fecha, String tipo, BigDecimal monto) {
}
