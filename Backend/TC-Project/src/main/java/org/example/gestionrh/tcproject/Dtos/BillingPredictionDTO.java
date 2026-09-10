package org.example.gestionrh.tcproject.DTOs;

import lombok.Data;
import java.util.List;

@Data
public class BillingPredictionDTO {
    private int nextYear;
    
    // Predictions
    private double predictedGlobal;
    private double predictedStock;
    private double predictedFlux;
    private double predictedPonctuel;
    
    // Last complete year values (as reference)
    private double lastYearGlobal;
    private double lastYearStock;
    private double lastYearFlux;
    private double lastYearPonctuel;
    
    // Evolution percentages (relative to last year)
    private double evolutionGlobalPercentage;
    private double evolutionStockPercentage;
    private double evolutionFluxPercentage;
    private double evolutionPonctuelPercentage;
    
    // AI Cognitive analysis
    private String aiAnalysis;
    private List<String> anomalies;
    
    // Historical points for chart
    private List<HistoricalDataPoint> historicalPoints;

    @Data
    public static class HistoricalDataPoint {
        private int year;
        private double stock;
        private double flux;
        private double ponctuel;
        private double total;
        private boolean isPrediction;
    }
}
