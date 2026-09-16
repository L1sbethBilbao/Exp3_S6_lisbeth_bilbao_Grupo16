package cl.duoc.bancoxyz.ms.cuentas.entities;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "transaccion_banco")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TransaccionEntity {

    @Id
    private Long transaccionId;
    private LocalDate fecha;
    private BigDecimal monto;
    private String tipo;
}
