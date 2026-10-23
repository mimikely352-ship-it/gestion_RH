-- Application arretee : dates reelles, sans inventer celles des anciens dossiers.
\set ON_ERROR_STOP on
BEGIN;
ALTER TABLE candidature ADD COLUMN IF NOT EXISTS date_entree DATE;
ALTER TABLE demande ADD COLUMN IF NOT EXISTS date_execution TIMESTAMP;
COMMIT;
