-- Shared, deterministic fixture. Use inside a rollback transaction with @Sql.
-- DELETE keeps schema migrations intact and remains transactional, unlike TRUNCATE.
DELETE FROM citas;
DELETE FROM doctor_schedules;
DELETE FROM doctors;
DELETE FROM patients;
DELETE FROM specialties;
DELETE FROM users;

INSERT INTO users
    (id, first_name, last_name, email, password_hash, role, active, created_at, updated_at)
VALUES
    (9001, 'Admin', 'Statistics', 'statistics.admin@example.com', 'unused-fixture-hash', 'ADMIN', TRUE, '2026-09-01 10:00:00', '2026-09-01 10:00:00'),
    (9002, 'Ana', 'Patient', 'statistics.patient.one@example.com', 'unused-fixture-hash', 'PATIENT', TRUE, '2026-09-01 10:00:00', '2026-09-01 10:00:00'),
    (9003, 'Luis', 'Patient', 'statistics.patient.two@example.com', 'unused-fixture-hash', 'PATIENT', TRUE, '2026-09-01 10:00:00', '2026-09-01 10:00:00'),
    (9004, 'Carla', 'Doctor', 'statistics.doctor.one@example.com', 'unused-fixture-hash', 'DOCTOR', TRUE, '2026-09-01 10:00:00', '2026-09-01 10:00:00'),
    (9005, 'Diego', 'Doctor', 'statistics.doctor.two@example.com', 'unused-fixture-hash', 'DOCTOR', FALSE, '2026-09-01 10:00:00', '2026-09-01 10:00:00');

INSERT INTO patients
    (user_id, document_number, birth_date, phone, created_at, updated_at)
VALUES
    (9002, '90000002', '1990-01-01', '999000002', '2026-09-01 10:00:00', '2026-09-01 10:00:00'),
    (9003, '90000003', '1992-02-02', '999000003', '2026-09-01 10:00:00', '2026-09-01 10:00:00');

-- Insert out of alphabetical order so a tied total must explicitly order by name.
INSERT INTO specialties (id, name, description, active, created_at)
VALUES
    (31, 'Neurologia', 'Inactive specialty with historical appointments', FALSE, '2026-09-01 10:00:00'),
    (32, 'Cardiologia', 'Active specialty with appointments', TRUE, '2026-09-01 10:00:00'),
    (33, 'Pediatria', 'Specialty without appointments', TRUE, '2026-09-01 10:00:00');

-- All three ID spaces differ to detect incorrect joins between user/doctor/specialty.
INSERT INTO doctors (id, user_id, specialty_id, license_number, biography, active)
VALUES
    (101, 9004, 32, 'STAT-CMP-001', 'Active doctor', TRUE),
    (202, 9005, 31, 'STAT-CMP-002', 'Inactive doctor with historical appointments', FALSE);

-- Four appointments in 2026-10-01..2026-10-23 inclusive, one per status.
-- The final second is compatible with the production DATETIME(0) precision.
-- created_at intentionally differs from the period: only start_at defines it.
INSERT INTO citas
    (id, patient_id, doctor_id, start_at, end_at, status, reason, active_slot_key, created_at, updated_at, version)
VALUES
    (1001, 9002, 101, '2026-10-01 00:00:00', '2026-10-01 00:30:00', 'PENDING', 'Start boundary', NULL, '2026-09-15 10:00:00', '2026-09-15 10:00:00', 0),
    (1002, 9002, 101, '2026-10-15 09:00:00', '2026-10-15 09:30:00', 'CONFIRMED', 'First appointment on shared day', NULL, '2026-09-16 10:00:00', '2026-09-16 10:00:00', 0),
    (1003, 9003, 202, '2026-10-15 10:00:00', '2026-10-15 10:30:00', 'COMPLETED', 'Inactive doctor and specialty history', NULL, '2026-09-17 10:00:00', '2026-09-17 10:00:00', 0),
    (1004, 9003, 202, '2026-10-23 23:59:59', '2026-10-24 00:29:59', 'CANCELLED', 'End boundary', NULL, '2026-09-18 10:00:00', '2026-09-18 10:00:00', 0),
    (1005, 9002, 101, '2026-09-30 23:59:59', '2026-10-01 00:29:59', 'PENDING', 'Outside before range despite created_at inside', NULL, '2026-10-10 10:00:00', '2026-10-10 10:00:00', 0),
    (1006, 9003, 202, '2026-10-24 00:00:00', '2026-10-24 00:30:00', 'CONFIRMED', 'Outside after range despite created_at inside', NULL, '2026-10-15 10:00:00', '2026-10-15 10:00:00', 0);
