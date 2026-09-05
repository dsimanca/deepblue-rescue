package com.deepblue.rescue.repository;

import com.deepblue.rescue.domain.Animal;
import com.deepblue.rescue.domain.RescueStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface AnimalRepository extends JpaRepository<Animal, Long> {
    Optional<Animal> findByAnimalCode(String animalCode);
    List<Animal> findByCommonNameContainingIgnoreCase(String text);
    List<Animal> findByRescueCaseStatus(RescueStatus status);
    List<Animal> findByRescueCaseRescueCenterCode(String centerCode);

    @Query("""
        SELECT DISTINCT a
        FROM Animal a
        JOIN a.rescueCase rc
        JOIN a.treatments t
        JOIN t.specialist s
        JOIN s.expertiseAreas e
        WHERE rc.status = :status
          AND LOWER(e.name) = LOWER(:expertise)
    """)
    List<Animal> findInRehabTreatedByExpertise(
            @Param("status") RescueStatus status,
            @Param("expertise") String expertise
    );
}
