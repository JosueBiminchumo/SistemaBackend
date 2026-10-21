# MediTurno Backend

API independiente de Spring Boot para MediTurno. El primer módulo implementa
identidad, autenticación y autorización para los roles `ADMIN`, `DOCTOR` y
`PATIENT`.

## Requisitos

- Java 25 (LTS)
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
- `GET /api/v1/patients/me`: devuelve el perfil del paciente autenticado.
- `PUT /api/v1/patients/me`: actualiza DNI, fecha de nacimiento y teléfono.
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
  "documentNumber": "12345678",
  "birthDate": "1998-05-20",
  "phone": "987654321",
  "email": "ana@example.com",
  "password": "una-clave-segura"
}
```

El registro responde `201 Created` y crea siempre el rol `PATIENT`; un campo
`role` enviado por el cliente no se utiliza. El login responde `200 OK`,
`401 Unauthorized` corresponde a credenciales inválidas y `409 Conflict` a un
correo ya registrado.

La política de CORS y la documentación Swagger se coordinan con el integrante 7.
