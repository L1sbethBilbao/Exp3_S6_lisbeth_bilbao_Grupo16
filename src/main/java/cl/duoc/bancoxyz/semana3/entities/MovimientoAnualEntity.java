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
public class MovimientoAnualEntity {

    private Integer cuentaId;
    private LocalDate fecha;
    private String transaccion;
    private BigDecimal monto;
    private String descripcion;
    private String estado;
}
