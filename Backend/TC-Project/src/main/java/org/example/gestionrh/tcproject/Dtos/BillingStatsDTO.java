package org.example.gestionrh.tcproject.DTOs;

import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
public class BillingStatsDTO {
    
    private SuiviManipulation suiviManipulation;
    private AppelsFondsOst appelsFondsOst;
    private RecouvrementStats facturesNonRecouvrees;
    private RecouvrementStats participantsSansPrelevements;
    private RecouvrementStats participantsAvecPrelevements;
    
    private EvolutionTcnRepo tcnData;
    private EvolutionTcnRepo repoEspeceData;
    
    private ChiffreAffairesData chiffreAffaires;
    private List<TopParticipant> top10Participants;

    @Data
    public static class SuiviManipulation {
        private String generationTickets;
        private int ticketsPonctuelsNbr;
        private double ticketsPonctuelsMdt;
        private String comptabilisationFactures;
        private double montantTicketsNonFacture;
        private String lastRefresh;
    }

    @Data
    public static class AppelsFondsOst {
        private String totalNombreOst;
    }

    @Data
    public static class RecouvrementStats {
        private double totalRecouvres;
        private double totalFactures;
        private int countFactures;
        private String exampleText;
    }

    @Data
    public static class EvolutionTcnRepo {
        private double trimestreEncours;
        private String status;
        private double moyMensuelle;
        private List<ChartDataPoint> chartData;
    }

    @Data
    public static class ChartDataPoint {
        private String label; // e.g. "1er Trim - 2025"
        private double facture;
        private double genere;
    }

    @Data
    public static class ChiffreAffairesData {
        private List<RevenuRow> revenusTable;
        private List<PieDataPoint> repartitionPie;
        private AnneeEncours anneeEncours;
    }

    @Data
    public static class RevenuRow {
        private String type;
        private double anneePrecedente;
        private double anneeCours;
    }

    @Data
    public static class PieDataPoint {
        private String name;
        private double value;
    }

    @Data
    public static class AnneeEncours {
        private double montantFactureHt;
        private double montantFactureTtc;
        private double montantRegleTtc;
        private double montantNonRegleHt;
        private double montantAvoirsHt;
        private double montantAvoirsTtc;
    }

    @Data
    public static class TopParticipant {
        private String nom;
        private double montant;
    }
}
