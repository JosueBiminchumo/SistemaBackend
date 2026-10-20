CREATE TABLE patients (
    user_id BIGINT NOT NULL,
    document_number VARCHAR(8) NOT NULL,
    birth_date DATE NOT NULL,
    phone VARCHAR(20) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    CONSTRAINT pk_patients PRIMARY KEY (user_id),
    CONSTRAINT fk_patients_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT uk_patients_document_number UNIQUE (document_number),
    CONSTRAINT uk_patients_user_id UNIQUE (user_id)
);
