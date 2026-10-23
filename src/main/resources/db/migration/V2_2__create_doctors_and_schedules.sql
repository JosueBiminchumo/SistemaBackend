CREATE TABLE doctors (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    specialty_id BIGINT NOT NULL,
    license_number VARCHAR(50) NOT NULL,
    biography VARCHAR(2000),
    active BOOLEAN NOT NULL DEFAULT TRUE,

    CONSTRAINT uk_doctor_user UNIQUE (user_id),
    CONSTRAINT uk_doctor_license UNIQUE (license_number),
    CONSTRAINT fk_doctor_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_doctor_specialty FOREIGN KEY (specialty_id) REFERENCES specialties (id)
);

CREATE INDEX idx_doctor_specialty_active
    ON doctors (specialty_id, active);

CREATE TABLE doctor_schedules (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    doctor_id BIGINT NOT NULL,
    day_of_week VARCHAR(15) NOT NULL,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    slot_minutes INT NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,

    CONSTRAINT fk_schedule_doctor
        FOREIGN KEY (doctor_id) REFERENCES doctors (id),

    CONSTRAINT chk_schedule_time_range
        CHECK (start_time < end_time),

    CONSTRAINT chk_schedule_slot_minutes
        CHECK (slot_minutes BETWEEN 5 AND 240)
);

CREATE INDEX idx_schedule_doctor_day_active
    ON doctor_schedules (doctor_id, day_of_week, active);