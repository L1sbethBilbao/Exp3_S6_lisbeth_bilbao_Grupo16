DROP TABLE IF EXISTS transaccion_procesada;
DROP TABLE IF EXISTS resumen_transacciones;
DROP TABLE IF EXISTS interes_procesado;
DROP TABLE IF EXISTS movimiento_anual;
DROP TABLE IF EXISTS estado_cuenta_anual;

CREATE TABLE transaccion_procesada (
    id              BIGSERIAL PRIMARY KEY,
    transaccion_id  INT            NOT NULL,
    fecha           DATE           NOT NULL,
    monto           DECIMAL(12, 2) NOT NULL,
    tipo            VARCHAR(20)    NOT NULL,
    estado          VARCHAR(20)    NOT NULL,
    observacion     VARCHAR(255),
    CONSTRAINT uq_transaccion_procesada_id UNIQUE (transaccion_id)
);

CREATE TABLE resumen_transacciones (
    id                   BIGSERIAL PRIMARY KEY,
    fecha_proceso        TIMESTAMP      NOT NULL,
    total_validas        INT            NOT NULL,
    total_debitos        DECIMAL(14, 2) NOT NULL,
    total_creditos       DECIMAL(14, 2) NOT NULL,
    cantidad_debitos     INT            NOT NULL,
    cantidad_creditos    INT            NOT NULL,
    job_execution_id     BIGINT,
    CONSTRAINT uq_resumen_transacciones_job UNIQUE (job_execution_id)
);

CREATE TABLE interes_procesado (
    id                 BIGSERIAL PRIMARY KEY,
    cuenta_id          INT            NOT NULL,
    nombre             VARCHAR(100)   NOT NULL,
    saldo_inicial      DECIMAL(14, 2) NOT NULL,
    edad               INT            NOT NULL,
    tipo               VARCHAR(20)    NOT NULL,
    tasa_aplicada      DECIMAL(6, 4)  NOT NULL,
    interes_calculado  DECIMAL(14, 2) NOT NULL,
    saldo_final        DECIMAL(14, 2) NOT NULL,
    estado             VARCHAR(20)    NOT NULL,
    CONSTRAINT uq_interes_procesado_cuenta UNIQUE (cuenta_id)
);

CREATE TABLE movimiento_anual (
    id            BIGSERIAL PRIMARY KEY,
    cuenta_id     INT            NOT NULL,
    fecha         DATE           NOT NULL,
    transaccion   VARCHAR(30)    NOT NULL,
    monto         DECIMAL(14, 2) NOT NULL,
    descripcion   VARCHAR(200)   NOT NULL,
    estado        VARCHAR(20)    NOT NULL,
    CONSTRAINT uq_movimiento_anual_clave UNIQUE (cuenta_id, fecha, transaccion, monto, descripcion)
);

CREATE TABLE estado_cuenta_anual (
    id                     BIGSERIAL PRIMARY KEY,
    cuenta_id              INT            NOT NULL,
    anio                   INT            NOT NULL,
    total_depositos        DECIMAL(14, 2) NOT NULL,
    total_retiros          DECIMAL(14, 2) NOT NULL,
    saldo_neto             DECIMAL(14, 2) NOT NULL,
    cantidad_movimientos   INT            NOT NULL,
    CONSTRAINT uq_estado_cuenta_anual UNIQUE (cuenta_id, anio)
);
