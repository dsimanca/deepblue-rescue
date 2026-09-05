# DeepBlue Rescue

DeepBlue Rescue es un proyecto de persistencia para registrar centros de rescate, casos, animales marinos, expedientes medicos, especialistas, areas de experiencia y tratamientos.

El alcance del proyecto es exclusivamente la capa de persistencia con Java 21, Spring Boot 4.1.x, Spring Data JPA, Hibernate, Flyway, PostgreSQL y Testcontainers.

## Modelo De Datos

Tablas principales:

- `rescue_centers`
- `rescue_cases`
- `animals`
- `medical_records`
- `specialists`
- `expertise`
- `treatments`

Tabla asociativa:

- `specialist_expertise`

Migraciones:

- `V1__create_schema.sql`: crea tablas, PK, FK, UNIQUE, CHECK e indices.
- `V2__insert_expertise_catalog.sql`: inserta el catalogo base de expertise.
- `V3__add_tracking_device_to_animal.sql`: agrega `tracking_device_code` a `animals`.

## Relaciones

- `RescueCenter` 1:N `RescueCase`.
- `RescueCase` 1:1 `Animal`.
- `Animal` 1:1 `MedicalRecord`.
- `Specialist` N:M `Expertise`.
- `Animal` 1:N `Treatment`.
- `Specialist` 1:N `Treatment`.

Las relaciones bidireccionales usan metodos de ayuda como `addCase`, `assignAnimal`, `assignMedicalRecord` y `addExpertise` para mantener sincronizados ambos lados.

## Ejecucion

Requisitos:

- JDK 21.
- Maven Wrapper incluido.
- PostgreSQL local si se ejecuta la aplicacion fuera de tests.

Variables por defecto de la aplicacion:

```text
DB_URL=jdbc:postgresql://localhost:5432/deepblue
DB_USER=postgres
DB_PASSWORD=postgres
```

Comando en PowerShell:

```powershell
$env:JAVA_HOME='C:\Program Files\Java\jdk-21.0.10'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
.\mvnw.cmd spring-boot:run
```

## Tests

Los tests de integracion usan PostgreSQL real mediante Testcontainers. Docker debe estar instalado y en ejecucion.

Comando:

```powershell
$env:JAVA_HOME='C:\Program Files\Java\jdk-21.0.10'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
.\mvnw.cmd clean test
```

Durante la ejecucion, `docker ps` debe mostrar temporalmente un contenedor PostgreSQL creado por Testcontainers.

## Flyway

Flyway crea y evoluciona el esquema. Hibernate no crea tablas porque la aplicacion usa:

```yaml
spring:
  jpa:
    hibernate:
      ddl-auto: validate
```

`validate` hace que Hibernate compare las entidades JPA contra el esquema existente. Si falta una tabla o columna, la aplicacion falla en lugar de modificar la base automaticamente.

## Testcontainers

Testcontainers levanta un PostgreSQL real para pruebas de integracion. Esto permite validar comportamiento real de constraints como PK, FK, UNIQUE y CHECK sin depender de una base compartida ni de H2.

## Query Methods

- `RescueCenterRepository.findByCode`
- `RescueCaseRepository.findByCaseCode`
- `RescueCaseRepository.findByStatusOrderByRescueDateAsc`
- `RescueCaseRepository.findByRescueCenterCode`
- `RescueCaseRepository.findByRescueDateAfterOrderByRescueDateDesc`
- `AnimalRepository.findByAnimalCode`
- `AnimalRepository.findByCommonNameContainingIgnoreCase`
- `AnimalRepository.findByRescueCaseStatus`
- `AnimalRepository.findByRescueCaseRescueCenterCode`
- `ExpertiseRepository.findByNameIgnoreCase`
- `TreatmentRepository.findByAnimalIdOrderByPerformedAtAsc`
- `TreatmentRepository.findByAnimalAnimalCodeOrderByPerformedAtAsc`

## JPQL

- `SpecialistRepository.findActiveByExpertise`
- `TreatmentRepository.findByDateRange`
- `TreatmentRepository.findByRescueCenterCode`
- `TreatmentRepository.findBySpecialistExpertise`
- `AnimalRepository.findInRehabTreatedByExpertise`

Las consultas JPQL usan entidades y asociaciones Java, por ejemplo `Specialist` y `expertiseAreas`, no nombres de tablas SQL como `specialists`.

## Clasificacion De Consultas

| Necesidad | Mecanismo |
|---|---|
| Buscar una entidad por ID | Metodo heredado |
| Buscar caso por codigo | Query Method |
| Casos segun status | Query Method |
| Animales de determinado centro | Query Method |
| Especialistas segun expertise | @Query + JPQL |
| Tratamientos en intervalo | @Query + JPQL |
| Tratamientos por expertise del especialista | @Query + JPQL |

