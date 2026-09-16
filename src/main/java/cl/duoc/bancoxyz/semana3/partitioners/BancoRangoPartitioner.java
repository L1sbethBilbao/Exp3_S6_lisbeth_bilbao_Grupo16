package cl.duoc.bancoxyz.semana3.partitioners;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.batch.core.partition.support.Partitioner;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.core.io.Resource;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Partitioner de la Semana 3 (estilo RutaExpress / PDF E1S3).
 * Divide un CSV oficial en rangos de lineas de datos (sin header) para
 * que cada worker reciba un subconjunto independiente.
 */
@Slf4j
@RequiredArgsConstructor
public class BancoRangoPartitioner implements Partitioner {

    private final Resource archivoCsv;
    private final String nombreParticion;

    @Override
    public Map<String, ExecutionContext> partition(int gridSize) {
        int totalDatos = contarLineasDatos();
        int tamano = (int) Math.ceil(totalDatos / (double) Math.max(gridSize, 1));
        Map<String, ExecutionContext> partitions = new LinkedHashMap<>();

        int start = 0;
        for (int i = 0; i < gridSize && start < totalDatos; i++) {
            int end = Math.min(start + tamano - 1, totalDatos - 1);
            ExecutionContext context = new ExecutionContext();
            context.putInt("start", start);
            context.putInt("end", end);
            context.putString("partitionName", nombreParticion + i);
            partitions.put("partition" + i, context);
            log.info("Particion {} [{}] lineas de datos {}-{} (total={})",
                    nombreParticion + i, archivoCsv.getFilename(), start, end, totalDatos);
            start = end + 1;
        }
        return partitions;
    }

    private int contarLineasDatos() {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(archivoCsv.getInputStream(), StandardCharsets.UTF_8))) {
            int n = 0;
            boolean header = true;
            String linea;
            while ((linea = reader.readLine()) != null) {
                if (header) {
                    header = false;
                    continue;
                }
                if (!linea.isBlank()) {
                    n++;
                }
            }
            return n;
        } catch (Exception ex) {
            throw new IllegalStateException("No se pudo contar lineas de " + archivoCsv, ex);
        }
    }
}
