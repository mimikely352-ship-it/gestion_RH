-- Le dossier salarié devient le référentiel central : identité, coordonnées, documents
-- administratifs (avec validité), historique de rémunération et journal d'audit.
\set ON_ERROR_STOP on
BEGIN;

-- ===== Identité et coordonnées portées par le dossier (et non plus par le dossier candidat) =====
ALTER TABLE dossier_employe ADD COLUMN IF NOT EXISTS matricule VARCHAR(20);
ALTER TABLE dossier_employe ADD COLUMN IF NOT EXISTS sexe VARCHAR(10);
ALTER TABLE dossier_employe ADD COLUMN IF NOT EXISTS date_naissance DATE;
ALTER TABLE dossier_employe ADD COLUMN IF NOT EXISTS lieu_naissance VARCHAR(150);
ALTER TABLE dossier_employe ADD COLUMN IF NOT EXISTS nationalite VARCHAR(80);
ALTER TABLE dossier_employe ADD COLUMN IF NOT EXISTS cin VARCHAR(30);
ALTER TABLE dossier_employe ADD COLUMN IF NOT EXISTS cin_delivre_le DATE;
ALTER TABLE dossier_employe ADD COLUMN IF NOT EXISTS cin_delivre_a VARCHAR(100);
ALTER TABLE dossier_employe ADD COLUMN IF NOT EXISTS situation_familiale VARCHAR(30);
ALTER TABLE dossier_employe ADD COLUMN IF NOT EXISTS nombre_enfants INTEGER;
ALTER TABLE dossier_employe ADD COLUMN IF NOT EXISTS adresse VARCHAR(255);
ALTER TABLE dossier_employe ADD COLUMN IF NOT EXISTS telephone VARCHAR(30);
ALTER TABLE dossier_employe ADD COLUMN IF NOT EXISTS email_personnel VARCHAR(150);
ALTER TABLE dossier_employe ADD COLUMN IF NOT EXISTS contact_urgence_nom VARCHAR(150);
ALTER TABLE dossier_employe ADD COLUMN IF NOT EXISTS contact_urgence_telephone VARCHAR(30);
ALTER TABLE dossier_employe ADD COLUMN IF NOT EXISTS numero_cnaps VARCHAR(30);
ALTER TABLE dossier_employe ADD COLUMN IF NOT EXISTS mis_a_jour_le TIMESTAMP;
ALTER TABLE dossier_employe ADD COLUMN IF NOT EXISTS mis_a_jour_par VARCHAR(150);

-- Reprise des informations saisies lors de la candidature.
UPDATE dossier_employe e SET
    telephone = COALESCE(e.telephone, c.telephone),
    date_naissance = COALESCE(e.date_naissance, c.date_naissance),
    lieu_naissance = COALESCE(e.lieu_naissance, c.lieu_naissance),
    cin = COALESCE(e.cin, c.cin),
    adresse = COALESCE(e.adresse, c.adresse)
FROM candidat c WHERE c.utilisateur_id = e.utilisateur_id;

-- Matricule unique attribué par la base à chaque nouveau dossier.
UPDATE dossier_employe SET matricule = 'EMP' || lpad(id::text, 5, '0') WHERE matricule IS NULL;
CREATE UNIQUE INDEX IF NOT EXISTS uk_dossier_matricule ON dossier_employe(matricule);
CREATE OR REPLACE FUNCTION attribuer_matricule() RETURNS trigger LANGUAGE plpgsql AS
'BEGIN IF NEW.matricule IS NULL THEN NEW.matricule := ''EMP'' || lpad(NEW.id::text, 5, ''0''); END IF; RETURN NEW; END;';
DROP TRIGGER IF EXISTS trg_dossier_matricule ON dossier_employe;
CREATE TRIGGER trg_dossier_matricule BEFORE INSERT ON dossier_employe
    FOR EACH ROW EXECUTE FUNCTION attribuer_matricule();

-- ===== Historique de rémunération =====
CREATE TABLE IF NOT EXISTS remuneration_employe (
    id BIGSERIAL PRIMARY KEY,
    dossier_id BIGINT NOT NULL REFERENCES dossier_employe(id),
    contrat_id BIGINT REFERENCES contrat_employe(id),
    date_effet DATE NOT NULL,
    salaire_brut NUMERIC(12,2) NOT NULL CHECK (salaire_brut >= 0),
    motif VARCHAR(150) NOT NULL,
    cree_le TIMESTAMP NOT NULL,
    cree_par VARCHAR(150) NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_remuneration_dossier ON remuneration_employe(dossier_id, date_effet);
INSERT INTO remuneration_employe(dossier_id, contrat_id, date_effet, salaire_brut, motif, cree_le, cree_par)
SELECT c.dossier_id, c.id, c.date_debut, c.salaire_brut, 'Salaire du contrat ' || c.type, c.cree_le, c.cree_par
FROM contrat_employe c
WHERE c.salaire_brut IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM remuneration_employe r WHERE r.contrat_id = c.id);

-- ===== Documents administratifs et validité =====
ALTER TABLE document ADD COLUMN IF NOT EXISTS numero VARCHAR(60);
ALTER TABLE document ADD COLUMN IF NOT EXISTS date_emission DATE;
ALTER TABLE document ADD COLUMN IF NOT EXISTS date_expiration DATE;
CREATE INDEX IF NOT EXISTS idx_document_expiration ON document(date_expiration) WHERE dossier_id IS NOT NULL;

-- ===== Journal d'audit du dossier salarié =====
CREATE TABLE IF NOT EXISTS audit_dossier (
    id BIGSERIAL PRIMARY KEY,
    dossier_id BIGINT NOT NULL REFERENCES dossier_employe(id),
    horodatage TIMESTAMP NOT NULL,
    acteur VARCHAR(150) NOT NULL,
    role VARCHAR(20),
    action VARCHAR(40) NOT NULL,
    detail VARCHAR(500)
);
CREATE INDEX IF NOT EXISTS idx_audit_dossier ON audit_dossier(dossier_id, horodatage DESC);

COMMIT;
