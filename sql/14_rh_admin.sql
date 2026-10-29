-- Responsables d'unites organisationnelles consultables/assignables par le RH :
-- directeur (direction) et chef de departement (departement). chef de service existait deja.
\set ON_ERROR_STOP on
BEGIN;
ALTER TABLE direction ADD COLUMN IF NOT EXISTS directeur_id BIGINT REFERENCES utilisateur(id);
ALTER TABLE departement ADD COLUMN IF NOT EXISTS chef_departement_id BIGINT REFERENCES utilisateur(id);
COMMIT;
