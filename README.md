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

La conexión se configura con estas variables de entorno:

- `DB_URL` (por defecto `jdbc:mysql://localhost:3306/mediturn?createDatabaseIfNotExist=true&useSSL=false&serverTimezone=UTC`)
- `DB_USERNAME` (por defecto `root`)
- `DB_PASSWORD` (por defecto vacío)
- `SERVER_PORT` (por defecto `8080`)

El módulo de pacientes está en `src/main/java/com/mediturn/patient`. La integración de `CurrentUserProvider` con el usuario autenticado queda pendiente del módulo de autenticación.
