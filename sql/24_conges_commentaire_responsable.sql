-- Bloc 7 : conserver le commentaire du responsable indépendamment de la décision RH.
-- À exécuter manuellement après 23_conges_annulation.sql, avant de démarrer cette version.
-- Aucun ajout de compte, de droit de congé ou de donnée de démonstration.
\set ON_ERROR_STOP on
\encoding UTF8
BEGIN;
ALTER TABLE demande_conge ADD COLUMN IF NOT EXISTS commentaire_manager VARCHAR(500);
-- Les anciens refus du responsable possèdent déjà un commentaire final : le conserver aux deux étapes.
UPDATE demande_conge
   SET commentaire_manager = commentaire
 WHERE commentaire_manager IS NULL AND decision_manager_par IS NOT NULL
   AND decision_manager_par = decision_par AND commentaire IS NOT NULL;
COMMIT;
