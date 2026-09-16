package cl.duoc.bancoxyz.semana3.util;

import java.text.Normalizer;
import java.util.Locale;

public final class CamposCsv {

    private CamposCsv() {
    }

    public static String limpio(String valor) {
        return valor == null ? "" : valor.trim();
    }

    public static boolean vacio(String valor) {
        return valor == null || valor.trim().isEmpty();
    }

    /** Minusculas sin tildes: {@code depósito} -> {@code deposito}. */
    public static String normalizar(String valor) {
        String limpio = limpio(valor).toLowerCase(Locale.ROOT);
        String nfd = Normalizer.normalize(limpio, Normalizer.Form.NFD);
        return nfd.replaceAll("\\p{M}+", "");
    }
}
