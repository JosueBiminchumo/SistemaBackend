# Validacion del modulo de estadisticas

Fecha: 23 de octubre de 2026. Java 21, Spring Boot 3.4.5 y MySQL Community
Server 8.0.46. Se uso una instancia temporal con acceso solo por loopback,
una base exclusiva y datos de prueba. El servidor MySQL tenia zona `+00:00`
y modos `ONLY_FULL_GROUP_BY`, `STRICT_TRANS_TABLES`, `NO_ZERO_IN_DATE`,
`NO_ZERO_DATE`, `ERROR_FOR_DIVISION_BY_ZERO` y `NO_ENGINE_SUBSTITUTION`.

## Resultados

| Ejecucion | Resultado |
| --- | --- |
| `mvn clean verify`, suite completa con H2 | 131 pruebas, 0 fallos, 0 errores |
| Suite de estadisticas en MySQL, JVM `America/Lima` | 92 pruebas, 0 fallos, 0 errores |
| Suite de estadisticas en MySQL, JVM `Asia/Tokyo` | 92 pruebas, 0 fallos, 0 errores |
| JAR con servidor HTTP y MySQL reales | 34 comprobaciones aprobadas |
| Lecturas HTTP simultaneas durante escrituras | 120 respuestas coherentes |

Los dos recorridos MySQL ejecutan las mismas 92 pruebas. La comparacion
con H2 detecta diferencias del driver y del motor; el cambio de zona JVM
comprueba que los limites UTC mantienen su significado.

## Casos cubiertos

- Flyway aplica las seis migraciones y Hibernate valida el esquema.
- Conteos de dashboard y series por dia, estado y especialidad.
- Extremos inclusivos de la fecha, medianoche, ultimo segundo y siguiente dia.
- Dias sin citas, tablas vacias, estados ausentes y estados adicionales.
- Rango maximo de 366 dias, ano bisiesto, rango invertido, 367 dias y
  parametros invalidos, incluidos valores extremos de `LocalDate`.
- Fechas validas entre `1000-01-01` y `9999-12-30`, incluido `1582-10-10`.
- Datos historicos con medicos y especialidades inactivos.
- Tres mil citas deterministas con doce pacientes, ocho medicos y seis
  especialidades. Un oraculo Java independiente comprueba 28 rangos, sus
  agregados, el orden de los resultados y la coherencia de las sumas.
- DTO con solo datos agregados, sin documentos, telefonos, nombres de
  pacientes, contrasenas ni motivos de consulta.
- Registro y login HTTP reales con BCrypt, JWT y MySQL. El registro
  publico conserva `PATIENT` aunque reciba un campo `role: ADMIN`.
- `ADMIN` accede; `PATIENT` y `DOCTOR` reciben 403; JWT ausente, expirado,
  alterado o perteneciente a una cuenta inactiva recibe 401.
- Un claim `role: ADMIN` firmado no reemplaza el rol de la base. Cambiar
  el rol del administrador a medico revoca el acceso del mismo token.
- Swagger publica ambos endpoints. CORS permite el frontend local y
  rechaza un origen ajeno.
- Ciento veinte lecturas del dashboard, con ocho clientes en paralelo,
  mientras otra conexion inserta, cambia estados y elimina citas. En todas
  las respuestas, la suma de estados coincide con `totalAppointments`.

## Correccion encontrada en MySQL

La conversion anterior `ResultSet.getDate(...).toLocalDate()` fallaba con
`1582-10-10` por el cambio de calendario de `GregorianCalendar` usado por
Connector/J. El error se reprodujo en MySQL real. Ahora se usa
`getObject(..., LocalDate.class)` de JDBC 4.2. Las ocho pruebas del
calendario pasan tanto en H2 como en MySQL con las dos zonas JVM.

## Reproducir las suites

Con Java 21 y Maven 3.9+:

```powershell
mvn clean verify
```

Para ejecutar las mismas pruebas del modulo sobre una base MySQL de pruebas
exclusiva y vacia, configure el datasource mediante variables de entorno:

```powershell
$env:SPRING_DATASOURCE_URL = 'jdbc:mysql://127.0.0.1:33307/base_pruebas?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC'
$env:SPRING_DATASOURCE_USERNAME = '<usuario-de-pruebas>'
$env:SPRING_DATASOURCE_PASSWORD = '<clave-de-pruebas>'
$env:SPRING_DATASOURCE_DRIVER_CLASS_NAME = 'com.mysql.cj.jdbc.Driver'
mvn '-Dtest=Statistics*Test' '-DargLine=-Duser.timezone=America/Lima' test
mvn '-Dtest=Statistics*Test' '-DargLine=-Duser.timezone=Asia/Tokyo' test
```

El fixture elimina e inserta registros dentro de transacciones que se
revierten. Use una base exclusiva de pruebas; las migraciones Flyway son
permanentes en esa base. Las comprobaciones HTTP se realizaron contra el
JAR generado, en otra fase, con datos sinteticos de la misma base temporal.

## Alcance

La validacion corresponde a estadisticas y a su base compartida de
configuracion, autenticacion y pacientes. La conexion de las pantallas del
frontend y la integracion final de las ramas de medicos, citas y
especialidades siguen siendo tareas del equipo.
