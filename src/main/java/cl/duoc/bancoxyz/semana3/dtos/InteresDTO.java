package cl.duoc.bancoxyz.semana3.dtos;

import lombok.Data;

/**
 * DTO de entrada: una linea de intereses.csv, antes de validar y calcular.
 */
@Data
public class InteresDTO {

    private String cuentaId;
    private String nombre;
    private String saldo;
    private String edad;
    private String tipo;
}
