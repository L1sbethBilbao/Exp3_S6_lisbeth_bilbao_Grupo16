package cl.duoc.bancoxyz.bff.cajero.dtos;

import java.math.BigDecimal;

public record SaldoCajeroDTO(Long cuentaId, BigDecimal saldo) {
}
