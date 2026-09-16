package cl.duoc.bancoxyz.semana3.dtos;

import lombok.Data;

/**
 * DTO de entrada: una linea de transacciones.csv, tal como llega del ItemReader.
 */
@Data
public class TransaccionDTO {

    private String id;
    private String fecha;
    private String monto;
    private String tipo;
}
