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
por la base compartida. Swagger muestra ambas operaciones en la etiqueta
`Estadisticas`.

## Fechas y significado de los conteos

| Parametro | Comportamiento |
| --- | --- |
| `from` | Fecha inicial inclusiva. Si falta, se usan los 30 dias terminados en `to`. |
| `to` | Fecha final inclusiva. Si falta, se usa la fecha actual UTC. |
| `groupBy` | `day` por defecto; admite `status` o `specialty`, sin distinguir mayusculas. |

El rango admite hasta 366 dias inclusivos, desde `1000-01-01` hasta
`9999-12-30`. Un rango invertido, una fecha invalida o una agrupacion
desconocida devuelven 400. La fecha final deja espacio para calcular el
limite superior exclusivo en SQL.

Se filtra por `citas.start_at`, la fecha de atencion, usando
`start_at >= from 00:00` y `start_at < dia_siguiente_a_to 00:00`. La fecha
de creacion de la reserva y su hora de fin no determinan el periodo.

Las citas se almacenan como `DATETIME` sin offset. El modulo mantiene la
convencion UTC de `ClockConfiguration` y la conexion MySQL de la base.
Los limites se envian como `LocalDateTime` para no desplazarlos por la zona
horaria de la JVM. La respuesta declara `timeZone: "UTC"`.

- `totalAppointments` y `appointmentsByStatus` corresponden al rango.
- `totalPatients`, `totalDoctors` y `totalSpecialties` son cantidades globales
  de registros, incluidos los inactivos. No representan altas del periodo.
- Se cuentan todos los estados, incluidas las citas canceladas.
- La especialidad se obtiene del medico actual. No existe una copia de la
  especialidad historica en la cita.

El resumen se consulta en una transaccion de solo lectura con aislamiento
`REPEATABLE_READ`, para conservar la misma vista entre el total y los estados.

## Resumen del dashboard

```http
GET /api/v1/statistics/dashboard?from=2026-10-01&to=2026-10-23
Authorization: Bearer <JWT_ADMIN>
```

Ejemplo con los datos de las pruebas:

```json
{
  "success": true,
  "message": "OK",
  "data": {
    "from": "2026-10-01",
    "to": "2026-10-23",
    "timeZone": "UTC",
    "totalAppointments": 4,
    "totalPatients": 2,
    "totalDoctors": 2,
    "totalSpecialties": 3,
    "appointmentsByStatus": {
      "PENDING": 1,
      "CONFIRMED": 1,
      "CANCELLED": 1,
      "COMPLETED": 1
    }
  },
  "timestamp": "2026-10-23T12:00:00Z"
}
```

## Series para graficos

```http
GET /api/v1/statistics/appointments?groupBy=status&from=2026-10-01&to=2026-10-23
Authorization: Bearer <JWT_ADMIN>
```

El campo `data` del sobre comun tiene esta forma:

```json
{
  "groupBy": "status",
  "from": "2026-10-01",
  "to": "2026-10-23",
  "timeZone": "UTC",
  "labels": ["PENDING", "CONFIRMED", "CANCELLED", "COMPLETED"],
  "values": [1, 1, 1, 1]
}
```

`labels[i]` corresponde a `values[i]`. Los valores son conteos enteros.

- `day`: etiquetas ISO ordenadas por fecha; incluye todos los dias del rango
  y completa los dias sin citas con cero.
- `status`: mantiene el orden de los cuatro estados del contrato de citas,
  incluso con cero registros. Si se agrega un estado en la BD, se conserva
  su conteo al final de la respuesta.
- `specialty`: solo especialidades con citas en el rango, incluidas las
  inactivas con historial. Orden: cantidad descendente y nombre ascendente
  para desempatar. Sin citas devuelve dos listas vacias.

El frontend puede construir los objetos para Recharts desde los pares
etiqueta/valor y traducir los estados a nombres visibles. Los porcentajes
se calculan sobre la suma de `values`, contemplando el caso de suma cero.

Las pantallas actuales del frontend usan arreglos locales. Su conexion a
esta API queda pendiente. La serie mensual puede derivarse de `day`; el
ranking por medico y las proximas citas no forman parte de estos endpoints.

## Permisos y errores

| Situacion | Respuesta |
| --- | --- |
| JWT ausente, invalido o cuenta inactiva | 401 |
| JWT valido con rol `PATIENT` o `DOCTOR` | 403 |
| JWT valido con rol `ADMIN` | 200 |
| Fechas o agrupacion invalidas | 400 con `ApiError` |

Las respuestas no serializan entidades JPA ni contienen datos personales.
Los filtros se pasan como parametros SQL; `groupBy` selecciona una consulta
predefinida.

## Migraciones y pruebas

`V4__indexar_citas_para_estadisticas.sql` agrega un indice sobre
`citas(start_at, status, doctor_id)` para las lecturas por periodo. Las
migraciones previas de otros integrantes conservan su contenido.

Ejecutar con Java 21 y Maven 3.9+:

```powershell
mvn test
mvn package
```

La suite usa H2 en modo MySQL y aplica las migraciones Flyway reales. Cubre
limites inclusivos, fechas extremas, UTC, datos vacios, varios estados,
historial inactivo, joins y permisos mediante JWT reales. El fixture solo
existe en `src/test/resources` y cada prueba revierte sus datos.

La validacion con una instancia MySQL del equipo queda pendiente. Si esa
base ejecuto el `V3` antiguo de la rama de citas, debe coordinarse su
migracion con el integrante 7: el `V3` de configuracion tiene otras claves
foraneas y otro checksum. Esta rama conserva ese historial.

Las ramas de medicos y citas aun requieren integracion del equipo. Antes
del PR a `dev`, revisar tambien que el numero `V4` siga disponible. La
implementacion de estadisticas consulta el esquema compartido y puede
funcionar sin duplicar sus entidades.

Validacion realizada el 23 de octubre de 2026 con Java 21:
`mvn clean verify` completo 117 pruebas sin fallos ni errores y genero el
JAR ejecutable. Incluye 39 pruebas de la base y 78 del modulo de estadisticas.
