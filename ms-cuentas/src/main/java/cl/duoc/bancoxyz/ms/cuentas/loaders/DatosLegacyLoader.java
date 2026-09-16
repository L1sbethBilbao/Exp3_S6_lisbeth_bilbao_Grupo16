package cl.duoc.bancoxyz.ms.cuentas.loaders;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import cl.duoc.bancoxyz.ms.cuentas.entities.CuentaEntity;
import cl.duoc.bancoxyz.ms.cuentas.entities.MovimientoEntity;
import cl.duoc.bancoxyz.ms.cuentas.entities.TransaccionEntity;
import cl.duoc.bancoxyz.ms.cuentas.repositories.CuentaRepository;
import cl.duoc.bancoxyz.ms.cuentas.repositories.MovimientoRepository;
import cl.duoc.bancoxyz.ms.cuentas.repositories.TransaccionRepository;
import cl.duoc.bancoxyz.ms.cuentas.util.CamposCsv;
import cl.duoc.bancoxyz.ms.cuentas.util.FechaLegacyParser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Carga los CSV oficiales de bank_legacy_data (semana_3) y descarta filas sucias
 * para que el backend interno entregue datos consistentes a los BFF.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DatosLegacyLoader implements CommandLineRunner {

    private static final Set<String> TIPOS_CUENTA = Set.of("ahorro", "prestamo");
    private static final Set<String> TIPOS_MOVIMIENTO = Set.of("deposito", "retiro", "compra");
    private static final Set<String> TIPOS_TRANSACCION = Set.of("debito", "credito");

    private final CuentaRepository cuentaRepository;
    private final MovimientoRepository movimientoRepository;
    private final TransaccionRepository transaccionRepository;

    @Override
    public void run(String... args) throws Exception {
        if (cuentaRepository.count() > 0) {
            log.info(">> Backend interno ya persistido en Oracle Autonomous: {} cuentas, {} movimientos, {} transacciones",
                    cuentaRepository.count(), movimientoRepository.count(), transaccionRepository.count());
            return;
        }
        int cuentas = cargarCuentas();
        int movimientos = cargarMovimientos();
        int transacciones = cargarTransacciones();
        log.info(">> Backend interno persistido en Oracle Autonomous desde CSV semana_3: {} cuentas, {} movimientos, {} transacciones",
                cuentas, movimientos, transacciones);
    }

    private int cargarCuentas() throws Exception {
        List<CuentaEntity> validas = new ArrayList<>();
        try (BufferedReader reader = abrir("data/intereses.csv")) {
            reader.readLine();
            String linea;
            while ((linea = reader.readLine()) != null) {
                if (linea.isBlank()) {
                    continue;
                }
                String[] c = linea.split(",", -1);
                if (c.length < 5) {
                    continue;
                }
                String id = CamposCsv.limpio(c[0]);
                String nombre = CamposCsv.limpio(c[1]);
                String saldoRaw = CamposCsv.limpio(c[2]);
                String edadRaw = CamposCsv.limpio(c[3]);
                String tipo = CamposCsv.normalizar(c[4]);
                if (CamposCsv.vacio(id) || CamposCsv.vacio(nombre) || CamposCsv.vacio(saldoRaw)
                        || CamposCsv.vacio(edadRaw) || "unknown".equals(CamposCsv.normalizar(nombre))) {
                    continue;
                }
                if (!TIPOS_CUENTA.contains(tipo)) {
                    continue;
                }
                try {
                    int edad = Integer.parseInt(edadRaw);
                    BigDecimal saldo = new BigDecimal(saldoRaw);
                    if (edad < 18 || edad > 90 || saldo.compareTo(BigDecimal.ZERO) <= 0) {
                        continue;
                    }
                    Long cuentaId = Long.parseLong(id);
                    if (validas.stream().anyMatch(x -> x.getCuentaId().equals(cuentaId))) {
                        continue;
                    }
                    validas.add(new CuentaEntity(cuentaId, nombre, saldo, edad, tipo));
                } catch (NumberFormatException ignored) {
                    // fila sucia
                }
            }
        }
        cuentaRepository.saveAll(validas);
        return validas.size();
    }

    private int cargarMovimientos() throws Exception {
        List<MovimientoEntity> validos = new ArrayList<>();
        try (BufferedReader reader = abrir("data/cuentas_anuales.csv")) {
            reader.readLine();
            String linea;
            while ((linea = reader.readLine()) != null) {
                if (linea.isBlank()) {
                    continue;
                }
                String[] c = linea.split(",", -1);
                if (c.length < 5) {
                    continue;
                }
                String id = CamposCsv.limpio(c[0]);
                String fechaRaw = CamposCsv.limpio(c[1]);
                String tipo = CamposCsv.normalizar(c[2]);
                String montoRaw = CamposCsv.limpio(c[3]);
                String descripcion = CamposCsv.limpio(c[4]);
                if (CamposCsv.vacio(id) || CamposCsv.vacio(fechaRaw) || CamposCsv.vacio(montoRaw)
                        || !TIPOS_MOVIMIENTO.contains(tipo)) {
                    continue;
                }
                try {
                    Long cuentaId = Long.parseLong(id);
                    if (!cuentaRepository.existsById(cuentaId)) {
                        continue;
                    }
                    BigDecimal monto = new BigDecimal(montoRaw);
                    if (monto.compareTo(BigDecimal.ZERO) == 0) {
                        continue;
                    }
                    if ("deposito".equals(tipo) && monto.compareTo(BigDecimal.ZERO) < 0) {
                        continue;
                    }
                    if (CamposCsv.vacio(descripcion)) {
                        descripcion = "Sin descripcion";
                    }
                    validos.add(new MovimientoEntity(
                            null, cuentaId, FechaLegacyParser.parsear(fechaRaw), tipo, monto.abs(), descripcion));
                } catch (NumberFormatException | DateTimeParseException ignored) {
                    // fila sucia
                }
            }
        }
        movimientoRepository.saveAll(validos);
        return validos.size();
    }

    private int cargarTransacciones() throws Exception {
        List<TransaccionEntity> validas = new ArrayList<>();
        try (BufferedReader reader = abrir("data/transacciones.csv")) {
            reader.readLine();
            String linea;
            while ((linea = reader.readLine()) != null) {
                if (linea.isBlank()) {
                    continue;
                }
                String[] c = linea.split(",", -1);
                if (c.length < 4) {
                    continue;
                }
                String id = CamposCsv.limpio(c[0]);
                String fechaRaw = CamposCsv.limpio(c[1]);
                String montoRaw = CamposCsv.limpio(c[2]);
                String tipo = CamposCsv.normalizar(c[3]);
                if (CamposCsv.vacio(id) || CamposCsv.vacio(fechaRaw) || CamposCsv.vacio(montoRaw)
                        || !TIPOS_TRANSACCION.contains(tipo)) {
                    continue;
                }
                try {
                    BigDecimal monto = new BigDecimal(montoRaw);
                    if (monto.compareTo(BigDecimal.ZERO) <= 0) {
                        continue;
                    }
                    Long txId = Long.parseLong(id);
                    if (validas.stream().anyMatch(x -> x.getTransaccionId().equals(txId))) {
                        continue;
                    }
                    validas.add(new TransaccionEntity(txId, FechaLegacyParser.parsear(fechaRaw), monto, tipo));
                } catch (NumberFormatException | DateTimeParseException ignored) {
                    // fila sucia
                }
            }
        }
        transaccionRepository.saveAll(validas);
        return validas.size();
    }

    private BufferedReader abrir(String classpath) throws Exception {
        return new BufferedReader(new InputStreamReader(
                new ClassPathResource(classpath).getInputStream(), StandardCharsets.UTF_8));
    }
}
