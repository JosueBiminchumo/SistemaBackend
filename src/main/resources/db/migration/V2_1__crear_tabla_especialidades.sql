CREATE TABLE specialties (
    id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(255),
    active BOOLEAN NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    CONSTRAINT pk_specialties PRIMARY KEY (id),
    CONSTRAINT uk_specialties_name UNIQUE (name)
);