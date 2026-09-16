package cl.duoc.bancoxyz.semana3.exceptions;

import lombok.experimental.StandardException;

/**
 * Falla transitoria al consultar el servicio de tasas (equivalente a
 * TarifadorNoDisponibleException de RutaExpress). Activa la RetryPolicy.
 */
@StandardException
public class TasaNoDisponibleException extends RuntimeException {
}
