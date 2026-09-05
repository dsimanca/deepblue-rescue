package com.deepblue.rescue.repository;

import com.deepblue.rescue.domain.Treatment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDateTime;
import java.util.List;

public interface TreatmentRepository extends JpaRepository<Treatment, Long> {

    List<Treatment> findByAnimalIdOrderByPerformedAtAsc(Long animalId);

    List<Treatment> findByAnimalAnimalCodeOrderByPerformedAtAsc(String animalCode);

    @Query("""
        SELECT t
        FROM Treatment t
        WHERE t.performedAt BETWEEN :start AND :end
        ORDER BY t.performedAt ASC
    """)
    List<Treatment> findByDateRange(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );

    @Query("""
        SELECT DISTINCT t
        FROM Treatment t
        JOIN t.animal a
        JOIN a.rescueCase rc
        JOIN rc.rescueCenter c
        WHERE c.code = :centerCode
    """)
    List<Treatment> findByRescueCenterCode(@Param("centerCode") String centerCode);

    @Query("""
        SELECT DISTINCT t
        FROM Treatment t
        JOIN t.specialist s
        JOIN s.expertiseAreas e
        WHERE LOWER(e.name) = LOWER(:expertise)
    """)
    List<Treatment> findBySpecialistExpertise(@Param("expertise") String expertise);
}
