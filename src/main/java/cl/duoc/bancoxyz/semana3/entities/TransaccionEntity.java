package cl.duoc.bancoxyz.semana3.entities;

import java.math.BigDecimal;
import java.time.LocalDate;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransaccionEntity {

    private Integer transaccionId;
    private LocalDate fecha;
    private BigDecimal monto;
    private String tipo;
    private String estado;
    private String observacion;
}
