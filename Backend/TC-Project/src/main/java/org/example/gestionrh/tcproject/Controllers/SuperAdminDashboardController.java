package org.example.gestionrh.tcproject.Controllers;

import lombok.RequiredArgsConstructor;
import org.example.gestionrh.tcproject.Dtos.response.SuperAdminDashboardStatsDto;
import org.example.gestionrh.tcproject.Services.SuperAdminDashboardService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard/super-admin")
@RequiredArgsConstructor
public class SuperAdminDashboardController {

    private final SuperAdminDashboardService dashboardService;

    // Retourne les statistiques globales pour le tableau de bord Super Admin
    @GetMapping("/stats")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('DIRECTEUR_GENERAL')")
    public ResponseEntity<SuperAdminDashboardStatsDto> getStats() {
        return ResponseEntity.ok(dashboardService.getDashboardStats());
    }
}
