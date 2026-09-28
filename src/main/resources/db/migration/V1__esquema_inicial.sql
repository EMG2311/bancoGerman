CREATE TABLE usuario (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    nombre        VARCHAR(100) NOT NULL,
    email         VARCHAR(255) NOT NULL,
    telefono      VARCHAR(30),
    password_hash VARCHAR(100) NOT NULL,
    creado_en     DATETIME(6)  NOT NULL,
    CONSTRAINT uk_usuario_email UNIQUE (email)
);

CREATE TABLE relacion (
    id                    BIGINT AUTO_INCREMENT PRIMARY KEY,
    acreedor_id           BIGINT        NOT NULL,
    deudor_id             BIGINT        NOT NULL,
    saldo                 DECIMAL(15,2) NOT NULL DEFAULT 0,
    requiere_confirmacion BOOLEAN       NOT NULL DEFAULT FALSE,
    estado                ENUM('INVITADA','ACTIVA','RECHAZADA','ELIMINADA') NOT NULL,
    creado_en             DATETIME(6)   NOT NULL,
    respondida_en         DATETIME(6),
    eliminada_en          DATETIME(6),
    -- 1 mientras la relación está viva, NULL cuando se rechazó o eliminó: MySQL permite
    -- varios NULL en un UNIQUE, así que solo puede haber una relación viva por par
    -- acreedor→deudor, pero se puede volver a invitar después de eliminar/rechazar.
    vigente               TINYINT GENERATED ALWAYS AS (IF(estado IN ('INVITADA','ACTIVA'), 1, NULL)) STORED,
    CONSTRAINT fk_rel_acreedor FOREIGN KEY (acreedor_id) REFERENCES usuario (id),
    CONSTRAINT fk_rel_deudor   FOREIGN KEY (deudor_id)   REFERENCES usuario (id),
    CONSTRAINT uk_rel_vigente  UNIQUE (acreedor_id, deudor_id, vigente),
    CONSTRAINT ck_rel_distintos CHECK (acreedor_id <> deudor_id),
    CONSTRAINT ck_rel_saldo     CHECK (saldo >= 0)
);

CREATE TABLE solicitud_cobro (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    relacion_id   BIGINT        NOT NULL,
    monto         DECIMAL(15,2) NOT NULL,
    estado        ENUM('PENDIENTE','PAGADA','RECHAZADA') NOT NULL,
    descripcion   VARCHAR(255),
    fecha         DATETIME(6)   NOT NULL,
    respondida_en DATETIME(6),
    CONSTRAINT fk_sol_relacion FOREIGN KEY (relacion_id) REFERENCES relacion (id),
    CONSTRAINT ck_sol_monto CHECK (monto > 0)
);
CREATE INDEX ix_sol_relacion_estado ON solicitud_cobro (relacion_id, estado);

CREATE TABLE movimiento (
    id                    BIGINT AUTO_INCREMENT PRIMARY KEY,
    relacion_id           BIGINT        NOT NULL,
    tipo                  ENUM('PAGO','CARGO') NOT NULL,
    monto                 DECIMAL(15,2) NOT NULL,
    estado                ENUM('PENDIENTE','CONFIRMADO','RECHAZADO') NOT NULL,
    medio_pago            ENUM('TRANSFERENCIA','EFECTIVO'),
    -- Foto del flag de la relación al crear el movimiento (el flag no es retroactivo)
    requirio_confirmacion BOOLEAN       NOT NULL,
    creado_por_id         BIGINT        NOT NULL,
    solicitud_id          BIGINT,
    descripcion           VARCHAR(255),
    fecha                 DATETIME(6)   NOT NULL,
    resuelto_en           DATETIME(6),
    CONSTRAINT fk_mov_relacion  FOREIGN KEY (relacion_id)   REFERENCES relacion (id),
    CONSTRAINT fk_mov_creador   FOREIGN KEY (creado_por_id) REFERENCES usuario (id),
    CONSTRAINT fk_mov_solicitud FOREIGN KEY (solicitud_id)  REFERENCES solicitud_cobro (id),
    CONSTRAINT ck_mov_monto CHECK (monto > 0),
    -- Los pagos siempre dicen por qué medio se hicieron; los cargos nunca
    CONSTRAINT ck_mov_medio CHECK ((tipo = 'PAGO') = (medio_pago IS NOT NULL))
);
CREATE INDEX ix_mov_relacion_estado ON movimiento (relacion_id, estado);
CREATE INDEX ix_mov_relacion_fecha  ON movimiento (relacion_id, fecha);

CREATE TABLE dispositivo (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    usuario_id     BIGINT       NOT NULL,
    push_token     VARCHAR(512) NOT NULL,
    plataforma     ENUM('ANDROID','IOS','WEB') NOT NULL,
    creado_en      DATETIME(6)  NOT NULL,
    actualizado_en DATETIME(6)  NOT NULL,
    CONSTRAINT fk_disp_usuario FOREIGN KEY (usuario_id) REFERENCES usuario (id),
    CONSTRAINT uk_disp_token UNIQUE (push_token)
);

CREATE TABLE notificacion (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    usuario_id BIGINT      NOT NULL,
    tipo       VARCHAR(50) NOT NULL,
    payload    JSON        NOT NULL,
    fecha      DATETIME(6) NOT NULL,
    leida      BOOLEAN     NOT NULL DEFAULT FALSE,
    CONSTRAINT fk_notif_usuario FOREIGN KEY (usuario_id) REFERENCES usuario (id)
);
CREATE INDEX ix_notif_usuario ON notificacion (usuario_id, leida, fecha);
