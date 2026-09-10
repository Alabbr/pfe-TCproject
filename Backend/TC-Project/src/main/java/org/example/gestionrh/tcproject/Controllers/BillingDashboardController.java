package org.example.gestionrh.tcproject.Controllers;

import org.example.gestionrh.tcproject.DTOs.BillingStatsDTO;
import org.example.gestionrh.tcproject.DTOs.BillingPredictionDTO;
import org.example.gestionrh.tcproject.Services.BillingDashboardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/billing")
public class BillingDashboardController {

    private final BillingDashboardService billingService;

    @Autowired
    public BillingDashboardController(BillingDashboardService billingService) {
        this.billingService = billingService;
    }

    @GetMapping("/dashboard-stats")
    public ResponseEntity<BillingStatsDTO> getDashboardStats() {
        BillingStatsDTO stats = billingService.getDashboardStats();
        return ResponseEntity.ok(stats);
    }

    @GetMapping("/prediction")
    public ResponseEntity<BillingPredictionDTO> getBillingPrediction() {
        BillingPredictionDTO prediction = billingService.getBillingPrediction();
        return ResponseEntity.ok(prediction);
    }
}
