package com.deepblue.rescue;

import com.deepblue.rescue.domain.*;
import com.deepblue.rescue.repository.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@Testcontainers
@SpringBootTest
@Transactional
class PersistenceIntegrationTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:18-alpine")
                    .withDatabaseName("deepblue_test")
                    .withUsername("deepblue")
                    .withPassword("deepblue");

    @Autowired private RescueCenterRepository centerRepo;
    @Autowired private RescueCaseRepository caseRepo;
    @Autowired private AnimalRepository animalRepo;
    @Autowired private SpecialistRepository specialistRepo;
    @Autowired private ExpertiseRepository expertiseRepo;
    @Autowired private TreatmentRepository treatmentRepo;
    @Autowired private JdbcTemplate jdbcTemplate;

    @Test
    void testFlywayMigrationsExecuted() {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM flyway_schema_history", Integer.class);
        assertTrue(count >= 2);
    }

    @Test
    void testInheritedMethods() {
        RescueCenter center = new RescueCenter("DB-CAR", "DeepBlue Caribbean", "Santa Marta");
        centerRepo.save(center);

        assertNotNull(center.getId());
        assertTrue(centerRepo.existsById(center.getId()));
        assertEquals(1, centerRepo.count());

        Optional<RescueCenter> found = centerRepo.findById(center.getId());
        assertTrue(found.isPresent());
        assertEquals("DB-CAR", found.get().getCode());
    }

    @Test
    void testOneToManyRelationship() {
        RescueCenter center = new RescueCenter("DB-CAR", "DeepBlue Caribbean", "Santa Marta");
        RescueCase case1 = new RescueCase("RES-001", LocalDate.now(), "Beach", RescueStatus.ADMITTED);
        RescueCase case2 = new RescueCase("RES-002", LocalDate.now(), "Ocean", RescueStatus.ADMITTED);

        center.addCase(case1);
        center.addCase(case2);
        centerRepo.save(center);

        assertEquals(2, center.getCases().size());
        assertEquals(center, case1.getRescueCenter());
    }

    @Test
    void testOneToOneRescueCaseAnimal() {
        RescueCenter center = new RescueCenter("DB-CAR", "DeepBlue Caribbean", "Santa Marta");
        RescueCase rescueCase = new RescueCase("RES-2026-001", LocalDate.now(), "Bahía", RescueStatus.ADMITTED);
        Animal animal = new Animal("AN-2026-001", "Green Sea Turtle", "Chelonia mydas", AnimalSex.FEMALE);

        center.addCase(rescueCase);
        rescueCase.assignAnimal(animal);
        centerRepo.save(center);

        assertNotNull(rescueCase.getAnimal());
        assertNotNull(animal.getRescueCase());
    }

    @Test
    void testOneToOneAnimalMedicalRecord() {
        RescueCenter center = new RescueCenter("DB-PAC", "Pacific Center", "Lima");
        RescueCase rescueCase = new RescueCase("RES-003", LocalDate.now(), "Coast", RescueStatus.ADMITTED);
        Animal animal = new Animal("AN-2026-002", "Dolphin", "Tursiops truncatus", AnimalSex.MALE);

        center.addCase(rescueCase);
        rescueCase.assignAnimal(animal);

        MedicalRecord record = new MedicalRecord(
                new BigDecimal("28.40"), "STABLE", "Left flipper injury", "Observations"
        );
        animal.assignMedicalRecord(record);
        centerRepo.save(center);

        assertNotNull(animal.getId());
        assertNotNull(record.getId());
    }

    @Test
    void testManyToManySpecialistExpertise() {
        Expertise trauma = expertiseRepo.findByNameIgnoreCase("Trauma").orElseThrow();
        Expertise rehab = expertiseRepo.findByNameIgnoreCase("Rehabilitation").orElseThrow();

        Specialist specialist = new Specialist("SPEC-001", "Elena", "Vargas", "elena@deepblue.org");
        specialist.addExpertise(trauma);
        specialist.addExpertise(rehab);

        specialistRepo.save(specialist);

        assertEquals(2, specialist.getExpertiseAreas().size());
    }

    @Test
    void testQueryMethodByStatus() {
        RescueCenter center = new RescueCenter("DB-TEST", "Test Center", "Test City");
        RescueCase case1 = new RescueCase("RES-100", LocalDate.now(), "Loc1", RescueStatus.IN_REHABILITATION);
        RescueCase case2 = new RescueCase("RES-101", LocalDate.now(), "Loc2", RescueStatus.READY_FOR_RELEASE);
        RescueCase case3 = new RescueCase("RES-102", LocalDate.now(), "Loc3", RescueStatus.IN_REHABILITATION);

        center.addCase(case1);
        center.addCase(case2);
        center.addCase(case3);
        centerRepo.save(center);

        List<RescueCase> inRehab = caseRepo.findByStatusOrderByRescueDateAsc(RescueStatus.IN_REHABILITATION);
        assertEquals(2, inRehab.size());
    }

    @Test
    void testQueryMethodNavigatingRelations() {
        RescueCenter car = new RescueCenter("DB-CAR", "Caribbean", "Santa Marta");
        RescueCenter pac = new RescueCenter("DB-PAC", "Pacific", "Lima");

        RescueCase case1 = new RescueCase("RES-200", LocalDate.now(), "Loc1", RescueStatus.ADMITTED);
        Animal animal1 = new Animal("AN-200", "Turtle", "Chelonia mydas", AnimalSex.FEMALE);
        car.addCase(case1);
        case1.assignAnimal(animal1);

        RescueCase case2 = new RescueCase("RES-201", LocalDate.now(), "Loc2", RescueStatus.ADMITTED);
        Animal animal2 = new Animal("AN-201", "Dolphin", "Tursiops", AnimalSex.MALE);
        pac.addCase(case2);
        case2.assignAnimal(animal2);

        centerRepo.save(car);
        centerRepo.save(pac);

        List<Animal> carAnimals = animalRepo.findByRescueCaseRescueCenterCode("DB-CAR");
        assertEquals(1, carAnimals.size());
    }

    @Test
    void testJPQLSpecialistsByExpertise() {
        Expertise trauma = expertiseRepo.findByNameIgnoreCase("Trauma").orElseThrow();
        Expertise rehab = expertiseRepo.findByNameIgnoreCase("Rehabilitation").orElseThrow();
        Expertise mammals = expertiseRepo.findByNameIgnoreCase("Marine Mammals").orElseThrow();

        Specialist elena = new Specialist("SPEC-001", "Elena", "Vargas", "elena@test.com");
        elena.addExpertise(trauma);
        elena.addExpertise(rehab);

        Specialist mateo = new Specialist("SPEC-002", "Mateo", "Lopez", "mateo@test.com");
        mateo.addExpertise(mammals);
        mateo.addExpertise(rehab);

        Specialist sofia = new Specialist("SPEC-003", "Sofia", "Torres", "sofia@test.com");
        sofia.addExpertise(trauma);

        specialistRepo.save(elena);
        specialistRepo.save(mateo);
        specialistRepo.save(sofia);

        List<Specialist> traumaExperts = specialistRepo.findActiveByExpertise("trauma");
        assertEquals(2, traumaExperts.size());
    }

    @Test
    void testTreatmentsChronological() {
        RescueCenter center = new RescueCenter("DB-TEST", "Test", "City");
        RescueCase rescueCase = new RescueCase("RES-300", LocalDate.now(), "Loc", RescueStatus.ADMITTED);
        Animal animal = new Animal("AN-300", "Turtle", "Test", AnimalSex.FEMALE);
        center.addCase(rescueCase);
        rescueCase.assignAnimal(animal);
        centerRepo.save(center);

        Specialist elena = new Specialist("SPEC-100", "Elena", "V", "e@test.com");
        Specialist mateo = new Specialist("SPEC-101", "Mateo", "L", "m@test.com");
        specialistRepo.save(elena);
        specialistRepo.save(mateo);

        Treatment t1 = new Treatment(LocalDateTime.of(2026, 8, 1, 10, 0), TreatmentType.WOUND_CARE, "Desc1");
        Treatment t2 = new Treatment(LocalDateTime.of(2026, 8, 5, 10, 0), TreatmentType.HYDRATION, "Desc2");
        Treatment t3 = new Treatment(LocalDateTime.of(2026, 8, 10, 10, 0), TreatmentType.OBSERVATION, "Desc3");

        t1.setSpecialist(elena);
        t2.setSpecialist(elena);
        t3.setSpecialist(mateo);

        animal.addTreatment(t1);
        animal.addTreatment(t2);
        animal.addTreatment(t3);

        animalRepo.save(animal);

        List<Treatment> treatments = treatmentRepo.findByAnimalIdOrderByPerformedAtAsc(animal.getId());
        assertEquals(3, treatments.size());
    }

    @Test
    void testJPQLDateRange() {
        RescueCenter center = new RescueCenter("DB-TEST2", "Test2", "City2");
        RescueCase rescueCase = new RescueCase("RES-400", LocalDate.now(), "Loc", RescueStatus.ADMITTED);
        Animal animal = new Animal("AN-400", "Turtle", "Test", AnimalSex.FEMALE);
        center.addCase(rescueCase);
        rescueCase.assignAnimal(animal);
        centerRepo.save(center);

        Specialist spec = new Specialist("SPEC-200", "Test", "Spec", "t@test.com");
        specialistRepo.save(spec);

        Treatment t1 = new Treatment(LocalDateTime.of(2026, 8, 1, 10, 0), TreatmentType.WOUND_CARE, "D1");
        Treatment t2 = new Treatment(LocalDateTime.of(2026, 8, 10, 10, 0), TreatmentType.HYDRATION, "D2");
        Treatment t3 = new Treatment(LocalDateTime.of(2026, 8, 20, 10, 0), TreatmentType.OBSERVATION, "D3");

        t1.setSpecialist(spec);
        t2.setSpecialist(spec);
        t3.setSpecialist(spec);

        animal.addTreatment(t1);
        animal.addTreatment(t2);
        animal.addTreatment(t3);
        animalRepo.save(animal);

        List<Treatment> treatments = treatmentRepo.findByDateRange(
                LocalDateTime.of(2026, 8, 5, 0, 0),
                LocalDateTime.of(2026, 8, 15, 23, 59)
        );

        assertEquals(1, treatments.size());
    }

    @Test
    void testForeignKeyConstraint() {
        assertThrows(DataIntegrityViolationException.class, () -> {
            jdbcTemplate.update("""
                    INSERT INTO rescue_cases (
                        case_code,
                        rescue_date,
                        rescue_location,
                        status,
                        rescue_center_id
                    )
                    VALUES (?, ?, ?, ?, ?)
                    """,
                    "RES-FK-001",
                    LocalDate.now(),
                    "Invalid center",
                    RescueStatus.ADMITTED.name(),
                    -1L
            );
        });
    }

    @Test
    void testCheckConstraint() {
        RescueCenter center = centerRepo.saveAndFlush(
                new RescueCenter("DB-CHECK", "Check Center", "Check City")
        );

        assertThrows(DataIntegrityViolationException.class, () -> {
            jdbcTemplate.update("""
                    INSERT INTO rescue_cases (
                        case_code,
                        rescue_date,
                        rescue_location,
                        status,
                        rescue_center_id
                    )
                    VALUES (?, ?, ?, ?, ?)
                    """,
                    "RES-CHECK-001",
                    LocalDate.now(),
                    "Invalid status",
                    "INVALID_STATUS",
                    center.getId()
            );
        });
    }

    @Test
    void testUniqueConstraint() {
        RescueCenter center = new RescueCenter("DB-UNIQUE", "Test", "City");
        RescueCase case1 = new RescueCase("RES-500", LocalDate.now(), "Loc", RescueStatus.ADMITTED);
        RescueCase case2 = new RescueCase("RES-501", LocalDate.now(), "Loc", RescueStatus.ADMITTED);

        Animal animal1 = new Animal("AN-100", "Turtle", "Test", AnimalSex.FEMALE);
        Animal animal2 = new Animal("AN-100", "Dolphin", "Test", AnimalSex.MALE);

        center.addCase(case1);
        center.addCase(case2);
        case1.assignAnimal(animal1);
        case2.assignAnimal(animal2);

        assertThrows(DataIntegrityViolationException.class, () -> {
            centerRepo.saveAndFlush(center);
        });
    }

    @Test
    void testIntegrationChallenge() {
        RescueCenter center = new RescueCenter("DB-CAR", "DeepBlue Caribbean", "Santa Marta");

        RescueCase rescueCase = new RescueCase(
                "RES-2026-100",
                LocalDate.of(2026, 8, 18),
                "Bahía Concha",
                RescueStatus.IN_REHABILITATION
        );

        Animal animal = new Animal(
                "AN-2026-100",
                "Green Sea Turtle",
                "Chelonia mydas",
                AnimalSex.FEMALE
        );

        MedicalRecord record = new MedicalRecord(
                new BigDecimal("27.80"),
                "STABLE",
                "Injury caused by fishing net",
                "Possible plastic ingestion"
        );

        Specialist elena = new Specialist("SPEC-001", "Elena", "Vargas", "elena@deepblue.org");
        Expertise reptiles = expertiseRepo.findByNameIgnoreCase("Marine Reptiles").orElseThrow();
        Expertise trauma = expertiseRepo.findByNameIgnoreCase("Trauma").orElseThrow();
        Expertise rehab = expertiseRepo.findByNameIgnoreCase("Rehabilitation").orElseThrow();

        elena.addExpertise(reptiles);
        elena.addExpertise(trauma);
        elena.addExpertise(rehab);

        Treatment t1 = new Treatment(
                LocalDateTime.of(2026, 8, 18, 14, 0),
                TreatmentType.WOUND_CARE,
                "Cleaning of left front flipper"
        );
        Treatment t2 = new Treatment(
                LocalDateTime.of(2026, 8, 19, 10, 0),
                TreatmentType.HYDRATION,
                "Subcutaneous fluid therapy"
        );

        t1.setSpecialist(elena);
        t2.setSpecialist(elena);

        center.addCase(rescueCase);
        rescueCase.assignAnimal(animal);
        animal.assignMedicalRecord(record);
        animal.addTreatment(t1);
        animal.addTreatment(t2);

        specialistRepo.save(elena);
        centerRepo.save(center);

        assertTrue(caseRepo.findByCaseCode("RES-2026-100").isPresent());
        assertFalse(caseRepo.findByStatusOrderByRescueDateAsc(RescueStatus.IN_REHABILITATION).isEmpty());
        assertEquals(1, animalRepo.findByRescueCaseRescueCenterCode("DB-CAR").size());
        assertFalse(animalRepo.findByCommonNameContainingIgnoreCase("turtle").isEmpty());
        assertFalse(specialistRepo.findActiveByExpertise("Trauma").isEmpty());
        assertEquals(2, treatmentRepo.findByAnimalAnimalCodeOrderByPerformedAtAsc("AN-2026-100").size());
        assertFalse(treatmentRepo.findBySpecialistExpertise("Rehabilitation").isEmpty());
        assertFalse(treatmentRepo.findByDateRange(
                LocalDateTime.of(2026, 8, 1, 0, 0),
                LocalDateTime.of(2026, 8, 31, 23, 59)
        ).isEmpty());
    }

    @Test
    void testChallengeAnimalsInRehabTreatedByTraumaSpecialist() {
        RescueCenter center = new RescueCenter("DB-CHALLENGE", "Challenge Center", "Santa Marta");
        RescueCase rescueCase = new RescueCase(
                "RES-CHALLENGE-001",
                LocalDate.now(),
                "Beach",
                RescueStatus.IN_REHABILITATION
        );
        Animal animal = new Animal("AN-CHALLENGE-001", "Sea Turtle", "Chelonia mydas", AnimalSex.FEMALE);
        Specialist specialist = new Specialist("SPEC-CHALLENGE-001", "Laura", "Reyes", "laura@deepblue.org");
        Expertise trauma = expertiseRepo.findByNameIgnoreCase("Trauma").orElseThrow();
        Treatment treatment = new Treatment(
                LocalDateTime.of(2026, 8, 20, 10, 0),
                TreatmentType.WOUND_CARE,
                "Trauma care"
        );

        specialist.addExpertise(trauma);
        specialistRepo.save(specialist);

        center.addCase(rescueCase);
        rescueCase.assignAnimal(animal);
        treatment.setSpecialist(specialist);
        animal.addTreatment(treatment);
        centerRepo.save(center);

        List<Animal> animals = animalRepo.findInRehabTreatedByExpertise(
                RescueStatus.IN_REHABILITATION,
                "trauma"
        );

        assertEquals(1, animals.size());
        assertEquals("AN-CHALLENGE-001", animals.getFirst().getAnimalCode());
    }
}
