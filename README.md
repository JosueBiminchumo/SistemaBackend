# MediTurn Backend

Backend REST de MediTurn con Java 21, Spring Boot y Maven Wrapper.

## Requisitos

- JDK 21
- MySQL para ejecutar la aplicación

No hace falta instalar Maven globalmente. En Windows, usa `mvnw.cmd` desde la raíz del repositorio; la primera ejecución descarga Maven 3.9.11.

## Comandos

```powershell
.\mvnw.cmd -version
.\mvnw.cmd compile
.\mvnw.cmd spring-boot:run
```

## Configuración de base de datos

Inicia tu servicio local de MySQL y crea la base de datos `mediturn`:

```sql
CREATE DATABASE mediturn;
```

La conexión de Spring Boot se configura con estas variables de entorno:

- `DB_URL` (por defecto `jdbc:mysql://localhost:3306/mediturn?createDatabaseIfNotExist=true&useSSL=false&serverTimezone=UTC`)
- `DB_USERNAME` (por defecto `mediturn`)
- `DB_PASSWORD` (por defecto `local-dev-only`)
- `SERVER_PORT` (por defecto `8080`)

Los valores predeterminados son para desarrollo local. Si tu usuario de MySQL tiene otras credenciales, configúralas antes de iniciar la aplicación. Por ejemplo, en PowerShell:

```powershell
$env:DB_USERNAME = "tu_usuario_mysql"
$env:DB_PASSWORD = "tu_contraseña_mysql"
```

No uses las credenciales de desarrollo en producción.

El módulo de pacientes está en `src/main/java/com/mediturn/patient`. La integración de `CurrentUserProvider` con el usuario autenticado queda pendiente del módulo de autenticación.
