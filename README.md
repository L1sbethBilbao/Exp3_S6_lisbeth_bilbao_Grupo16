# Banco XYZ - Backend for Frontend (Semana 5)

Actividad sumativa: **Implementando el patron arquitectonico Backend for Frontend (BFF)**.
Continuidad del Banco XYZ. Entrega: `Exp2_S5_lisbeth_bilbao_Grupo_16`.

Esta semana mantiene tres BFF **independientes** (web, movil, cajero) y agrega
**HTTPS + certificados**, **autenticacion y autorizacion por canal**, y
**optimizacion** de llamadas al core (timeouts y consultas en paralelo en web).

El Batch de la semana 3 sigue en la raiz (puerto 8081). Detalle: [README-BATCH.md](README-BATCH.md).

## Propuesta tecnica: estrategia elegida

La guia de aprendizaje lista 3 estrategias. Se eligio la **1: backends independientes
por cada tipo de cliente**, con un toque de la **3** (el BFF Web combina cuenta +
movimientos + resumen, en paralelo).

| Estrategia | Por que si / no |
| --- | --- |
| **1. Backends independientes** (elegida) | La actividad y la pauta CL piden "su propio Backend" / BFF completamente independientes. Auth, HTTPS y permisos distintos por canal. Codigo por proyecto (criterio organizacion). |
| 2. Endpoints `/web` y `/mobile` en un solo servicio | Como el monolito del material o el `bff-unificado` de la clase demo. No cumple "su propio backend" ni el 100% de la pauta. |
| 3. BFF que combina microservicios | El core es un `ms-cuentas`; el BFF Web si orquesta dos llamadas (cuenta + movimientos) en paralelo. |

La clase (RutaExpress agregador) se uso como referencia de **tecnicas** (timeouts,
paralelismo, payloads distintos), no como arquitectura a copiar.

```
App Web     -->  bff-web    :8091 (HTTPS)  -->  ms-cuentas :8090 (HTTP interno)
App Movil   -->  bff-movil  :8092 (HTTPS)  -->  ms-cuentas :8090
Cajero      -->  bff-cajero :8093 (HTTPS)  -->  ms-cuentas :8090
```

`ms-cuentas` persiste en **Oracle Autonomous** y expone el dato completo.
Los BFF recortan segun el canal y se exponen solo por **HTTPS**.

## Estructura

```
Exp2_S5_lisbeth_bilbao_Grupo_16/
├── ms-cuentas/     backend interno (CSV, Oracle Autonomous)
├── bff-web/        datos completos + resumen (HTTPS)
├── bff-movil/      saldo, nombre, 3 ultimos movimientos (HTTPS)
├── bff-cajero/     saldo, movimientos basicos y retiro (HTTPS)
├── certs/          keystore PKCS12 fuente (copiado a cada BFF)
└── (raiz)          Spring Batch semana 3, puerto 8081
```

## Seguridad por canal (HTTPS + token + permisos)

| Canal | Puerto | Token (`X-Canal-Token`) | Permisos | Que ve / puede |
| --- | --- | --- | --- | --- |
| Web | 8091 HTTPS | `token-web-xyz` | `CONSULTA_COMPLETA` | cuenta completa, historial, resumen, transacciones |
| Movil | 8092 HTTPS | `token-movil-xyz` | `CONSULTA_LIVIANA` | id, nombre, saldo y 3 movimientos (sin descripcion ni edad) |
| Cajero | 8093 HTTPS | `token-cajero-xyz` | `SALDO`, `MOVIMIENTOS`, `RETIRO` | saldo, 2 movimientos, POST retiro |

- Certificado: `keystore.p12` (PKCS12, alias `bancoxyz-bff`, password `bancoxyz-s5`).
- Token invalido o de otro canal → **401**.
- Permiso insuficiente (cajero) → **403**.
- Solo el BFF cajero expone retiro (autorizacion por superficie de API + permiso `RETIRO`).

### Regenerar el certificado (opcional)

```powershell
keytool -genkeypair -alias bancoxyz-bff -keyalg RSA -keysize 2048 -storetype PKCS12 `
  -keystore certs\bancoxyz-bff.p12 -validity 825 -storepass bancoxyz-s5 -keypass bancoxyz-s5 `
  -dname "CN=localhost, OU=BancoXYZ, O=DuocUC, L=Santiago, ST=RM, C=CL" `
  -ext "SAN=DNS:localhost,IP:127.0.0.1"
Copy-Item certs\bancoxyz-bff.p12 bff-web\src\main\resources\keystore.p12 -Force
Copy-Item certs\bancoxyz-bff.p12 bff-movil\src\main\resources\keystore.p12 -Force
Copy-Item certs\bancoxyz-bff.p12 bff-cajero\src\main\resources\keystore.p12 -Force
```

## Optimizacion

- Respuestas recortadas por canal (menos payload en movil/cajero).
- Timeouts al core: connect 2s / read 3s en los tres BFF.
- BFF Web: cuenta + movimientos en **paralelo** (`CompletableFuture`).
- BFF Cajero: no lista cuentas completas; solo saldo / pocos movimientos / retiro.

## Como ejecutar

JDK 17+ y Maven. Wallet Oracle de la semana 3. **Cuatro terminales**, en este orden:

```powershell
cd ms-cuentas
mvn spring-boot:run
```

```powershell
cd bff-web
mvn spring-boot:run
```

```powershell
cd bff-movil
mvn spring-boot:run
```

```powershell
cd bff-cajero
mvn spring-boot:run
```

Los BFF usan certificado autofirmado: en PowerShell 7+ usa `-SkipCertificateCheck`.

### Web (HTTPS, datos ricos)

```powershell
$h = @{ "X-Canal-Token" = "token-web-xyz" }
Invoke-RestMethod -SkipCertificateCheck -Headers $h -Uri https://localhost:8091/bff/web/cuentas
Invoke-RestMethod -SkipCertificateCheck -Headers $h -Uri https://localhost:8091/bff/web/cuentas/101
Invoke-RestMethod -SkipCertificateCheck -Headers $h -Uri https://localhost:8091/bff/web/transacciones
```

### Movil (HTTPS, liviano)

```powershell
$h = @{ "X-Canal-Token" = "token-movil-xyz" }
Invoke-RestMethod -SkipCertificateCheck -Headers $h -Uri https://localhost:8092/bff/movil/cuentas
Invoke-RestMethod -SkipCertificateCheck -Headers $h -Uri https://localhost:8092/bff/movil/cuentas/101
```

### Cajero (HTTPS, saldo y retiro)

```powershell
$h = @{ "X-Canal-Token" = "token-cajero-xyz" }
Invoke-RestMethod -SkipCertificateCheck -Headers $h -Uri https://localhost:8093/bff/cajero/saldo/101
Invoke-RestMethod -SkipCertificateCheck -Headers $h -Uri https://localhost:8093/bff/cajero/cuentas/101/movimientos
Invoke-RestMethod -SkipCertificateCheck -Method POST -Headers $h -ContentType "application/json" `
  -Uri https://localhost:8093/bff/cajero/cuentas/101/retiro `
  -Body '{"monto":100}'
```

### Auth (401 si el token es de otro canal)

```powershell
Invoke-RestMethod -SkipCertificateCheck -Headers @{ "X-Canal-Token" = "token-web-xyz" } `
  -Uri https://localhost:8093/bff/cajero/saldo/101
```

## Datos

CSV oficiales de [bank_legacy_data](https://github.com/KariVillagran/bank_legacy_data)
`data/semana_3`. El core los persiste en **Oracle Autonomous** (wallet `Wallet_miQuintaBD`)
y descarta filas sucias para entregar datos consistentes a los tres BFF.

## Version

- Java 17, Spring Boot 3.3.5
- HTTPS (PKCS12) en los tres BFF
- Oracle Autonomous
- Entrega individual / Grupo 16
