package cl.duoc.bancoxyz.ms.cuentas.entities;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "movimiento_banco")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MovimientoEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "movimiento_banco_seq")
    @SequenceGenerator(name = "movimiento_banco_seq", sequenceName = "movimiento_banco_seq", allocationSize = 1)
    private Long id;
    private Long cuentaId;
    private LocalDate fecha;
    private String tipo;
    private BigDecimal monto;
    private String descripcion;
}
