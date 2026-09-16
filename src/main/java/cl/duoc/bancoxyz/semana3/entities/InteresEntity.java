package cl.duoc.bancoxyz.semana3.entities;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InteresEntity {

    private Integer cuentaId;
    private String nombre;
    private BigDecimal saldoInicial;
    private Integer edad;
    private String tipo;
    private BigDecimal tasaAplicada;
    private BigDecimal interesCalculado;
    private BigDecimal saldoFinal;
    private String estado;
}
