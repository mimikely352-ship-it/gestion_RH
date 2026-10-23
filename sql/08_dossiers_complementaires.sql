\set ON_ERROR_STOP on
BEGIN;
ALTER TABLE document ADD COLUMN libelle VARCHAR(100);
CREATE TABLE offre_piece_requise (
  offre_id BIGINT NOT NULL REFERENCES offre_emploi(id),
  libelle VARCHAR(100) NOT NULL,
  PRIMARY KEY (offre_id, libelle)
);
CREATE TABLE candidature_document (
  candidature_id BIGINT NOT NULL REFERENCES candidature(id),
  document_id BIGINT NOT NULL REFERENCES document(id),
  PRIMARY KEY (candidature_id, document_id)
);
COMMIT;
