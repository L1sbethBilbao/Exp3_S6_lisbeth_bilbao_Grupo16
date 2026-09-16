# Banco XYZ - Migracion de procesos batch (Semana 3)

Continuidad de las semanas 1 y 2. Los tres procesos legacy del Banco XYZ se
ejecutan con **Spring Batch 5** y **particiones** (estilo de clase RutaExpress
+ PDF E1S3): un Step maestro reparte rangos de lineas del CSV oficial y un
pool de hilos ejecuta los workers en paralelo.

## Objetivo

1. **Reporte de transacciones diarias** (`transaccionesDiariasJob`)
2. **Calculo de intereses mensuales** (`interesesMensualesJob`)
3. **Generacion de estados de cuenta anuales** (`estadosCuentaAnualesJob`)

Datos de entrada: [bank_legacy_data](https://github.com/KariVillagran/bank_legacy_data)
carpeta **`data/semana_3`** (~1000 filas sucias por archivo).

## Por que particiones (y no multithread de chunks)

En la semana 2 el Step usaba `.taskExecutor(...)` y un `SynchronizedItemStreamReader`
porque el `FlatFileItemReader` no es thread-safe. Eso paraleliza el **chunk**,
pero obliga a serializar la lectura.

En la semana 3 cada particion tiene **su propio reader `@StepScope`** con un
rango `start`/`end` independiente. El `TaskExecutor` vive en el
`PartitionHandler`, no en el worker. No hace falta sincronizar el CSV.

Cada Job tiene **un** CSV oficial, por eso el partitioner divide **rangos de
lineas** (como el PDF de 1–1000 / 1001–2000), no un archivo por zona.

## Version

- Java 17
- Spring Boot 3.3.5 (Spring Batch 5.1.x)
- Base de datos de esta entrega: **Oracle Autonomous** (wallet `Wallet_miQuintaBD`, fuera del repo)
- Alternativa documentada: **H2** (comentar Oracle y descomentar H2)

## Escalado: parametros elegidos y comparacion

| Parametro | Semana 2 (multithread) | Semana 3 (particiones, elegido) | Alternativa evaluada |
| --- | --- | --- | --- |
| Estrategia | 3 hilos en el Step + reader sincronizado | `TaskExecutorPartitionHandler` | Mismo handler, otro `gridSize` |
| Paralelismo | `core/max = 3`, cola 20 | `gridSize = 3`, `core/max = 3`, cola 10 | `gridSize = 2`, pool 3 |
| Unidad de trabajo | Chunks de 5 sobre el CSV completo | ~334 lineas por particion (~1000 / 3) | ~500 lineas por particion |
| Reader | Un reader compartido (lock) | Un reader por particion (`@StepScope`) | Igual |
| Skip | Limite 50 (archivo chico) | Limite **2000 por worker** | Igual |
| Retry | 3 intentos | 3 intentos + `ExponentialBackOffPolicy` | Igual |

**Por que `gridSize = 3` y pool 3:** las tres particiones arrancan a la vez
(`banco-particion-*`) y no quedan workers en cola. Con `gridSize = 2` el
tercer hilo del pool queda ocioso y cada worker lee ~500 filas (peor
latencia de fin de Job). Con `gridSize = 5` y pool 3, dos particiones
esperan en la cola de 10: mas overhead de Steps de Spring Batch para el
mismo CSV. Chunks de 5 se mantienen (politica de la semana 2) para commits
cortos y skip/retry visibles.

Los tasklets de resumen e informe anual **no** se particionan: corren
despues del master, sobre lo ya escrito en Oracle.

## Que se implementa en la semana 3

| Concepto | Implementacion |
| --- | --- |
| Partitioner | `BancoRangoPartitioner`: cuenta lineas de datos y pone `start` / `end` / `partitionName` |
| Workers | Step chunk 5 + skip + retry 3 + backoff exponencial (sin `taskExecutor` en el Step) |
| Master | `.partitioner(...).partitionHandler(...)` con `gridSize` de properties |
| Pool | `PartitionTaskExecutorConfig` (`banco-particion-*`) |
| Readers | `LectoresCsvRango` + `@StepScope` (`#{stepExecutionContext['start']}`) |
| SkipPolicy | `BancoSkipPolicy`: datos sucios (fechas `2024-13-01`, tipos `invalid`/`pago`, etc.) |
| RetryPolicy | `BancoRetryPolicy` + `ServicioTasas` (falla transitoria deterministica) |
| Listeners | Job (cuenta particiones `:partition`), Step y Skip |

## Estructura

```
src/main/java/cl/duoc/bancoxyz/semana3/
├── BancoXyzSemana3Application.java
├── controllers/     POST /api/jobs/*
├── services/        JobLauncher + ServicioTasas
├── config/          3 Jobs, escalado local/remoto, writers MERGE, Actuator
├── partitioners/    BancoRangoPartitioner
├── policies/        Skip / Retry / Completion
├── listeners/       Job, Step, Skip
├── processors/      validacion semana_3
├── dtos/
├── entities/
├── exceptions/
└── util/
```

El wallet **no** va dentro de este proyecto. Queda al lado, en la carpeta de la semana 4:

`C:/Users/lisbe/OneDrive/Escritorio/backend_semana_5/Wallet_miQuintaBD`

## Validaciones (ItemProcessor)

- **Transacciones:** tipos solo `debito`/`credito` (`invalid`, `desconocido` → SKIP);
  fechas mixtas (`yyyy-MM-dd`, `yyyy/MM/dd`, `dd-MM-yyyy`, `dd/MM/yyyy`);
  mes invalido `2024-13-01` → SKIP; monto vacio, cero o negativo → SKIP.
- **Intereses:** tipos solo `ahorro`/`prestamo` (`hipoteca`, `-1` → SKIP);
  edad 18–90 (100 y 150 → SKIP); saldo vacio/no positivo → SKIP;
  nombre `Unknown` → SKIP. El servicio de tasas reintenta en cuentas multiplo de 3.
- **Estados de cuenta:** tipos `deposito`/`retiro`/`compra`; `depósito` se
  normaliza a `deposito`; `pago` → SKIP; monto vacio o cero → SKIP;
  deposito negativo → SKIP; descripcion vacia se completa.

## Como ejecutar

Requisitos: JDK 17+ y Maven. El wallet debe estar descomprimido en la ruta de `TNS_ADMIN`.

```bash
cd banco-xyz-migracion-batch
mvn spring-boot:run
```

Puerto **8081**. Los Jobs no arrancan solos (`spring.batch.job.enabled=false`):

```http
POST http://localhost:8081/api/jobs/transacciones
POST http://localhost:8081/api/jobs/intereses
POST http://localhost:8081/api/jobs/estados-cuenta
POST http://localhost:8081/api/jobs/todos
```

PowerShell:

```powershell
Invoke-RestMethod -Method POST -Uri http://localhost:8081/api/jobs/todos
```

En consola deben verse hilos `banco-particion-*`, Steps
`...WorkerStep:partition0|1|2`, `SKIP` de registros sucios, `Reintento` del
servicio de tasas y `COMPLETED` de los tres Jobs.

### Ver datos en Oracle

SQL Developer / Database Actions de miQuintaBD:

```sql
SELECT COUNT(*) FROM transaccion_procesada;
SELECT * FROM resumen_transacciones;
SELECT COUNT(*) FROM interes_procesado;
SELECT COUNT(*) FROM movimiento_anual;
SELECT * FROM estado_cuenta_anual;
```

### Cambiar a H2

1. En `application.properties`, comentar el bloque Oracle.
2. Descomentar el bloque H2.
3. Volver a ejecutar. Consola: [http://localhost:8081/h2-console](http://localhost:8081/h2-console)
   JDBC URL: `jdbc:h2:file:./data/bancoxyz`

## Resultados esperados (datos oficiales de la semana 3)

Cada CSV tiene **1000** filas. Ejecucion real contra Oracle (`POST /api/jobs/todos`,
`gridSize=3`):

| Job | Leidos | Escritos | Omitidos (SKIP) | Particiones | Estado |
| --- | --- | --- | --- | --- | --- |
| `transaccionesDiariasJob` | 1000 | 401 | 599 | 3 | COMPLETED |
| `interesesMensualesJob` | 1000 | 203 | 797 | 3 | COMPLETED |
| `estadosCuentaAnualesJob` | 1000 | 817 | 183 | 3 | COMPLETED |

Ejemplos de filas que se omiten:

- Transacciones: `tipo=invalid|desconocido`, `2024-13-01`, monto vacio/`0`/`-200`
- Intereses: `Unknown`, edad `100`/`150`/vacia, `hipoteca`, tipo `-1`, saldo vacio
- Estados: `pago`, monto vacio, deposito `-100`, monto `0`

Los tres master Steps lanzan **3 particiones**. El resumen de transacciones y
el informe anual se generan **despues** de que terminan los workers.

## Recomendaciones extra del profesor (semana 3)

No eran correcciones de la nota 7: son profundizacion de Spring Batch.
Quedan en este mismo proyecto (copia S3 para el repo de la semana 4).
El modo por defecto sigue siendo **local** (el que se evaluo).

### 1. Observabilidad (Micrometer)

Actuator + Prometheus. Tras levantar la app:

- `GET http://localhost:8081/actuator/health`
- `GET http://localhost:8081/actuator/metrics`
- `GET http://localhost:8081/actuator/metrics/bancoxyz.job.duration`
- `GET http://localhost:8081/actuator/prometheus`

Cada Job registra duracion, leidos, escritos, omitidos y estado.

### 2. Restart / reanudacion

Si un Job queda `FAILED` o `STOPPED`, no hay que partir de cero:

```http
GET  http://localhost:8081/api/jobs/ejecuciones
POST http://localhost:8081/api/jobs/{jobExecutionId}/restart
```

Spring Batch retoma desde el ultimo chunk/particion confirmado en el JobRepository.

### 3. Idempotencia

Los writers hacen `MERGE` (Oracle/H2) u `ON CONFLICT` (PostgreSQL) sobre claves unicas
(`transaccion_id`, `cuenta_id`, clave de movimiento anual). Relanzar o reanudar
no duplica filas. Los resúmenes se regeneran con `DELETE` + `INSERT`.

### 4. Particionamiento remoto y remote chunking

En `application.properties`:

```properties
# local              = hilos en la misma JVM (entrega semana 3)
# remoto-particion   = el maestro manda rangos start/end por un canal
# remoto-chunk       = el maestro lee el CSV y el worker procesa/escribe el chunk
bancoxyz.escalado.modo=local
```

Los canales son de Spring Integration **en la misma JVM** (sin RabbitMQ), para
poder demostrar el patron en clase. En un escenario distribuido ese canal se
cambia por una cola AMQP y el worker corre en otro proceso.
