-- psql : appliquer une fois sur une base existante, application arretee.
\set ON_ERROR_STOP on
SET client_encoding = 'UTF8';
BEGIN;
ALTER TABLE poste ADD COLUMN categorie VARCHAR(150);
-- Ne pas transformer silencieusement une ancienne valeur negative.
-- Si cette contrainte echoue, corriger les valeurs puis relancer le script.
ALTER TABLE offre_emploi ADD CONSTRAINT offre_experience_non_negative
  CHECK (experience_min_mois IS NULL OR experience_min_mois >= 0);
ALTER TABLE candidature ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
CREATE TABLE candidature_diplome (
  candidature_id BIGINT NOT NULL REFERENCES candidature(id),
  diplome_id BIGINT NOT NULL REFERENCES diplome(id),
  PRIMARY KEY (candidature_id, diplome_id)
);
-- Reprise des diplomes actuels : approximation pour les anciens dossiers.
INSERT INTO candidature_diplome (candidature_id, diplome_id)
SELECT c.id, d.diplome_id FROM candidature c
JOIN candidat_diplome d ON d.candidat_id = c.candidat_id;
-- Ne pas affecter le dernier CV aux anciennes candidatures : ce ne serait
-- pas une preuve du CV reellement depose. Ne pas modifier leurs statuts.
COMMIT;
