CREATE TABLE citas (
    id              BIGINT       NOT NULL AUTO_INCREMENT,
    patient_id      BIGINT       NOT NULL,
    doctor_id       BIGINT       NOT NULL,
    start_at        DATETIME     NOT NULL,
    end_at          DATETIME     NOT NULL,
    status          VARCHAR(20)  NOT NULL,
    reason          VARCHAR(255) NOT NULL,
    active_slot_key VARCHAR(64)  NULL,
    created_at      DATETIME     NOT NULL,
    updated_at      DATETIME     NOT NULL,
    version         BIGINT       NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    CONSTRAINT uk_citas_active_slot UNIQUE (active_slot_key),
    CONSTRAINT fk_citas_paciente FOREIGN KEY (patient_id) REFERENCES patients (user_id),
    CONSTRAINT fk_citas_doctor FOREIGN KEY (doctor_id) REFERENCES doctors (id),
    CONSTRAINT chk_citas_rango CHECK (end_at > start_at)
);

CREATE INDEX idx_citas_doctor_inicio ON citas (doctor_id, start_at);
CREATE INDEX idx_citas_paciente_inicio ON citas (patient_id, start_at);
CREATE INDEX idx_citas_estado ON citas (status);