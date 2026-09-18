# Banco XYZ - Semana 6 (Spring Cloud)

Entrega: `Exp3_S6_lisbeth_bilbao_Grupo_16`

Continuidad del Banco XYZ (BFF Web / Movil / Cajero + `ms-cuentas`) evolucionada con:

1. **Config Server** (configuracion centralizada)
2. **Eureka** (Service Discovery + llamada por nombre logico)
3. **Resilience4j** (Circuit Breaker + Retry + Fallback en los 3 BFF)
4. **Spring Security** (HTTP Basic en BFF + Basic/OAuth2 JWT en `ms-cuentas`)

Alineado a la clase del profesor (ejemplos Config Server, Eureka, Resilience4j y OAuth2) y a la pauta de evaluacion (maximo puntaje).

## Arquitectura

```
Postman / cliente
   |  HTTP Basic (web/movil/cajero)
   v
bff-web :8091  ----\ 
bff-movil :8092 ----+--> Eureka :8761  -->  MS-CUENTAS :8090  (/core/**)
bff-cajero :8093 --/         ^                    |
                             |                    +--> /api/basic/**  (HTTP Basic)
Config Server :8888 ----------+                    +--> /api/oauth2/** (JWT)
                             |
Auth Server :9000  (emite JWT client_credentials)
```

## Proyectos

| Carpeta | Puerto | Rol |
| --- | --- | --- |
| `config-server` | 8888 | Configuracion central (native / classpath) |
| `discovery-server` | 8761 | Eureka |
| `auth-server` | 9000 | OAuth2 Authorization Server |
| `ms-cuentas` | 8090 | Core + demos Basic/OAuth2 |
| `bff-web` | 8091 | BFF Web + Resilience4j + Basic |
| `bff-movil` | 8092 | BFF Movil + Resilience4j + Basic |
| `bff-cajero` | 8093 | BFF Cajero + Resilience4j + Basic |

## Orden de arranque (importante)

Abre **6 terminales** (o mas) y ejecuta en este orden:

```powershell
cd config-server
mvn spring-boot:run
```

```powershell
cd discovery-server
mvn spring-boot:run
```

```powershell
cd auth-server
mvn spring-boot:run
```

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

Requisitos: JDK 17+, Maven, wallet Oracle de semanas anteriores (ruta en `ms-cuentas` `application.properties`).

## Credenciales (Postman - Auth Type: Basic Auth)

| Servicio | Usuario | Password | Rol |
| --- | --- | --- | --- |
| bff-web | `web` | `web123` | WEB |
| bff-movil | `movil` | `movil123` | MOVIL |
| bff-cajero | `cajero` | `cajero123` | CAJERO |
| ms-cuentas Basic | `user` | `password123` | USER |
| ms-cuentas Basic admin | `admin` | `admin123` | ADMIN |

### OAuth2 (auth-server)

- Token URL: `http://localhost:9000/oauth2/token`
- Grant type: `client_credentials`
- Client ID: `bancoxyz-bff-client`
- Client Secret: `secret123`
- Scope: `cuentas.read`

## Endpoints utiles

### Config Server
- `GET http://localhost:8888/ms-cuentas/default`
- `GET http://localhost:8888/bff-web/default`

### Eureka
- Dashboard: `http://localhost:8761`
- Deben aparecer: `MS-CUENTAS`, `BFF-WEB`, `BFF-MOVIL`, `BFF-CAJERO`

### ms-cuentas (seguridad clase)
- `GET http://localhost:8090/api/publico/info` (sin auth; muestra mensaje del Config Server)
- `GET http://localhost:8090/api/basic/cuentas` (Basic user/password123)
- `GET http://localhost:8090/api/basic/admin/cuentas` (solo admin)
- `GET http://localhost:8090/api/oauth2/cuentas` (Bearer JWT)

### BFF (Basic Auth)
- `GET http://localhost:8091/bff/web/cuentas` (web / web123)
- `GET http://localhost:8092/bff/movil/cuentas` (movil / movil123)
- `GET http://localhost:8093/bff/cajero/saldo/101` (cajero / cajero123)
- `POST http://localhost:8093/bff/cajero/cuentas/101/retiro` body `{"monto":100}`

### Resilience4j
- `GET http://localhost:8091/actuator/health`
- `GET http://localhost:8091/actuator/circuitbreakers`
- Para forzar fallback: detener `ms-cuentas` y volver a llamar el BFF.

## Criterios de la pauta cubiertos

1. Config Server integrado con microservicios (ms-cuentas + 3 BFF)
2. Eureka con al menos 3 microservicios registrados + llamada `http://MS-CUENTAS/...`
3. 3 BFF con tolerancia a fallos (Resilience4j) y autenticacion (HTTP Basic)
4. Auth + autorizacion (Basic con roles + OAuth2 JWT con scopes)

## Evidencias

Sigue el documento: `../guia_capturas_semana_6.txt` (ruta `semana_6_backend`).

## Version

- Java 17, Spring Boot 3.3.5, Spring Cloud 2023.0.3
- Resilience4j 2.1.0
- Grupo 16 / Lisbeth Bilbao
