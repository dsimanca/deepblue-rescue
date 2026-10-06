package com.deepblue.rescue.service;

import com.deepblue.rescue.domain.Animal;
import com.deepblue.rescue.domain.RescueCase;
import com.deepblue.rescue.domain.RescueStatus;
import com.deepblue.rescue.dto.response.AnimalResponse;
import com.deepblue.rescue.exception.ResourceNotFoundException;
import com.deepblue.rescue.repository.AnimalRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AnimalServiceImpl implements AnimalService {

    private final AnimalRepository animalRepository;

    public AnimalServiceImpl(AnimalRepository animalRepository) {
        this.animalRepository = animalRepository;
    }

    @Override
    public AnimalResponse findByCode(String animalCode) {
        Animal animal = animalRepository.findByAnimalCode(animalCode)
                .orElseThrow(() -> new ResourceNotFoundException("Animal not found: " + animalCode));
        return toResponse(animal);
    }

    @Override
    public List<AnimalResponse> findAnimalsInRehabilitation() {
        return animalRepository.findByRescueCaseStatus(RescueStatus.IN_REHABILITATION)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    public boolean canReceiveTreatment(String animalCode) {
        Animal animal = animalRepository.findByAnimalCode(animalCode)
                .orElseThrow(() -> new ResourceNotFoundException("Animal not found: " + animalCode));

        RescueCase rescueCase = animal.getRescueCase();
        return rescueCase != null
                && rescueCase.getStatus() != RescueStatus.RELEASED
                && rescueCase.getStatus() != RescueStatus.CLOSED;
    }

    private AnimalResponse toResponse(Animal animal) {
        if (animal == null) {
            return null;
        }

        RescueCase rescueCase = animal.getRescueCase();
        RescueStatus rescueStatus = rescueCase != null ? rescueCase.getStatus() : null;
        String rescueCaseCode = rescueCase != null ? rescueCase.getCaseCode() : null;
        String centerCode = rescueCase != null && rescueCase.getRescueCenter() != null
                ? rescueCase.getRescueCenter().getCode()
                : null;

        return new AnimalResponse(
                animal.getId(),
                animal.getAnimalCode(),
                animal.getCommonName(),
                animal.getScientificName(),
                animal.getSex(),
                rescueCaseCode,
                rescueStatus,
                centerCode
        );
    }
}
