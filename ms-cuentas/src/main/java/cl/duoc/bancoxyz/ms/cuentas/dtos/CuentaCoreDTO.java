package cl.duoc.bancoxyz.ms.cuentas.dtos;

import java.math.BigDecimal;

public record CuentaCoreDTO(
        Long cuentaId,
        String nombre,
        BigDecimal saldo,
        Integer edad,
        String tipo) {
}
