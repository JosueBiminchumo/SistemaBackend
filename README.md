# MediTurno Backend

API independiente de Spring Boot para MediTurno. El primer módulo implementa
identidad, autenticación y autorización para los roles `ADMIN`, `DOCTOR` y
`PATIENT`.

## Requisitos

- Java 21
- Maven 3.9+
- MySQL 8+

## Configuración local

Defina `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` y `JWT_SECRET` en el entorno antes
de iniciar la API. `JWT_SECRET` debe ser una clave Base64 que represente al
menos 256 bits. Los secretos y contraseñas reales no deben guardarse en Git.

La aplicación ejecuta migraciones Flyway y valida el esquema con Hibernate;
no crea ni modifica tablas automáticamente. Para desarrollo, cree la base
`mediturno` en MySQL y use un usuario con permisos limitados a esa base.

Desde esta carpeta:

```powershell
$env:JWT_SECRET = "<clave-base64-de-al-menos-32-bytes>"
$env:DB_PASSWORD = "<contraseña-local>"
mvn spring-boot:run
```

Para ejecutar las pruebas, que usan H2 en memoria:

```powershell
mvn test
```

## Autenticación

- `POST /api/v1/auth/register`: crea una cuenta `PATIENT` si el registro está habilitado.
- `POST /api/v1/auth/login`: valida credenciales y devuelve un JWT.
- Las rutas `/api/v1/admin/**`, `/api/v1/doctors/**` y `/api/v1/patients/**`
  requieren respectivamente `ADMIN`, `DOCTOR` o `PATIENT` (también se permite
  `ADMIN` en las rutas de los otros roles).
- Las contraseñas se almacenan con BCrypt. El hash nunca forma parte de las respuestas.

El registro se puede deshabilitar en despliegues existentes con
`AUTH_REGISTRATION_ENABLED=false`. La duración del token se configura con
`JWT_EXPIRATION_MS` (por defecto, una hora).

Ejemplo de registro:

```json
{
  "firstName": "Ana",
  "lastName": "Pérez",
  "email": "ana@example.com",
  "password": "una-clave-segura"
}
```

El registro responde `201 Created` y crea siempre el rol `PATIENT`; un campo
`role` enviado por el cliente no se utiliza. El login responde `200 OK`,
`401 Unauthorized` corresponde a credenciales inválidas y `409 Conflict` a un
correo ya registrado.

La política de CORS y la documentación Swagger se coordinan con el integrante 7.

## Módulos de negocio (todos integrados)

Todas las rutas cuelgan de `/api/v1`. Errores con el formato `ApiError`
(`timestamp, status, error, message, path, details`): 400 validación, 401 sin token,
403 sin rol, 404 inexistente, 409 conflicto/duplicado.

| Módulo | Rutas | Acceso |
|---|---|---|
| Especialidades | `GET /specialties`, `GET /specialties/{id}` | Público (solo activas; ADMIN puede `?includeInactive=true`) |
| | `POST /specialties`, `PUT /specialties/{id}`, `DELETE /specialties/{id}` (baja lógica) | ADMIN |
| Médicos | `GET /doctors?specialtyId=&q=`, `GET /doctors/{id}` | Público (solo activos) |
| | `GET /doctors/{id}/schedule?date=YYYY-MM-DD` (franjas con `available`) | Público |
| | `GET /doctors/{id}/schedule/rules` | Público |
| | `PUT /doctors/{id}/schedule/rules` (reemplaza la agenda semanal) | DOCTOR dueño o ADMIN |
| | `GET/PUT /doctors/me` (biografía) | DOCTOR |
| | `POST /doctors`, `PUT /doctors/{id}`, `DELETE /doctors/{id}` | ADMIN |
| Citas | `POST /appointments` (`doctorId`, `startAt`, `reason`) | PATIENT (el paciente sale del JWT) |
| | `GET /appointments?status=&from=&to=&doctorId=` | Cada rol ve solo lo suyo; ADMIN ve todas |
| | `GET /appointments/{id}` | Dueño, médico asignado o ADMIN |
| | `PUT /appointments/{id}` (reprogramar) | PATIENT dueño o ADMIN |
| | `PATCH /appointments/{id}/cancel` | Paciente dueño, médico asignado o ADMIN |
| | `PATCH /appointments/{id}/status` (`CONFIRMED`/`COMPLETED`/`CANCELLED`) | DOCTOR asignado o ADMIN |
| Estadísticas | `GET /statistics/dashboard?from=&to=` | ADMIN |
| | `GET /statistics/appointments?groupBy=day\|status\|specialty&from=&to=` | ADMIN |

Reglas de citas: estados `PENDING → CONFIRMED → COMPLETED` (o `CANCELLED`); la hora debe caer
exactamente en una franja de la agenda activa del médico, con al menos 30 min de anticipación;
no se permiten cruces del médico ni del paciente (409). La reserva bloquea la fila del médico
y la BD tiene una clave única (`active_slot_key`) como segunda barrera contra doble reserva.

Fechas: `startAt` es hora local de `America/Lima` (`APP_TIMEZONE`), sin offset, ej. `2026-11-03T09:30:00`.
Estadísticas: `from`/`to` inclusivos, por defecto los últimos 30 días, máximo 366 días.

### Primer administrador y datos demo

- `ADMIN_EMAIL` + `ADMIN_PASSWORD` crean el primer ADMIN al arrancar (si no existe).
- Con `SPRING_PROFILES_ACTIVE=local` y `DEMO_PASSWORD` definido se cargan 4 especialidades,
  3 médicos con agenda L-V y `admin@mediturn.pe` (solo si la tabla de especialidades está vacía).
