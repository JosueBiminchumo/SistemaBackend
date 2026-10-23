CREATE INDEX idx_citas_inicio_estado_doctor
    ON citas (start_at, status, doctor_id);
