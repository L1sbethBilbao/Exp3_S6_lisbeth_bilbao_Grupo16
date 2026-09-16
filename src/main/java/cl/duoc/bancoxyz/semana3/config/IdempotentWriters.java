package cl.duoc.bancoxyz.semana3.config;

import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDate;

import javax.sql.DataSource;

import org.springframework.batch.item.database.JdbcBatchItemWriter;
import org.springframework.batch.item.database.builder.JdbcBatchItemWriterBuilder;

import cl.duoc.bancoxyz.semana3.entities.InteresEntity;
import cl.duoc.bancoxyz.semana3.entities.MovimientoAnualEntity;
import cl.duoc.bancoxyz.semana3.entities.TransaccionEntity;

/**
 * Writers MERGE / UPSERT: relanzar o reanudar un Job no duplica filas.
 */
public final class IdempotentWriters {

    private IdempotentWriters() {
    }

    public static JdbcBatchItemWriter<TransaccionEntity> transacciones(DataSource dataSource, String platform) {
        return new JdbcBatchItemWriterBuilder<TransaccionEntity>()
                .dataSource(dataSource)
                .sql(sqlTransaccion(platform))
                .itemPreparedStatementSetter(IdempotentWriters::bindTransaccion)
                .assertUpdates(false)
                .build();
    }

    public static JdbcBatchItemWriter<InteresEntity> intereses(DataSource dataSource, String platform) {
        return new JdbcBatchItemWriterBuilder<InteresEntity>()
                .dataSource(dataSource)
                .sql(sqlInteres(platform))
                .itemPreparedStatementSetter(IdempotentWriters::bindInteres)
                .assertUpdates(false)
                .build();
    }

    public static JdbcBatchItemWriter<MovimientoAnualEntity> movimientos(DataSource dataSource, String platform) {
        return new JdbcBatchItemWriterBuilder<MovimientoAnualEntity>()
                .dataSource(dataSource)
                .sql(sqlMovimiento(platform))
                .itemPreparedStatementSetter(IdempotentWriters::bindMovimiento)
                .assertUpdates(false)
                .build();
    }

    private static void bindTransaccion(TransaccionEntity item, PreparedStatement ps) throws SQLException {
        setInt(ps, 1, item.getTransaccionId());
        setDate(ps, 2, item.getFecha());
        ps.setBigDecimal(3, item.getMonto());
        ps.setString(4, item.getTipo());
        ps.setString(5, item.getEstado());
        ps.setString(6, item.getObservacion());
    }

    private static void bindInteres(InteresEntity item, PreparedStatement ps) throws SQLException {
        setInt(ps, 1, item.getCuentaId());
        ps.setString(2, item.getNombre());
        ps.setBigDecimal(3, item.getSaldoInicial());
        setInt(ps, 4, item.getEdad());
        ps.setString(5, item.getTipo());
        ps.setBigDecimal(6, item.getTasaAplicada());
        ps.setBigDecimal(7, item.getInteresCalculado());
        ps.setBigDecimal(8, item.getSaldoFinal());
        ps.setString(9, item.getEstado());
    }

    private static void bindMovimiento(MovimientoAnualEntity item, PreparedStatement ps) throws SQLException {
        setInt(ps, 1, item.getCuentaId());
        setDate(ps, 2, item.getFecha());
        ps.setString(3, item.getTransaccion());
        ps.setBigDecimal(4, item.getMonto());
        ps.setString(5, item.getDescripcion());
        ps.setString(6, item.getEstado());
    }

    private static void setInt(PreparedStatement ps, int index, Integer value) throws SQLException {
        if (value == null) {
            ps.setNull(index, java.sql.Types.INTEGER);
        } else {
            ps.setInt(index, value);
        }
    }

    private static void setDate(PreparedStatement ps, int index, LocalDate value) throws SQLException {
        if (value == null) {
            ps.setNull(index, java.sql.Types.DATE);
        } else {
            ps.setDate(index, Date.valueOf(value));
        }
    }

    private static String sqlTransaccion(String platform) {
        if (esH2(platform)) {
            return "MERGE INTO transaccion_procesada (transaccion_id, fecha, monto, tipo, estado, observacion) "
                    + "KEY (transaccion_id) VALUES (?, ?, ?, ?, ?, ?)";
        }
        if (esPostgres(platform)) {
            return "INSERT INTO transaccion_procesada (transaccion_id, fecha, monto, tipo, estado, observacion) "
                    + "VALUES (?, ?, ?, ?, ?, ?) "
                    + "ON CONFLICT (transaccion_id) DO UPDATE SET fecha = EXCLUDED.fecha, monto = EXCLUDED.monto, "
                    + "tipo = EXCLUDED.tipo, estado = EXCLUDED.estado, observacion = EXCLUDED.observacion";
        }
        return "MERGE INTO transaccion_procesada dest "
                + "USING (SELECT ? AS transaccion_id, ? AS fecha, ? AS monto, ? AS tipo, "
                + "? AS estado, ? AS observacion FROM dual) src "
                + "ON (dest.transaccion_id = src.transaccion_id) "
                + "WHEN MATCHED THEN UPDATE SET dest.fecha = src.fecha, dest.monto = src.monto, "
                + "dest.tipo = src.tipo, dest.estado = src.estado, dest.observacion = src.observacion "
                + "WHEN NOT MATCHED THEN INSERT (transaccion_id, fecha, monto, tipo, estado, observacion) "
                + "VALUES (src.transaccion_id, src.fecha, src.monto, src.tipo, src.estado, src.observacion)";
    }

    private static String sqlInteres(String platform) {
        if (esH2(platform)) {
            return "MERGE INTO interes_procesado (cuenta_id, nombre, saldo_inicial, edad, tipo, "
                    + "tasa_aplicada, interes_calculado, saldo_final, estado) "
                    + "KEY (cuenta_id) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        }
        if (esPostgres(platform)) {
            return "INSERT INTO interes_procesado (cuenta_id, nombre, saldo_inicial, edad, tipo, "
                    + "tasa_aplicada, interes_calculado, saldo_final, estado) "
                    + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?) "
                    + "ON CONFLICT (cuenta_id) DO UPDATE SET nombre = EXCLUDED.nombre, "
                    + "saldo_inicial = EXCLUDED.saldo_inicial, edad = EXCLUDED.edad, tipo = EXCLUDED.tipo, "
                    + "tasa_aplicada = EXCLUDED.tasa_aplicada, interes_calculado = EXCLUDED.interes_calculado, "
                    + "saldo_final = EXCLUDED.saldo_final, estado = EXCLUDED.estado";
        }
        return "MERGE INTO interes_procesado dest "
                + "USING (SELECT ? AS cuenta_id, ? AS nombre, ? AS saldo_inicial, ? AS edad, ? AS tipo, "
                + "? AS tasa_aplicada, ? AS interes_calculado, ? AS saldo_final, ? AS estado FROM dual) src "
                + "ON (dest.cuenta_id = src.cuenta_id) "
                + "WHEN MATCHED THEN UPDATE SET dest.nombre = src.nombre, dest.saldo_inicial = src.saldo_inicial, "
                + "dest.edad = src.edad, dest.tipo = src.tipo, dest.tasa_aplicada = src.tasa_aplicada, "
                + "dest.interes_calculado = src.interes_calculado, dest.saldo_final = src.saldo_final, "
                + "dest.estado = src.estado "
                + "WHEN NOT MATCHED THEN INSERT (cuenta_id, nombre, saldo_inicial, edad, tipo, "
                + "tasa_aplicada, interes_calculado, saldo_final, estado) "
                + "VALUES (src.cuenta_id, src.nombre, src.saldo_inicial, src.edad, src.tipo, "
                + "src.tasa_aplicada, src.interes_calculado, src.saldo_final, src.estado)";
    }

    private static String sqlMovimiento(String platform) {
        if (esH2(platform)) {
            return "MERGE INTO movimiento_anual (cuenta_id, fecha, transaccion, monto, descripcion, estado) "
                    + "KEY (cuenta_id, fecha, transaccion, monto, descripcion) VALUES (?, ?, ?, ?, ?, ?)";
        }
        if (esPostgres(platform)) {
            return "INSERT INTO movimiento_anual (cuenta_id, fecha, transaccion, monto, descripcion, estado) "
                    + "VALUES (?, ?, ?, ?, ?, ?) "
                    + "ON CONFLICT (cuenta_id, fecha, transaccion, monto, descripcion) DO UPDATE SET "
                    + "estado = EXCLUDED.estado";
        }
        return "MERGE INTO movimiento_anual dest "
                + "USING (SELECT ? AS cuenta_id, ? AS fecha, ? AS transaccion, ? AS monto, "
                + "? AS descripcion, ? AS estado FROM dual) src "
                + "ON (dest.cuenta_id = src.cuenta_id AND dest.fecha = src.fecha "
                + "AND dest.transaccion = src.transaccion AND dest.monto = src.monto "
                + "AND dest.descripcion = src.descripcion) "
                + "WHEN MATCHED THEN UPDATE SET dest.estado = src.estado "
                + "WHEN NOT MATCHED THEN INSERT (cuenta_id, fecha, transaccion, monto, descripcion, estado) "
                + "VALUES (src.cuenta_id, src.fecha, src.transaccion, src.monto, src.descripcion, src.estado)";
    }

    private static boolean esH2(String platform) {
        return platform != null && platform.equalsIgnoreCase("h2");
    }

    private static boolean esPostgres(String platform) {
        return platform != null && platform.toLowerCase().startsWith("postgres");
    }
}
