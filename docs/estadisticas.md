# Estadisticas y dashboard

Modulo del integrante 6. Entrega indicadores administrativos a partir de las
tablas compartidas, sin crear entidades de negocio adicionales.

## Base de trabajo

La rama `feature/backend-statistics` parte de
`feature/integrante-7-config` (`146679b`). Esta base contiene autenticacion,
pacientes y las migraciones de `users`, `patients`, `doctors`, `specialties`
y `citas`. Las implementaciones de citas y medicos siguen en sus propias
ramas; los nombres SQL se contrastan con esas ramas antes de consultar datos.

## Operaciones previstas

- `GET /api/v1/statistics/dashboard`: totales y conteos de citas por estado.
- `GET /api/v1/statistics/appointments`: series por `day`, `status` o
  `specialty`.
- Acceso exclusivo al rol `ADMIN` mediante el JWT del modulo de usuarios.
- Filtros `from` y `to` como fechas ISO `YYYY-MM-DD`, ambos inclusivos.
- Consultas agregadas en la base de datos; las respuestas no incluyen
  nombres, documentos ni datos de contacto de pacientes.

Las respuestas usan `ApiResponse` y los errores usan `ApiError`, definidos
por la base compartida. Los contratos se documentan aqui conforme se agregan
las operaciones.
