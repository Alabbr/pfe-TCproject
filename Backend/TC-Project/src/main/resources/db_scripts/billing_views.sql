-- ==========================================================
-- SCRIPT DE MIGRATION : DASHBOARD FACTURATION IA
-- A exécuter sur la base de données : db_facturation
-- ==========================================================

-- 1. Vue pour les Tickets Non Facturés (Montant)
CREATE OR ALTER VIEW vue_tickets_non_factures AS
SELECT sum(MT_TOTAL) AS TOTAL_NON_FACTURE 
FROM admin_fact.TFACT_TICKET 
WHERE ANNEE = (DATEPART(year, (SELECT max(dt_extract) FROM admin_fact.TFACT_JEXTRACT_TICKET))) 
AND C_ST_TICKET = '00';

GO

-- 2. Vue pour le Top 10 Participants (Recouvrement)
CREATE OR ALTER VIEW vue_top10_participants AS
WITH cte AS (
    SELECT
        (a.MT_TOTAL - a.MT_TVA + a.MT_RS + a.MT_TVA_ACT - a.MT_RS_ACT) - CASE WHEN b.MT_REGL_FACTURE IS NULL THEN 0 ELSE b.MT_REGL_FACTURE END AS REG,
        CASE WHEN b.MT_REGL_FACTURE IS NULL THEN 0 ELSE b.MT_REGL_FACTURE END AS MT_REGL_FACTURE,
        (a.MT_TOTAL - a.MT_TVA + a.MT_RS + a.MT_TVA_ACT - a.MT_RS_ACT) AS MT_TOTAL_ACT, 
        a.* 
    FROM admin_fact.TFACT_FACTURES a
    LEFT JOIN (SELECT num_fact, sum(MT_REGL_FACTURE) AS MT_REGL_FACTURE FROM admin_fact.TFACT_REGLEMENT_FACTURE GROUP BY num_fact) b ON a.NUM_FACT = b.NUM_FACT
    WHERE a.Y_FACT = 1 
    AND substring(a.NUM_FACT,3,4) = (DATEPART(year,(SELECT max(dt_extract) FROM admin_fact.TFACT_JEXTRACT_TICKET))) 
    AND I_FORC_REGL = 0 AND I_REGLEMENT = 0 
    AND convert(DECIMAL(18, 3), (a.MT_TOTAL - a.MT_TVA + a.MT_RS + a.MT_TVA_ACT - a.MT_RS_ACT)) - convert(DECIMAL(18, 3), CASE WHEN b.MT_REGL_FACTURE IS NULL THEN 0 ELSE b.MT_REGL_FACTURE END) <> 0
)
SELECT TOP 10 
    a.INSTITUTION_PAYEUR, 
    z.ShortName, 
    sum(convert(decimal(18,3),a.REG)) AS REG
FROM cte a
LEFT JOIN [192.168.254.60].[csd_tn].[dbo].[institution] z ON z.swift COLLATE FRENCH_CI_AS = a.INSTITUTION_PAYEUR
GROUP BY a.INSTITUTION_PAYEUR, z.ShortName
ORDER BY sum(a.REG) DESC;

GO

-- 3. Vue pour l'évolution des trimestres (TCN)
CREATE OR ALTER VIEW vue_trimestres_tcn AS
SELECT TOP 8 * FROM (
    SELECT '1er Trim' AS trimestre, ANNEE, 'Mvts & Cptes' AS prestation, CASE WHEN C_ST_TICKET ='01' THEN 'Facturé' ELSE 'Généré' END AS C_ST_TICKET, convert(decimal(18,1), (sum(mt_total)/1000)) AS MT 
    FROM admin_fact.TFACT_TICKET WHERE C_REGLE in (118,117,119,122,121,120,134,143,142) AND C_ST_TICKET IN ('01','00') AND DT_GEN_TICKET >= '01/01/'+cast(annee as varchar) AND DT_GEN_TICKET <= '31/03/'+cast(annee as varchar) GROUP BY C_ST_TICKET, ANNEE
    UNION ALL
    SELECT '2ème Trim' AS trimestre, ANNEE, 'Mvts & Cptes' AS prestation, CASE WHEN C_ST_TICKET ='01' THEN 'Facturé' ELSE 'Généré' END AS C_ST_TICKET, convert(decimal(18,1), (sum(mt_total)/1000)) AS MT 
    FROM admin_fact.TFACT_TICKET WHERE C_REGLE in (118,117,119,122,121,120,134,143,142) AND C_ST_TICKET IN ('01','00') AND DT_GEN_TICKET >= '01/04/'+cast(annee as varchar) AND DT_GEN_TICKET <= '30/06/'+cast(annee as varchar) GROUP BY C_ST_TICKET, ANNEE
    UNION ALL
    SELECT '3ème Trim' AS trimestre, ANNEE, 'Mvts & Cptes' AS prestation, CASE WHEN C_ST_TICKET ='01' THEN 'Facturé' ELSE 'Généré' END AS C_ST_TICKET, convert(decimal(18,1), (sum(mt_total)/1000)) AS MT 
    FROM admin_fact.TFACT_TICKET WHERE C_REGLE in (118,117,119,122,121,120,134,143,142) AND C_ST_TICKET IN ('01','00') AND DT_GEN_TICKET >= '01/07/'+cast(annee as varchar) AND DT_GEN_TICKET <= '30/09/'+cast(annee as varchar) GROUP BY C_ST_TICKET, ANNEE
    UNION ALL
    SELECT '4ème Trim' AS trimestre, ANNEE, 'Mvts & Cptes' AS prestation, CASE WHEN C_ST_TICKET ='01' THEN 'Facturé' ELSE 'Généré' END AS C_ST_TICKET, convert(decimal(18,1), (sum(mt_total)/1000)) AS MT 
    FROM admin_fact.TFACT_TICKET WHERE C_REGLE in (118,117,119,122,121,120,134,143,142) AND C_ST_TICKET IN ('01','00') AND DT_GEN_TICKET >= '01/10/'+cast(annee as varchar) AND DT_GEN_TICKET <= '31/12/'+cast(annee as varchar) GROUP BY C_ST_TICKET, ANNEE
) T
WHERE ANNEE IN (DATEPART(year,(SELECT max(dt_extract) FROM admin_fact.TFACT_JEXTRACT_TICKET)),DATEPART(year,(SELECT max(dt_extract) FROM admin_fact.TFACT_JEXTRACT_TICKET))-1)
ORDER BY ANNEE desc, trimestre desc;

GO

-- 4. Vue pour l'évolution des trimestres (REPO ESPECE)
CREATE OR ALTER VIEW vue_trimestres_repo AS
SELECT TOP 8 * FROM (
    SELECT '1er Trim' AS trimestre, ANNEE, 'REPO' AS prestation, CASE WHEN C_ST_TICKET ='01' THEN 'Facturé' ELSE 'Généré' END AS C_ST_TICKET, convert(decimal(18,1), (sum(mt_total)/1000)) AS MT 
    FROM admin_fact.TFACT_TICKET WHERE C_REGLE=116 AND C_ST_TICKET IN ('01','00') AND DT_GEN_TICKET >= '01/01/'+cast(annee as varchar) AND DT_GEN_TICKET <= '31/03/'+cast(annee as varchar) GROUP BY C_ST_TICKET, ANNEE
    UNION ALL
    SELECT '2ème Trim' AS trimestre, ANNEE, 'REPO' AS prestation, CASE WHEN C_ST_TICKET ='01' THEN 'Facturé' ELSE 'Généré' END AS C_ST_TICKET, convert(decimal(18,1), (sum(mt_total)/1000)) AS MT 
    FROM admin_fact.TFACT_TICKET WHERE C_REGLE=116 AND C_ST_TICKET IN ('01','00') AND DT_GEN_TICKET >= '01/04/'+cast(annee as varchar) AND DT_GEN_TICKET <= '30/06/'+cast(annee as varchar) GROUP BY C_ST_TICKET, ANNEE
    UNION ALL
    SELECT '3ème Trim' AS trimestre, ANNEE, 'REPO' AS prestation, CASE WHEN C_ST_TICKET ='01' THEN 'Facturé' ELSE 'Généré' END AS C_ST_TICKET, convert(decimal(18,1), (sum(mt_total)/1000)) AS MT 
    FROM admin_fact.TFACT_TICKET WHERE C_REGLE=116 AND C_ST_TICKET IN ('01','00') AND DT_GEN_TICKET >= '01/07/'+cast(annee as varchar) AND DT_GEN_TICKET <= '30/09/'+cast(annee as varchar) GROUP BY C_ST_TICKET, ANNEE
    UNION ALL
    SELECT '4ème Trim' AS trimestre, ANNEE, 'REPO' AS prestation, CASE WHEN C_ST_TICKET ='01' THEN 'Facturé' ELSE 'Généré' END AS C_ST_TICKET, convert(decimal(18,1), (sum(mt_total)/1000)) AS MT 
    FROM admin_fact.TFACT_TICKET WHERE C_REGLE=116 AND C_ST_TICKET IN ('01','00') AND DT_GEN_TICKET >= '01/10/'+cast(annee as varchar) AND DT_GEN_TICKET <= '31/12/'+cast(annee as varchar) GROUP BY C_ST_TICKET, ANNEE
) T
WHERE ANNEE IN (DATEPART(year,(SELECT max(dt_extract) FROM admin_fact.TFACT_JEXTRACT_TICKET)),DATEPART(year,(SELECT max(dt_extract) FROM admin_fact.TFACT_JEXTRACT_TICKET))-1)
ORDER BY ANNEE desc, trimestre desc;

GO

-- 5. Procédure Stockée pour calculer la répartition du chiffre d'affaires
-- Accepte un paramètre @Decalage (0 pour l'année en cours, 1 pour A-1)
CREATE OR ALTER PROCEDURE get_repartition_ca 
    @Decalage INT = 0
AS
BEGIN
    SET NOCOUNT ON;
    
    WITH cte AS (
        SELECT ANNEE, 'Stock' AS Prestation, sum(MT_TOTAL) AS MT 
        FROM admin_fact.TFACT_TICKET 
        WHERE C_ST_TICKET='01' 
        AND ANNEE = DATEPART(year,(SELECT max(dt_extract) FROM admin_fact.TFACT_JEXTRACT_TICKET)) - @Decalage 
        AND C_REGLE IN (83,86,100,109,110,111,101,9,10,11,12,3,4,5,6,70,71,7,8,15,17,18,28,123,62,63,64,66,113,65,67,84,106,107,50,117,118,119,120,122,91,72,108,134,143,142) 
        GROUP BY ANNEE
        
        UNION ALL
        
        SELECT ANNEE, 'Flux' AS Prestation, sum(MT_TOTAL) AS MT 
        FROM admin_fact.TFACT_TICKET 
        WHERE C_ST_TICKET='01' 
        AND ANNEE = DATEPART(year,(SELECT max(dt_extract) FROM admin_fact.TFACT_JEXTRACT_TICKET)) - @Decalage 
        AND C_REGLE IN (49,61,75,92,93,95,96,97,13,20,1,23,24,25,26,73,74,78,79,80,81,82,85,98,99,112,124,127,128,21,22,116,14,94,133,135) 
        GROUP BY ANNEE
        
        UNION ALL
        
        SELECT ANNEE, 'Ponctuel' AS Prestation, sum(MT_TOTAL) AS MT 
        FROM admin_fact.TFACT_TICKET 
        WHERE C_ST_TICKET='01' 
        AND ANNEE = DATEPART(year,(SELECT max(dt_extract) FROM admin_fact.TFACT_JEXTRACT_TICKET)) - @Decalage 
        AND C_REGLE IN (16,19,27,105,126,129,130,104,114,115,29,30,68,69,103,125,59,60,88,89,121,131,132) 
        GROUP BY ANNEE
    )
    SELECT z.TOTAL, A.MT*100/Z.TOTAL AS Pourcentage, a.* 
    FROM cte a
    LEFT JOIN (SELECT ANNEE, sum(MT) AS TOTAL FROM cte GROUP BY ANNEE) z ON z.ANNEE=a.ANNEE
    WHERE a.mt IS NOT NULL AND a.mt > 0
    ORDER BY MT DESC;
END;
GO

-- 6. Vue pour les Tickets Ponctuels Non Facturés
CREATE OR ALTER VIEW vue_tickets_ponctuels_non_factures AS
SELECT z.C_REGLE, z.LIB_REGLE, e.ShortName, a.DT_GEN_TICKET, a.MT_TOTAL 
FROM admin_fact.tfact_ticket a  
LEFT JOIN admin_fact.TFACT_REGLE z on z.C_REGLE=a.C_REGLE  
LEFT JOIN [192.168.254.60].[csd_tn].[dbo].[institution] e ON e.swift COLLATE FRENCH_CI_AS = a.INSTITUTION_PAYEUR  
WHERE a.DT_GEN_TICKET >= dateadd(month,-12, (SELECT max(dt_extract) FROM admin_fact.TFACT_JEXTRACT_TICKET))   
AND a.C_ST_TICKET='00' AND a.C_REGLE IN (93,14,100,97,94,95,96,75,13,20,91,92,72,108);

GO

-- 7. Vue pour les Appels de Fonds OST
CREATE OR ALTER VIEW vue_appels_fonds_ost AS
WITH cte AS(  
    SELECT row_number() over(order by DateDuJour) AS NUM_LIGNE, DateDuJour, TypeDuJour 
    FROM ( 
        SELECT bc_month, bc_year, substring(F.days, v.number+1, 1) AS TypeDuJour, bc_month+1 AS MONTH, 
        row_number() over( PARTITION BY bc_month, bc_year order by bc_month, bc_year) AS Day,  
        CAST(CAST(bc_year AS varchar) + '-' + CAST(bc_month+1 AS varchar) + '-' + CAST(row_number() over( PARTITION BY bc_month, bc_year order by bc_month, bc_year) AS varchar) AS DATETIME) AS DateDuJour   
        FROM (SELECT bc_month, bc_year, days FROM BusinessCalendar WHERE calendarKey='DEFAULT') F 
        JOIN master..spt_values v ON v.number < len(F.days)
        WHERE v.type = 'P' AND bc_year IN (DATEPART(year,(SELECT max(businessdate) FROM BusinessDateInfo)),DATEPART(year,(SELECT max(businessdate) FROM BusinessDateInfo))+1,DATEPART(year,(SELECT max(businessdate) FROM BusinessDateInfo))-1) 
    ) O 
    WHERE TypeDuJour='1'  
)  
SELECT z.shortName, a.paymentDate 
FROM CorporateAction a   
LEFT JOIN Issue ON Issue.id = a.issue_id   
LEFT JOIN issuer z ON z.id=Issue.ISSUER_ID  
WHERE a.status NOT IN ('REMOVED','WITHDRAWN')   
AND a.corporateActionType='Dividend'   
AND a.exDate=(SELECT DateDuJour FROM cte WHERE DateDuJour=(SELECT min(DateDuJour) FROM cte WHERE DateDuJour>(SELECT max(businessDate) FROM BusinessDateInfo WHERE t_key='DEFAULT')));

GO

-- 8. Vue pour les factures non recouvrées (Plus de 6 mois)
CREATE OR ALTER VIEW vue_factures_non_recouvrees_6mois AS
WITH cte1 AS ( 
    SELECT num_fact, sum(MT_REGL_FACTURE) AS Montant_regle 
    FROM admin_fact.TFACT_REGLEMENT_FACTURE 
    GROUP BY num_fact 
)
SELECT * FROM (
    SELECT a.NUM_FACTURE, A.INSTITUTION_PAYEUR, e.ShortName, a.Montant_Facture_ACT, z.Montant_regle, 
    a.Montant_Facture_ACT - sum(CASE WHEN z.Montant_regle IS NULL THEN 0 ELSE z.Montant_regle END) AS RESTE_A_REGLE
    FROM admin_fact.TFACT_FACTURES a 
    LEFT JOIN (SELECT * FROM cte1) z ON a.NUM_FACTURE = z.NUM_FACT 
    LEFT JOIN [192.168.254.60].[csd_tn].[dbo].[institution] e ON e.swift COLLATE FRENCH_CI_AS = a.INSTITUTION_PAYEUR 
    WHERE a.DATE_EDITION <= dateadd(month, -2, (SELECT max(dt_extract) FROM admin_fact.TFACT_JEXTRACT_TICKET))
    AND a.DATE_EDITION >= dateadd(month, -6, (SELECT max(dt_extract) FROM admin_fact.TFACT_JEXTRACT_TICKET))
    GROUP BY z.Montant_regle, A.INSTITUTION_PAYEUR, a.DATE_EDITION, e.ShortName, a.NUM_FACTURE, Montant_Facture_ACT
) k 
WHERE RESTE_A_REGLE > 10 OR RESTE_A_REGLE IS NULL;
GO
