package org.example.gestionrh.tcproject.Services;

import lombok.RequiredArgsConstructor;
import org.example.gestionrh.tcproject.Dtos.request.CreateTransferRequestDto;
import org.example.gestionrh.tcproject.Entities.*;
import org.example.gestionrh.tcproject.Repositories.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TransferRequestService {

    private final TransferRequestRepository transferRequestRepository;
    private final UserRepository userRepository;
    private final DepartmentRepository departmentRepository;
    private final JobPositionRepository jobPositionRepository;

    // Retourne toutes les demandes de transfert
    public List<TransferRequest> getAllRequests() {
        return transferRequestRepository.findAll();
    }

    // Retourne uniquement les demandes de transfert en attente (PENDING)
    public List<TransferRequest> getPendingRequests() {
        return transferRequestRepository.findByStatus(ValidationStatus.PENDING);
    }

    // Retourne les demandes de transfert créées par un chef de département spécifique
    public List<TransferRequest> getRequestsByDepartmentChief(Long chiefId) {
        return transferRequestRepository.findByRequestedBy_Id(chiefId);
    }

    // Crée une nouvelle demande de transfert d'employé vers un autre département
    @Transactional
    public TransferRequest createTransferRequest(CreateTransferRequestDto dto, User requestedBy) {
        User employee = userRepository.findById(dto.getEmployeeId())
                .orElseThrow(() -> new RuntimeException("Employé introuvable"));
        Department targetDept = departmentRepository.findById(dto.getTargetDepartmentId())
                .orElseThrow(() -> new RuntimeException("Département cible introuvable"));

        JobPosition newJob = null;
        if (dto.getNewJobPositionId() != null) {
            newJob = jobPositionRepository.findById(dto.getNewJobPositionId())
                    .orElseThrow(() -> new RuntimeException("Poste introuvable"));
        }

        TransferRequest request = TransferRequest.builder()
                .employee(employee)
                .targetDepartment(targetDept)
                .newJobPosition(newJob)
                .requestedBy(requestedBy)
                .comment(dto.getComment())
                .build();

        return transferRequestRepository.save(request);
    }

    // Traite une demande de transfert : si approuvée, déplace l'employé vers le nouveau département
    @Transactional
    public TransferRequest processTransferRequest(Long requestId, boolean isApproved, User decidedBy) {
        TransferRequest request = transferRequestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Demande de transfert introuvable"));

        request.setDecisionDate(LocalDateTime.now());
        request.setDecidedBy(decidedBy);

        if (isApproved) {
            request.setStatus(ValidationStatus.VALIDATED);
            // Apply changes to the user
            User employee = request.getEmployee();
            employee.setDepartment(request.getTargetDepartment());
            if (request.getNewJobPosition() != null) {
                employee.setJobPosition(request.getNewJobPosition());
            }
            userRepository.save(employee);
        } else {
            request.setStatus(ValidationStatus.REJECTED);
        }

        return transferRequestRepository.save(request);
    }
}
