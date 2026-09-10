package org.example.gestionrh.tcproject.Services;

import org.example.gestionrh.tcproject.DTOs.BillingStatsDTO;
import org.example.gestionrh.tcproject.DTOs.BillingPredictionDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

@Service
public class BillingDashboardService {

    private final JdbcTemplate jdbcTemplate;
    private final MachineLearningService mlService;
    private final GeminiService geminiService;

    @Autowired
    public BillingDashboardService(
            @Qualifier("billingJdbcTemplate") JdbcTemplate jdbcTemplate,
            MachineLearningService mlService,
            GeminiService geminiService) {
        this.jdbcTemplate = jdbcTemplate;
        this.mlService = mlService;
        this.geminiService = geminiService;
    }

    public BillingStatsDTO getDashboardStats() {
        BillingStatsDTO stats = new BillingStatsDTO();
        
        try {
            // 1. Suivi Manipulation
            BillingStatsDTO.SuiviManipulation sm = new BillingStatsDTO.SuiviManipulation();
            
            try {
                // vue_tickets_non_factures renvoie 1 ligne avec TOTAL_NON_FACTURE
                Map<String, Object> tnf = jdbcTemplate.queryForMap("SELECT TOTAL_NON_FACTURE FROM dbo.vue_tickets_non_factures");
                Double total = ((Number) tnf.get("TOTAL_NON_FACTURE")).doubleValue();
                sm.setMontantTicketsNonFacture(total);
                sm.setGenerationTickets(total > 0 ? "En cours" : "à jour");
            } catch (Exception e) {
                sm.setMontantTicketsNonFacture(0.0);
                sm.setGenerationTickets("à jour");
            }
            
            try {
                // On fait un COUNT et SUM sur la vue ponctuelle
                Map<String, Object> tp = jdbcTemplate.queryForMap("SELECT count(*) as NB, sum(MT_TOTAL) as MT FROM dbo.vue_tickets_ponctuels_non_factures");
                sm.setTicketsPonctuelsNbr(tp.get("NB") != null ? ((Number) tp.get("NB")).intValue() : 0);
                sm.setTicketsPonctuelsMdt(tp.get("MT") != null ? ((Number) tp.get("MT")).doubleValue() : 0.0);
            } catch (Exception e) {
                sm.setTicketsPonctuelsNbr(0);
                sm.setTicketsPonctuelsMdt(0.0);
            }
            
            sm.setComptabilisationFactures("à jour"); 
            DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
            sm.setLastRefresh(dtf.format(LocalDateTime.now()));
            stats.setSuiviManipulation(sm);

            // 2. Appels Fonds OST (vue_appels_fonds_ost)
            BillingStatsDTO.AppelsFondsOst ost = new BillingStatsDTO.AppelsFondsOst();
            try {
                Map<String, Object> afo = jdbcTemplate.queryForMap("SELECT count(*) as NB_OP FROM dbo.vue_appels_fonds_ost");
                int nbOp = ((Number) afo.get("NB_OP")).intValue();
                ost.setTotalNombreOst(nbOp > 0 ? String.valueOf(nbOp) : "R.A.S");
            } catch (Exception e) {
                ost.setTotalNombreOst("R.A.S");
            }
            stats.setAppelsFondsOst(ost);

            // 3. Factures Non Recouvrées (vue_factures_non_recouvrees_6mois)
            BillingStatsDTO.RecouvrementStats fnr = new BillingStatsDTO.RecouvrementStats();
            try {
                List<Map<String, Object>> fData = jdbcTemplate.queryForList("SELECT * FROM dbo.vue_factures_non_recouvrees_6mois");
                fnr.setCountFactures(fData.size());
                
                double totalFacture = 0;
                double totalRegle = 0;
                for (Map<String, Object> row : fData) {
                    totalFacture += row.get("Montant_Facture_ACT") != null ? ((Number) row.get("Montant_Facture_ACT")).doubleValue() : 0;
                    totalRegle += row.get("Montant_regle") != null ? ((Number) row.get("Montant_regle")).doubleValue() : 0;
                }
                
                fnr.setTotalRecouvres(totalRegle);
                fnr.setTotalFactures(totalFacture);
                
                if (!fData.isEmpty()) {
                    Map<String, Object> firstRow = fData.get(0);
                    String numFact = (String) firstRow.get("NUM_FACTURE");
                    String libAdh = firstRow.get("ShortName") != null ? (String) firstRow.get("ShortName") : (String) firstRow.get("INSTITUTION_PAYEUR");
                    Double mtFact = firstRow.get("Montant_Facture_ACT") != null ? ((Number) firstRow.get("Montant_Facture_ACT")).doubleValue() : 0.0;
                    fnr.setExampleText("N°:" + numFact + " De " + libAdh + " (MT Fact: " + String.format("%.3f", mtFact) + " DT)");
                } else {
                    fnr.setExampleText("Aucune facture trouvée");
                }
            } catch (Exception e) {
                fnr.setTotalRecouvres(0.0);
                fnr.setTotalFactures(0.0);
                fnr.setCountFactures(0);
                fnr.setExampleText("Aucune facture trouvée");
            }
            stats.setFacturesNonRecouvrees(fnr);

            // 4. Participants sans/avec prélèvements
            BillingStatsDTO.RecouvrementStats psp = new BillingStatsDTO.RecouvrementStats();
            psp.setTotalRecouvres(0.0); psp.setTotalFactures(0.0); psp.setCountFactures(0);
            psp.setExampleText("Données temps réel non configurées (vue manquante)");
            stats.setParticipantsSansPrelevements(psp);

            BillingStatsDTO.RecouvrementStats pap = new BillingStatsDTO.RecouvrementStats();
            pap.setTotalRecouvres(0.0); pap.setTotalFactures(0.0); pap.setCountFactures(0);
            pap.setExampleText("Données temps réel non configurées (vue manquante)");
            stats.setParticipantsAvecPrelevements(pap);

            // 5. TCN Data (vue_trimestres_tcn)
            BillingStatsDTO.EvolutionTcnRepo tcn = new BillingStatsDTO.EvolutionTcnRepo();
            try {
                // Vue retourne: trimestre, ANNEE, prestation, C_ST_TICKET ('Facturé' ou 'Généré'), MT
                List<Map<String, Object>> tcnList = jdbcTemplate.queryForList("SELECT * FROM dbo.vue_trimestres_tcn ORDER BY ANNEE DESC, trimestre DESC");
                if (!tcnList.isEmpty()) {
                    Map<String, Object> latest = tcnList.get(0);
                    tcn.setTrimestreEncours(((Number) latest.get("MT")).doubleValue());
                    tcn.setStatus((String) latest.get("C_ST_TICKET"));
                    
                    double sum = 0;
                    for (Map<String, Object> row : tcnList) {
                        sum += ((Number) row.get("MT")).doubleValue();
                    }
                    tcn.setMoyMensuelle((sum / tcnList.size()) / 3);

                    List<BillingStatsDTO.ChartDataPoint> tcnChart = new ArrayList<>();
                    for (Map<String, Object> row : tcnList) {
                        double mtFacture = "Facturé".equals(row.get("C_ST_TICKET")) ? ((Number) row.get("MT")).doubleValue() : 0.0;
                        double mtGenere = "Généré".equals(row.get("C_ST_TICKET")) ? ((Number) row.get("MT")).doubleValue() : 0.0;
                        tcnChart.add(createChartPoint(
                            row.get("trimestre") + " - " + row.get("ANNEE"),
                            mtFacture,
                            mtGenere
                        ));
                    }
                    tcn.setChartData(tcnChart);
                }
            } catch (Exception e) {}
            stats.setTcnData(tcn);

            // 6. Repo Espece Data (vue_trimestres_repo)
            BillingStatsDTO.EvolutionTcnRepo repo = new BillingStatsDTO.EvolutionTcnRepo();
            try {
                List<Map<String, Object>> repoList = jdbcTemplate.queryForList("SELECT * FROM dbo.vue_trimestres_repo ORDER BY ANNEE DESC, trimestre DESC");
                if (!repoList.isEmpty()) {
                    Map<String, Object> latest = repoList.get(0);
                    repo.setTrimestreEncours(((Number) latest.get("MT")).doubleValue());
                    repo.setStatus((String) latest.get("C_ST_TICKET"));
                    
                    double sum = 0;
                    for (Map<String, Object> row : repoList) {
                        sum += ((Number) row.get("MT")).doubleValue();
                    }
                    repo.setMoyMensuelle((sum / repoList.size()) / 3);

                    List<BillingStatsDTO.ChartDataPoint> repoChart = new ArrayList<>();
                    for (Map<String, Object> row : repoList) {
                        double mtFacture = "Facturé".equals(row.get("C_ST_TICKET")) ? ((Number) row.get("MT")).doubleValue() : 0.0;
                        double mtGenere = "Généré".equals(row.get("C_ST_TICKET")) ? ((Number) row.get("MT")).doubleValue() : 0.0;
                        repoChart.add(createChartPoint(
                            row.get("trimestre") + " - " + row.get("ANNEE"),
                            mtFacture,
                            mtGenere
                        ));
                    }
                    repo.setChartData(repoChart);
                }
            } catch (Exception e) {}
            stats.setRepoEspeceData(repo);

            // 7. Chiffre d'Affaires (v_repartition_ca)
            BillingStatsDTO.ChiffreAffairesData ca = new BillingStatsDTO.ChiffreAffairesData();
            try {
                List<Map<String, Object>> caListA0 = jdbcTemplate.queryForList("SELECT * FROM dbo.v_repartition_ca WHERE Decalage = 0");
                List<Map<String, Object>> caListA1 = jdbcTemplate.queryForList("SELECT * FROM dbo.v_repartition_ca WHERE Decalage = 1");

                List<BillingStatsDTO.RevenuRow> revenus = new ArrayList<>();
                List<BillingStatsDTO.PieDataPoint> pie = new ArrayList<>();

                String[] types = {"Stock", "Flux", "Ponctuel"};
                double totalA0 = 0;
                double totalA1 = 0;

                for (String t : types) {
                    double mtA0 = getMtFromList(caListA0, t);
                    double mtA1 = getMtFromList(caListA1, t);
                    revenus.add(createRevenuRow(t.toUpperCase(), mtA1, mtA0));
                    
                    double pourcentage = getPourcentageFromList(caListA0, t);
                    if (pourcentage > 0) {
                        pie.add(createPiePoint(t, pourcentage));
                    }
                    totalA0 += mtA0;
                    totalA1 += mtA1;
                }
                revenus.add(createRevenuRow("TOTAL", totalA1, totalA0));
                
                ca.setRevenusTable(revenus);
                ca.setRepartitionPie(pie);

                BillingStatsDTO.AnneeEncours ae = new BillingStatsDTO.AnneeEncours();
                ae.setMontantFactureHt(totalA0);
                ae.setMontantFactureTtc(totalA0 * 1.19); // Approximation TVA
                ae.setMontantRegleTtc(totalA0 * 1.19 * 0.95); // Exemple
                ae.setMontantNonRegleHt(0);
                ae.setMontantAvoirsHt(0);
                ca.setAnneeEncours(ae);
                
            } catch (Exception e) {}
            stats.setChiffreAffaires(ca);

            // 8. Top 10 Participants (vue_top10_participants)
            List<BillingStatsDTO.TopParticipant> top10 = new ArrayList<>();
            try {
                // Vue retourne: INSTITUTION_PAYEUR, ShortName, REG
                List<Map<String, Object>> topList = jdbcTemplate.queryForList("SELECT * FROM dbo.vue_top10_participants ORDER BY REG DESC");
                for (Map<String, Object> row : topList) {
                    String nom = row.get("ShortName") != null ? (String) row.get("ShortName") : (String) row.get("INSTITUTION_PAYEUR");
                    top10.add(createParticipant(nom, ((Number) row.get("REG")).doubleValue()));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            stats.setTop10Participants(top10);

        } catch (Exception e) {
            e.printStackTrace();
        }

        return stats;
    }

    private double getMtFromList(List<Map<String, Object>> list, String type) {
        for (Map<String, Object> row : list) {
            if (type.equalsIgnoreCase((String) row.get("Prestation"))) {
                return ((Number) row.get("MT")).doubleValue();
            }
        }
        return 0.0;
    }

    private double getPourcentageFromList(List<Map<String, Object>> list, String type) {
        for (Map<String, Object> row : list) {
            if (type.equalsIgnoreCase((String) row.get("Prestation"))) {
                return ((Number) row.get("Pourcentage")).doubleValue();
            }
        }
        return 0.0;
    }

    private BillingStatsDTO.ChartDataPoint createChartPoint(String label, double facture, double genere) {
        BillingStatsDTO.ChartDataPoint pt = new BillingStatsDTO.ChartDataPoint();
        pt.setLabel(label);
        pt.setFacture(facture);
        pt.setGenere(genere);
        return pt;
    }

    private BillingStatsDTO.RevenuRow createRevenuRow(String type, double a1, double a) {
        BillingStatsDTO.RevenuRow row = new BillingStatsDTO.RevenuRow();
        row.setType(type);
        row.setAnneePrecedente(a1);
        row.setAnneeCours(a);
        return row;
    }

    private BillingStatsDTO.PieDataPoint createPiePoint(String name, double value) {
        BillingStatsDTO.PieDataPoint pt = new BillingStatsDTO.PieDataPoint();
        pt.setName(name);
        pt.setValue(value);
        return pt;
    }

    private BillingStatsDTO.TopParticipant createParticipant(String nom, double montant) {
        BillingStatsDTO.TopParticipant p = new BillingStatsDTO.TopParticipant();
        p.setNom(nom);
        p.setMontant(montant);
        return p;
    }

    public BillingPredictionDTO getBillingPrediction() {
        BillingPredictionDTO dto = new BillingPredictionDTO();
        
        try {
            // 1. Fetch historical aggregated data from SQL Server view
            List<Map<String, Object>> dbData = jdbcTemplate.queryForList(
                "SELECT ANNEE, Prestation, SUM(MT) as TOTAL_MT " +
                "FROM dbo.v_evolution_ca_cinqAnnee_par_regle " +
                "GROUP BY ANNEE, Prestation " +
                "ORDER BY ANNEE ASC"
            );

            if (dbData.isEmpty()) {
                dto.setAiAnalysis("Aucune donnée historique trouvée pour effectuer les prédictions.");
                dto.setAnomalies(List.of("Alerte: Historique vide."));
                return dto;
            }

            // 2. Map and aggregate by year
            Map<Integer, BillingPredictionDTO.HistoricalDataPoint> pointsMap = new TreeMap<>();
            for (Map<String, Object> row : dbData) {
                int year = Integer.parseInt(row.get("ANNEE").toString().trim());
                String prestation = (String) row.get("Prestation");
                double amount = row.get("TOTAL_MT") != null ? ((Number) row.get("TOTAL_MT")).doubleValue() : 0.0;
                
                BillingPredictionDTO.HistoricalDataPoint pt = pointsMap.computeIfAbsent(year, k -> {
                    BillingPredictionDTO.HistoricalDataPoint p = new BillingPredictionDTO.HistoricalDataPoint();
                    p.setYear(year);
                    p.setPrediction(false);
                    return p;
                });
                
                if ("Stock".equalsIgnoreCase(prestation)) {
                    pt.setStock(amount);
                } else if ("Flux".equalsIgnoreCase(prestation)) {
                    pt.setFlux(amount);
                } else if ("Ponctuel".equalsIgnoreCase(prestation)) {
                    pt.setPonctuel(amount);
                }
                pt.setTotal(pt.getTotal() + amount);
            }

            // 3. Exclude the current (incomplete) year AND the outlier year (2021) from training data
            int currentYear = java.time.Year.now().getValue(); // 2026
            Map<Integer, BillingPredictionDTO.HistoricalDataPoint> completeYearsMap = new TreeMap<>();
            BillingPredictionDTO.HistoricalDataPoint currentYearActual = null;

            for (Map.Entry<Integer, BillingPredictionDTO.HistoricalDataPoint> entry : pointsMap.entrySet()) {
                int year = entry.getKey();
                // We exclude 2021 because it's an extreme outlier that skews the linear regression.
                // We only train on the stable period (2022 - 2025)
                if (year > 2021 && year < currentYear) {
                    completeYearsMap.put(year, entry.getValue());
                } else if (year == currentYear) {
                    currentYearActual = entry.getValue(); // Keep for reference (partial data)
                }
            }

            if (completeYearsMap.size() < 2) {
                dto.setAiAnalysis("Pas assez d'années complètes pour effectuer les prédictions (minimum 2 ans requis).");
                dto.setAnomalies(List.of("Alerte: Données insuffisantes."));
                return dto;
            }

            // 4. Train models on COMPLETE years only
            List<Map<String, Object>> globalPoints = new ArrayList<>();
            List<Map<String, Object>> stockPoints = new ArrayList<>();
            List<Map<String, Object>> fluxPoints = new ArrayList<>();
            List<Map<String, Object>> ponctuelPoints = new ArrayList<>();

            for (BillingPredictionDTO.HistoricalDataPoint pt : completeYearsMap.values()) {
                globalPoints.add(Map.of("year", pt.getYear(), "value", pt.getTotal()));
                stockPoints.add(Map.of("year", pt.getYear(), "value", pt.getStock()));
                fluxPoints.add(Map.of("year", pt.getYear(), "value", pt.getFlux()));
                ponctuelPoints.add(Map.of("year", pt.getYear(), "value", pt.getPonctuel()));
            }

            MachineLearningService.ModelResult globalModel = mlService.trainModel(globalPoints);
            MachineLearningService.ModelResult stockModel = mlService.trainModel(stockPoints);
            MachineLearningService.ModelResult fluxModel = mlService.trainModel(fluxPoints);
            MachineLearningService.ModelResult ponctuelModel = mlService.trainModel(ponctuelPoints);

            // 5. Predict CURRENT year (2026) and NEXT year (2027)
            int nextYear = currentYear + 1;
            dto.setNextYear(nextYear);

            // Prediction for current year (2026 full year estimate)
            double predGlobal2026 = Math.max(0.0, globalModel.predict(currentYear));
            double predStock2026 = Math.max(0.0, stockModel.predict(currentYear));
            double predFlux2026 = Math.max(0.0, fluxModel.predict(currentYear));
            double predPonctuel2026 = Math.max(0.0, ponctuelModel.predict(currentYear));

            // Prediction for next year (2027)
            double predGlobal = Math.max(0.0, globalModel.predict(nextYear));
            double predStock = Math.max(0.0, stockModel.predict(nextYear));
            double predFlux = Math.max(0.0, fluxModel.predict(nextYear));
            double predPonctuel = Math.max(0.0, ponctuelModel.predict(nextYear));

            dto.setPredictedGlobal(predGlobal);
            dto.setPredictedStock(predStock);
            dto.setPredictedFlux(predFlux);
            dto.setPredictedPonctuel(predPonctuel);

            // 6. Get last COMPLETE year reference (2025)
            int lastCompleteYear = completeYearsMap.keySet().stream().max(Integer::compareTo).orElse(currentYear - 1);
            BillingPredictionDTO.HistoricalDataPoint lastYearPt = completeYearsMap.get(lastCompleteYear);
            double lastGlobal = lastYearPt != null ? lastYearPt.getTotal() : 0.0;
            double lastStock = lastYearPt != null ? lastYearPt.getStock() : 0.0;
            double lastFlux = lastYearPt != null ? lastYearPt.getFlux() : 0.0;
            double lastPonctuel = lastYearPt != null ? lastYearPt.getPonctuel() : 0.0;

            dto.setLastYearGlobal(lastGlobal);
            dto.setLastYearStock(lastStock);
            dto.setLastYearFlux(lastFlux);
            dto.setLastYearPonctuel(lastPonctuel);

            // 7. Calculate evolution percentages (vs last COMPLETE year, not partial 2026)
            dto.setEvolutionGlobalPercentage(lastGlobal > 0 ? ((predGlobal - lastGlobal) / lastGlobal) * 100 : 0.0);
            dto.setEvolutionStockPercentage(lastStock > 0 ? ((predStock - lastStock) / lastStock) * 100 : 0.0);
            dto.setEvolutionFluxPercentage(lastFlux > 0 ? ((predFlux - lastFlux) / lastFlux) * 100 : 0.0);
            dto.setEvolutionPonctuelPercentage(lastPonctuel > 0 ? ((predPonctuel - lastPonctuel) / lastPonctuel) * 100 : 0.0);

            // 8. Format history points list (complete years only)
            List<BillingPredictionDTO.HistoricalDataPoint> historyList = new ArrayList<>(completeYearsMap.values());
            
            // Add predicted 2026 point (full year estimate)
            BillingPredictionDTO.HistoricalDataPoint pred2026Pt = new BillingPredictionDTO.HistoricalDataPoint();
            pred2026Pt.setYear(currentYear);
            pred2026Pt.setTotal(predGlobal2026);
            pred2026Pt.setStock(predStock2026);
            pred2026Pt.setFlux(predFlux2026);
            pred2026Pt.setPonctuel(predPonctuel2026);
            pred2026Pt.setPrediction(true);
            historyList.add(pred2026Pt);

            // Add predicted 2027 point
            BillingPredictionDTO.HistoricalDataPoint pred2027Pt = new BillingPredictionDTO.HistoricalDataPoint();
            pred2027Pt.setYear(nextYear);
            pred2027Pt.setTotal(predGlobal);
            pred2027Pt.setStock(predStock);
            pred2027Pt.setFlux(predFlux);
            pred2027Pt.setPonctuel(predPonctuel);
            pred2027Pt.setPrediction(true);
            historyList.add(pred2027Pt);

            dto.setHistoricalPoints(historyList);

            // 9. Fetch detailed rule changes (last 2 complete years) for anomaly detection
            List<Map<String, Object>> rulesData = jdbcTemplate.queryForList(
                "SELECT C_REGLE, LIB_REGLE, ANNEE, Prestation, SUM(MT) as TOTAL_MT " +
                "FROM dbo.v_evolution_ca_cinqAnnee_par_regle " +
                "WHERE ANNEE IN (?, ?) " +
                "GROUP BY C_REGLE, LIB_REGLE, ANNEE, Prestation " +
                "ORDER BY C_REGLE, ANNEE DESC",
                String.valueOf(lastCompleteYear), String.valueOf(lastCompleteYear - 1)
            );

            // 10. Call Cognitive AI (Llama 3.3 via Groq)
            StringBuilder summary = new StringBuilder();
            summary.append("Données historiques agrégées (années complètes uniquement, l'année " + currentYear + " est exclue car incomplète) :\n");
            for (BillingPredictionDTO.HistoricalDataPoint pt : completeYearsMap.values()) {
                summary.append(String.format("- Année %d : Total=%.2f DT, Stock=%.2f DT, Flux=%.2f DT, Ponctuel=%.2f DT\n",
                    pt.getYear(), pt.getTotal(), pt.getStock(), pt.getFlux(), pt.getPonctuel()));
            }

            summary.append(String.format("\nValeurs de la dernière année complète (%d) :\n", lastCompleteYear));
            summary.append(String.format("- CA Global dernière année : %.2f DT\n", lastGlobal));
            summary.append(String.format("- Stock dernière année : %.2f DT\n", lastStock));
            summary.append(String.format("- Flux dernière année : %.2f DT\n", lastFlux));
            summary.append(String.format("- Ponctuel dernière année : %.2f DT\n", lastPonctuel));

            summary.append(String.format("\nPrédiction pour l'année en cours %d (estimation année complète) :\n", currentYear));
            summary.append(String.format("- CA Global %d : %.2f DT\n", currentYear, predGlobal2026));
            summary.append(String.format("- Stock %d : %.2f DT\n", currentYear, predStock2026));
            summary.append(String.format("- Flux %d : %.2f DT\n", currentYear, predFlux2026));
            summary.append(String.format("- Ponctuel %d : %.2f DT\n", currentYear, predPonctuel2026));

            summary.append(String.format("\nPrédictions calculées par Régression Linéaire (Scikit-Learn) pour l'année %d :\n", nextYear));
            summary.append(String.format("- Chiffre d'Affaires Global : %.2f DT (R2 = %.2f) => Évolution vs %d : %.2f%%\n", predGlobal, globalModel.r2, lastCompleteYear, dto.getEvolutionGlobalPercentage()));
            summary.append(String.format("- Prestation Stock : %.2f DT (R2 = %.2f) => Évolution vs %d : %.2f%%\n", predStock, stockModel.r2, lastCompleteYear, dto.getEvolutionStockPercentage()));
            summary.append(String.format("- Prestation Flux : %.2f DT (R2 = %.2f) => Évolution vs %d : %.2f%%\n", predFlux, fluxModel.r2, lastCompleteYear, dto.getEvolutionFluxPercentage()));
            summary.append(String.format("- Prestation Ponctuel : %.2f DT (R2 = %.2f) => Évolution vs %d : %.2f%%\n", predPonctuel, ponctuelModel.r2, lastCompleteYear, dto.getEvolutionPonctuelPercentage()));
            summary.append("\nIMPORTANT : Utilise EXACTEMENT les pourcentages d'évolution ci-dessus dans ton analyse. Ne calcule PAS tes propres pourcentages. Précise que les évolutions sont calculées par rapport à l'année " + lastCompleteYear + " (dernière année complète).\n");

            summary.append("\nComparatif détaillé des règles de facturation sur les deux dernières années :\n");
            int ruleCount = 0;
            for (Map<String, Object> rule : rulesData) {
                if (ruleCount++ >= 40) {
                    summary.append("- ... (autres règles ignorées pour limiter la taille du prompt)\n");
                    break;
                }
                summary.append(String.format("- Règle %s (%s) - %s : Année %s, Montant = %.2f DT\n",
                    rule.get("C_REGLE"), rule.get("LIB_REGLE"), rule.get("Prestation"), rule.get("ANNEE"), ((Number) rule.get("TOTAL_MT")).doubleValue()));
            }

            String systemPrompt = 
                "Tu es l'Agent IA d'analyse financière et de détection d'anomalies de Tunisie Clearing.\n" +
                "Analyse les données historiques et les prédictions mathématiques fournies par l'utilisateur. Détecte des baisses suspectes ou hausses anormales entre la dernière année et l'année précédente au niveau des règles de facturation.\n" +
                "Retourne UNIQUEMENT un objet JSON valide contenant exactement ces deux clés:\n" +
                "{\n" +
                "  \"aiAnalysis\": \"Synthèse rédigée très professionnelle en français expliquant la tendance globale et les prestations à suivre.\",\n" +
                "  \"anomalies\": [\n" +
                "    \"Description claire en français d'une anomalie détectée sur une règle (ex: baisse drastique d'une règle/activité spécifique)\",\n" +
                "    \"Autre anomalie ou observation...\"\n" +
                "  ]\n" +
                "}\n" +
                "Ne réponds avec rien d'autre que du JSON. Pas de blabla, pas de markdown (```json ... ```).";

            String aiResponse = geminiService.askRaw(systemPrompt, summary.toString());
            
            // Clean AI response if it contains markdown formatting
            aiResponse = aiResponse.trim();
            if (aiResponse.startsWith("```json")) {
                aiResponse = aiResponse.substring(7);
            }
            if (aiResponse.endsWith("```")) {
                aiResponse = aiResponse.substring(0, aiResponse.length() - 3);
            }
            aiResponse = aiResponse.trim();

            // Parse simple JSON manually to avoid Jackson databind dependency issues
            String aiAnalysis = "";
            List<String> anomalies = new ArrayList<>();
            
            try {
                // Extract aiAnalysis
                int aiAnalysisIdx = aiResponse.indexOf("\"aiAnalysis\"");
                if (aiAnalysisIdx != -1) {
                    int startQuote = aiResponse.indexOf("\"", aiAnalysisIdx + 12);
                    if (startQuote != -1) {
                        int endQuote = -1;
                        boolean escaped = false;
                        for (int i = startQuote + 1; i < aiResponse.length(); i++) {
                            char c = aiResponse.charAt(i);
                            if (escaped) {
                                escaped = false;
                            } else if (c == '\\') {
                                escaped = true;
                            } else if (c == '"') {
                                endQuote = i;
                                break;
                            }
                        }
                        if (endQuote != -1) {
                            aiAnalysis = aiResponse.substring(startQuote + 1, endQuote)
                                           .replace("\\n", "\n")
                                           .replace("\\\"", "\"");
                        }
                    }
                }
                
                // Extract anomalies
                int anomaliesIdx = aiResponse.indexOf("\"anomalies\"");
                if (anomaliesIdx != -1) {
                    int startBracket = aiResponse.indexOf("[", anomaliesIdx);
                    int endBracket = aiResponse.indexOf("]", startBracket);
                    if (startBracket != -1 && endBracket != -1 && endBracket > startBracket + 1) {
                        String listContent = aiResponse.substring(startBracket + 1, endBracket);
                        String[] items = listContent.split("\",\\s*\"");
                        for (String item : items) {
                            String cleanItem = item.replace("\"", "").trim()
                                                   .replace("\\n", "\n")
                                                   .replace("\\\"", "\"");
                            if (!cleanItem.isEmpty()) {
                                anomalies.add(cleanItem);
                            }
                        }
                    }
                }
            } catch (Exception parseEx) {
                parseEx.printStackTrace();
            }

            dto.setAiAnalysis(aiAnalysis.isEmpty() ? aiResponse : aiAnalysis);
            dto.setAnomalies(anomalies);

        } catch (Exception e) {
            e.printStackTrace();
            dto.setAiAnalysis("Erreur lors de la génération des prédictions : " + e.getMessage());
        }

        return dto;
    }
}
