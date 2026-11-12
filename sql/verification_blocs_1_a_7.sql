-- Diagnostic en lecture seule à exécuter sur la base de l'application, avec le même search_path.
-- Une ligne MANQUANT signale une migration à appliquer ; ce script ne modifie aucune donnée.
WITH attentes(table_nom, colonne, migration) AS (VALUES
 ('demande','effectif_demande','19_besoin_personnel.sql'),
 ('demande','budget_global_recrutement','21_budget_recrutement.sql'),
 ('candidature','proposition_statut','18_proposition_embauche.sql'),
 ('candidature','proposition_role','22_embauche_contrats.sql'),
 ('dossier_employe','matricule','17_dossier_salarie.sql et 22_embauche_contrats.sql'),
 ('contrat_employe','contrat_precedent_id','22_embauche_contrats.sql'),
 ('onboarding','id','16_contrats_onboarding_departs.sql'),
 ('onboarding_tache','id','16_contrats_onboarding_departs.sql'),
 ('remuneration_employe','id','17_dossier_salarie.sql'),
 ('audit_dossier','id','17_dossier_salarie.sql'),
 ('regime_travail','id','20_temps_conges_absences.sql'),
 ('jour_planning','id','20_temps_conges_absences.sql'),
 ('pointage_jour','id','20_temps_conges_absences.sql'),
 ('type_conge','id','20_temps_conges_absences.sql'),
 ('droit_conge','id','20_temps_conges_absences.sql'),
 ('absence_employe','id','20_temps_conges_absences.sql'),
 ('demande_conge','motif_annulation','23_conges_annulation.sql'),
 ('demande_conge','commentaire_manager','24_conges_commentaire_responsable.sql')
)
SELECT table_nom, colonne, migration,
       CASE WHEN EXISTS (
           SELECT 1 FROM pg_attribute
            WHERE attrelid = to_regclass(table_nom) AND attname = colonne
              AND attnum > 0 AND NOT attisdropped
       ) THEN 'OK' ELSE 'MANQUANT' END AS etat
  FROM attentes ORDER BY migration, table_nom, colonne;

SELECT current_database() AS base, current_schema() AS schema, current_setting('search_path') AS search_path;
