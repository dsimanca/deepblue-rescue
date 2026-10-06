package com.deepblue.rescue.service;

import com.deepblue.rescue.domain.RescueCase;
import com.deepblue.rescue.domain.RescueStatus;
import com.deepblue.rescue.dto.request.ChangeRescueStatusRequest;
import com.deepblue.rescue.dto.response.RescueCaseResponse;
import com.deepblue.rescue.exception.BusinessRuleException;
import com.deepblue.rescue.exception.ResourceNotFoundException;
import com.deepblue.rescue.repository.RescueCaseRepository;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;

@Service
public class RescueCaseServiceImpl implements RescueCaseService {

    private final RescueCaseRepository rescueCaseRepository;

    public RescueCaseServiceImpl(RescueCaseRepository rescueCaseRepository) {
        this.rescueCaseRepository = rescueCaseRepository;
    }

    @Override
    public RescueCaseResponse findByCode(String caseCode) {
        RescueCase rescueCase = rescueCaseRepository.findByCaseCode(caseCode)
                .orElseThrow(() -> new ResourceNotFoundException("Rescue case not found: " + caseCode));
        return toResponse(rescueCase);
    }

    @Override
    public List<RescueCaseResponse> findByStatus(RescueStatus status) {
        return rescueCaseRepository.findByStatusOrderByRescueDateAsc(status)
                .stream()
                .sorted(Comparator.comparing(RescueCase::getRescueDate).thenComparing(RescueCase::getCaseCode))
                .map(this::toResponse)
                .toList();
    }

    @Override
    public RescueCaseResponse changeStatus(String caseCode, ChangeRescueStatusRequest request) {
        if (request == null || request.status() == null) {
            throw new BusinessRuleException("Status is required");
        }

        RescueCase rescueCase = rescueCaseRepository.findByCaseCode(caseCode)
                .orElseThrow(() -> new ResourceNotFoundException("Rescue case not found: " + caseCode));

        RescueStatus currentStatus = rescueCase.getStatus();
        RescueStatus nextStatus = request.status();

        if (currentStatus == RescueStatus.RELEASED || currentStatus == RescueStatus.CLOSED) {
            throw new BusinessRuleException("Cannot change the status of a closed rescue case");
        }

        if (!isValidTransition(currentStatus, nextStatus)) {
            throw new BusinessRuleException("Invalid status transition: " + currentStatus + " -> " + nextStatus);
        }

        rescueCase.setStatus(nextStatus);
        rescueCaseRepository.save(rescueCase);
        return toResponse(rescueCase);
    }

    private boolean isValidTransition(RescueStatus currentStatus, RescueStatus nextStatus) {
        if (currentStatus == null || nextStatus == null) {
            return false;
        }

        return switch (currentStatus) {
            case ADMITTED -> nextStatus == RescueStatus.UNDER_EVALUATION;
            case UNDER_EVALUATION -> nextStatus == RescueStatus.IN_REHABILITATION;
            case IN_REHABILITATION -> nextStatus == RescueStatus.READY_FOR_RELEASE || nextStatus == RescueStatus.RELEASED;
            case READY_FOR_RELEASE -> nextStatus == RescueStatus.RELEASED;
            case RELEASED -> false;
            case CLOSED -> false;
        };
    }

    private RescueCaseResponse toResponse(RescueCase rescueCase) {
        if (rescueCase == null) {
            return null;
        }

        String centerCode = rescueCase.getRescueCenter() != null ? rescueCase.getRescueCenter().getCode() : null;
        String animalCode = rescueCase.getAnimal() != null ? rescueCase.getAnimal().getAnimalCode() : null;

        return new RescueCaseResponse(
                rescueCase.getId(),
                rescueCase.getCaseCode(),
                rescueCase.getRescueDate(),
                rescueCase.getRescueLocation(),
                rescueCase.getStatus(),
                centerCode,
                animalCode
        );
    }
}
