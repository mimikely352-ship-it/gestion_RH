-- Migration MANUELLE : arrêter l'application et sauvegarder la base avant import.
-- Aucun nom d'entreprise n'est inventé et aucune offre n'est attribuée automatiquement.
\set ON_ERROR_STOP on
BEGIN;
CREATE TABLE IF NOT EXISTS entreprise (
    id BIGSERIAL PRIMARY KEY,
    nom VARCHAR(150) NOT NULL UNIQUE CHECK (length(trim(nom)) > 0)
);
ALTER TABLE offre_emploi ADD COLUMN IF NOT EXISTS entreprise_id BIGINT REFERENCES entreprise(id);
CREATE INDEX IF NOT EXISTS idx_offre_entreprise ON offre_emploi(entreprise_id);
COMMIT;
