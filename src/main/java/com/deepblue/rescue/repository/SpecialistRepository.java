package com.deepblue.rescue.repository;

import com.deepblue.rescue.domain.Specialist;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface SpecialistRepository extends JpaRepository<Specialist, Long> {

    @Query("""
        SELECT DISTINCT s
        FROM Specialist s
        JOIN s.expertiseAreas e
        WHERE LOWER(e.name) = LOWER(:name) AND s.active = true
        ORDER BY s.lastName
    """)
    List<Specialist> findActiveByExpertise(@Param("name") String name);
}
