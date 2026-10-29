-- Rattrape les ecarts entre le schema 01_schema.sql actuel et les bases creees avant
-- l'ajout du recrutement etendu (diplomes/pieces complementaires sur candidature, dossier
-- complementaire par offre). Idempotent : peut etre rejoue sans erreur.
\set ON_ERROR_STOP on
BEGIN;
ALTER TABLE candidature ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;

CREATE TABLE IF NOT EXISTS candidature_diplome (
  candidature_id BIGINT NOT NULL REFERENCES candidature(id),
  diplome_id BIGINT NOT NULL REFERENCES diplome(id),
  PRIMARY KEY (candidature_id, diplome_id)
);

ALTER TABLE poste ADD COLUMN IF NOT EXISTS categorie VARCHAR(150);
ALTER TABLE poste ALTER COLUMN section_id DROP NOT NULL;
ALTER TABLE offre_emploi DROP CONSTRAINT IF EXISTS offre_experience_non_negative;
ALTER TABLE offre_emploi ADD CONSTRAINT offre_experience_non_negative
  CHECK (experience_min_mois IS NULL OR experience_min_mois >= 0);

ALTER TABLE document ADD COLUMN IF NOT EXISTS libelle VARCHAR(100);
CREATE TABLE IF NOT EXISTS offre_piece_requise (
  offre_id BIGINT NOT NULL REFERENCES offre_emploi(id),
  libelle VARCHAR(100) NOT NULL,
  PRIMARY KEY (offre_id, libelle)
);
CREATE TABLE IF NOT EXISTS candidature_document (
  candidature_id BIGINT NOT NULL REFERENCES candidature(id),
  document_id BIGINT NOT NULL REFERENCES document(id),
  PRIMARY KEY (candidature_id, document_id)
);
COMMIT;
