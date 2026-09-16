package cl.duoc.bancoxyz.semana3.dtos;

import lombok.Data;

/**
 * DTO de entrada: una linea de cuentas_anuales.csv.
 */
@Data
public class CuentaAnualDTO {

    private String cuentaId;
    private String fecha;
    private String transaccion;
    private String monto;
    private String descripcion;
}
