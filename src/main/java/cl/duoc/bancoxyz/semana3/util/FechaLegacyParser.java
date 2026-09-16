package cl.duoc.bancoxyz.semana3.util;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;

/**
 * Normaliza fechas del sistema legacy. El CSV del banco mezcla
 * yyyy-MM-dd (esperado) con yyyy/MM/dd y otros formatos de archivo plano.
 */
public final class FechaLegacyParser {

    private static final List<DateTimeFormatter> FORMATOS = List.of(
            DateTimeFormatter.ofPattern("yyyy-MM-dd"),
            DateTimeFormatter.ofPattern("yyyy/MM/dd"),
            DateTimeFormatter.ofPattern("dd-MM-yyyy"),
            DateTimeFormatter.ofPattern("dd/MM/yyyy")
    );

    private FechaLegacyParser() {
    }

    public static LocalDate parsear(String valor) {
        if (valor == null || valor.isBlank()) {
            throw new DateTimeParseException("Fecha vacia", "", 0);
        }
        String limpio = valor.trim();
        for (DateTimeFormatter formato : FORMATOS) {
            try {
                return LocalDate.parse(limpio, formato);
            } catch (DateTimeParseException ignored) {
                // probar el siguiente formato
            }
        }
        throw new DateTimeParseException("Formato de fecha no soportado: " + limpio, limpio, 0);
    }
}
