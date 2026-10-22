# MediTurno Backend

API REST de MediTurno con Java 21, Spring Boot y Maven Wrapper. Incluye autenticación JWT y perfil de paciente.

## Requisitos

- JDK 21
- MySQL 8+

No hace falta instalar Maven globalmente. En Windows, ejecuta `mvnw.cmd` desde la raíz del repositorio; la primera ejecución descarga Maven.

## Configuración local

Crea la base de datos `mediturno` en MySQL. Configura las variables de entorno antes de iniciar la API:

- `DB_URL` (por defecto `jdbc:mysql://localhost:3306/mediturno?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC`)
- `DB_USERNAME` (por defecto `mediturno`)
- `DB_PASSWORD` (contraseña de tu usuario MySQL)
- `JWT_SECRET` (clave Base64 de al menos 256 bits)
- `SERVER_PORT` (por defecto `8080`)

En PowerShell:

```powershell
$env:DB_USERNAME = "tu_usuario_mysql"
$env:DB_PASSWORD = "tu_contraseña_mysql"
$env:JWT_SECRET = "<clave-base64-de-al-menos-32-bytes>"
.\mvnw.cmd spring-boot:run
```

Flyway aplica las migraciones y Hibernate valida el esquema. No se deben guardar secretos reales en Git.

## Comandos

```powershell
.\mvnw.cmd -version
.\mvnw.cmd compile
.\mvnw.cmd test
```

## API disponible

- `POST /api/v1/auth/register`: registra una cuenta PATIENT y su perfil.
- `POST /api/v1/auth/login`: valida credenciales y devuelve un JWT.
- `GET /api/v1/patients/me`: consulta el perfil del paciente autenticado.
- `PUT /api/v1/patients/me`: actualiza DNI, fecha de nacimiento y teléfono.

El registro recibe `firstName`, `lastName`, `documentNumber`, `birthDate`, `phone`, `email` y `password`. Las contraseñas se almacenan con BCrypt; el hash nunca se devuelve. Las rutas de pacientes requieren el rol `PATIENT` o `ADMIN`.

El registro puede deshabilitarse con `AUTH_REGISTRATION_ENABLED=false`. La duración del JWT se configura con `JWT_EXPIRATION_MS` (por defecto, una hora).

Los datos inválidos responden `400`; un correo o DNI ya registrado responde `409`; si no existe el perfil asociado al usuario autenticado, responde `404`.

Ejemplo de registro:

```json
{
  "firstName": "Ana",
  "lastName": "Pérez",
  "documentNumber": "12345678",
  "birthDate": "1998-04-12",
  "phone": "987654321",
  "email": "ana@example.com",
  "password": "una-clave-segura"
}
```
