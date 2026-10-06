package com.deepblue.rescue.service;

import com.deepblue.rescue.domain.Animal;
import com.deepblue.rescue.domain.RescueStatus;
import com.deepblue.rescue.domain.Specialist;
import com.deepblue.rescue.domain.Treatment;
import com.deepblue.rescue.dto.request.CreateTreatmentRequest;
import com.deepblue.rescue.dto.response.TreatmentResponse;
import com.deepblue.rescue.exception.BusinessRuleException;
import com.deepblue.rescue.exception.ResourceNotFoundException;
import com.deepblue.rescue.repository.AnimalRepository;
import com.deepblue.rescue.repository.SpecialistRepository;
import com.deepblue.rescue.repository.TreatmentRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class TreatmentServiceImpl implements TreatmentService {

    private final TreatmentRepository treatmentRepository;
    private final AnimalRepository animalRepository;
    private final SpecialistRepository specialistRepository;

    public TreatmentServiceImpl(TreatmentRepository treatmentRepository,
                                AnimalRepository animalRepository,
                                SpecialistRepository specialistRepository) {
        this.treatmentRepository = treatmentRepository;
        this.animalRepository = animalRepository;
        this.specialistRepository = specialistRepository;
    }

    @Override
    public TreatmentResponse register(CreateTreatmentRequest request) {
        if (request == null) {
            throw new BusinessRuleException("Treatment request is required");
        }

        Animal animal = animalRepository.findByAnimalCode(request.animalCode())
                .orElseThrow(() -> new ResourceNotFoundException("Animal not found: " + request.animalCode()));

        Specialist specialist = specialistRepository.findByProfessionalCode(request.specialistCode())
                .orElseThrow(() -> new ResourceNotFoundException("Specialist not found: " + request.specialistCode()));

        if (Boolean.FALSE.equals(specialist.getActive())) {
            throw new BusinessRuleException("Specialist is inactive: " + specialist.getProfessionalCode());
        }

        if (animal.getRescueCase() == null) {
            throw new BusinessRuleException("Animal is not associated with a rescue case");
        }

        if (animal.getRescueCase().getStatus() == RescueStatus.RELEASED
                || animal.getRescueCase().getStatus() == RescueStatus.CLOSED) {
            throw new BusinessRuleException("Released animals cannot receive treatments");
        }

        LocalDateTime performedAt = request.performedAt();
        if (performedAt.isBefore(animal.getRescueCase().getRescueDate().atStartOfDay())) {
            throw new BusinessRuleException("Treatment date cannot be earlier than the rescue date");
        }

        Treatment treatment = new Treatment(performedAt, request.type(), request.description());
        treatment.setAnimal(animal);
        treatment.setSpecialist(specialist);
        Animal savedAnimal = animalRepository.save(animal);
        savedAnimal.addTreatment(treatment);
        Treatment savedTreatment = treatmentRepository.save(treatment);

        return toResponse(savedTreatment);
    }

    @Override
    public List<TreatmentResponse> findByAnimalCode(String animalCode) {
        animalRepository.findByAnimalCode(animalCode)
                .orElseThrow(() -> new ResourceNotFoundException("Animal not found: " + animalCode));

        return treatmentRepository.findByAnimalAnimalCodeOrderByPerformedAtAsc(animalCode)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private TreatmentResponse toResponse(Treatment treatment) {
        if (treatment == null) {
            return null;
        }

        return new TreatmentResponse(
                treatment.getId(),
                treatment.getAnimal() != null ? treatment.getAnimal().getAnimalCode() : null,
                treatment.getSpecialist() != null ? treatment.getSpecialist().getProfessionalCode() : null,
                treatment.getPerformedAt(),
                treatment.getType(),
                treatment.getDescription()
        );
    }
}
