package cl.duoc.bancoxyz.ms.cuentas.util;

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

    public static String normalizar(String valor) {
        String limpio = limpio(valor).toLowerCase(Locale.ROOT);
        String nfd = Normalizer.normalize(limpio, Normalizer.Form.NFD);
        return nfd.replaceAll("\\p{M}+", "");
    }
}
