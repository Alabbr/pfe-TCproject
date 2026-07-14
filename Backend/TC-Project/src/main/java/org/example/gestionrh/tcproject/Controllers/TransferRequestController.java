package org.example.gestionrh.tcproject.Controllers;

import lombok.RequiredArgsConstructor;
import org.example.gestionrh.tcproject.Dtos.request.CreateTransferRequestDto;
import org.example.gestionrh.tcproject.Entities.TransferRequest;
import org.example.gestionrh.tcproject.Entities.User;
import org.example.gestionrh.tcproject.Services.TransferRequestService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/transfers")
@RequiredArgsConstructor
public class TransferRequestController {

    private final TransferRequestService transferRequestService;

    // Retourne la liste de toutes les demandes de transfert (pour Super Admin / DG)
    @GetMapping
    @PreAuthorize("hasAuthority('ROLE_SUPER_ADMIN') or hasAuthority('ROLE_DIRECTEUR_GENERAL')")
    public ResponseEntity<List<TransferRequest>> getAllRequests() {
        return ResponseEntity.ok(transferRequestService.getAllRequests());
    }

    // Retourne uniquement les demandes de transfert en attente (pour Super Admin / DG)
    @GetMapping("/pending")
    @PreAuthorize("hasAuthority('ROLE_SUPER_ADMIN') or hasAuthority('ROLE_DIRECTEUR_GENERAL')")
    public ResponseEntity<List<TransferRequest>> getPendingRequests() {
        return ResponseEntity.ok(transferRequestService.getPendingRequests());
    }

    // Retourne les demandes de transfert créées par le chef de département connecté
    @GetMapping("/my-requests")
    @PreAuthorize("hasAuthority('ROLE_DIRECTEUR') or hasAuthority('ROLE_RESPONSABLE')")
    public ResponseEntity<List<TransferRequest>> getMyRequests(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(transferRequestService.getRequestsByDepartmentChief(user.getId()));
    }

    // Crée une nouvelle demande de transfert d'un employé vers un autre département
    @PostMapping
    @PreAuthorize("hasAuthority('ROLE_DIRECTEUR') or hasAuthority('ROLE_RESPONSABLE')")
    public ResponseEntity<TransferRequest> createRequest(
            @RequestBody CreateTransferRequestDto dto,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(transferRequestService.createTransferRequest(dto, user));
    }

    // Traite une demande de transfert (approuve ou rejette) par un Super Admin / DG
    @PutMapping("/{id}/process")
    @PreAuthorize("hasAuthority('ROLE_SUPER_ADMIN') or hasAuthority('ROLE_DIRECTEUR_GENERAL')")
    public ResponseEntity<TransferRequest> processRequest(
            @PathVariable Long id,
            @RequestParam boolean isApproved,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(transferRequestService.processTransferRequest(id, isApproved, user));
    }
}
