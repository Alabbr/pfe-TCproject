package org.example.gestionrh.tcproject.Controllers;

import org.example.gestionrh.tcproject.Entities.InterimDelegation;
import org.example.gestionrh.tcproject.Entities.User;
import org.example.gestionrh.tcproject.Services.InterimService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/interim")
public class InterimController {

    private final InterimService interimService;

    public InterimController(InterimService interimService) {
        this.interimService = interimService;
    }

    // Assigne un intérimaire pour remplacer l'utilisateur connecté (chef) pendant une période donnée
    @PostMapping("/assign")
    public ResponseEntity<?> assignInterim(@AuthenticationPrincipal User currentUser,
                                           @RequestParam Long interimUserId,
                                           @RequestParam String startDate,
                                           @RequestParam String endDate) {
        try {
            LocalDate start = LocalDate.parse(startDate);
            LocalDate end = LocalDate.parse(endDate);
            InterimDelegation delegation = interimService.assignInterim(currentUser.getId(), interimUserId, start, end);
            return ResponseEntity.ok(mapToDto(delegation));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error assigning interim: " + e.getMessage());
        }
    }

    // Retourne les délégations actives où l'utilisateur connecté est le délégant (le chef remplacé)
    @GetMapping("/active")
    public ResponseEntity<?> getActiveDelegations(@AuthenticationPrincipal User currentUser) {
        List<InterimDelegation> dels = interimService.getActiveDelegationsForChef(currentUser.getId());
        return ResponseEntity.ok(dels.stream().map(this::mapToDto).collect(Collectors.toList()));
    }

    // Révoque une délégation d'intérim avant sa date de fin
    @PostMapping("/revoke/{id}")
    public ResponseEntity<?> revokeDelegation(@PathVariable Long id) {
        interimService.revokeDelegation(id);
        return ResponseEntity.ok("Delegation revoked successfully.");
    }

    // Retourne les délégations actives où l'utilisateur connecté agit comme remplaçant (intérimaire)
    @GetMapping("/my-delegation")
    public ResponseEntity<?> getMyInterimDelegation(@AuthenticationPrincipal User currentUser) {
        List<InterimDelegation> dels = interimService.getActiveDelegationsForInterim(currentUser.getId());
        if (dels.isEmpty()) {
            return ResponseEntity.ok(java.util.Collections.emptyList());
        }
        List<Map<String, Object>> result = dels.stream().map(d -> {
            Map<String, Object> dto = mapToDto(d);
            // Add department info for the interim employee's UI
            if (d.getDelegator().getDepartment() != null) {
                dto.put("departmentId", d.getDelegator().getDepartment().getId());
                dto.put("departmentName", d.getDelegator().getDepartment().getName());
            }
            return dto;
        }).collect(Collectors.toList());
        return ResponseEntity.ok(result);
    }

    private Map<String, Object> mapToDto(InterimDelegation d) {
        Map<String, Object> dto = new HashMap<>();
        dto.put("id", d.getId());
        dto.put("startDate", d.getStartDate().toString());
        dto.put("endDate", d.getEndDate().toString());
        dto.put("isActive", d.isActive());

        Map<String, Object> delegator = new HashMap<>();
        delegator.put("id", d.getDelegator().getId());
        delegator.put("firstName", d.getDelegator().getFirstName());
        delegator.put("lastName", d.getDelegator().getLastName());
        dto.put("delegator", delegator);

        Map<String, Object> interim = new HashMap<>();
        interim.put("id", d.getInterim().getId());
        interim.put("firstName", d.getInterim().getFirstName());
        interim.put("lastName", d.getInterim().getLastName());
        dto.put("interim", interim);

        return dto;
    }
}
