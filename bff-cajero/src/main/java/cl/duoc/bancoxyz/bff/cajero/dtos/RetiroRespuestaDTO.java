package cl.duoc.bancoxyz.bff.cajero.dtos;

import java.math.BigDecimal;

public record RetiroRespuestaDTO(Long cuentaId, BigDecimal montoRetirado, BigDecimal saldoDisponible) {
}
