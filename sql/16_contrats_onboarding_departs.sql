-- Contrats successifs, onboarding (checklist + workflow) et départs (démission / renvoi).
-- Un employé garde tout l'historique de ses contrats : un nouveau contrat clôt le précédent
-- au lieu d'écraser un simple "type de contrat".
\set ON_ERROR_STOP on
BEGIN;

-- Remplacée par onboarding / onboarding_tache.
DROP TABLE IF EXISTS element_integration_employe;

CREATE TABLE IF NOT EXISTS contrat_employe (
    id BIGSERIAL PRIMARY KEY,
    dossier_id BIGINT NOT NULL REFERENCES dossier_employe(id),
    type VARCHAR(20) NOT NULL,
    date_debut DATE NOT NULL,
    date_fin_prevue DATE,
    date_fin_effective DATE,
    statut VARCHAR(20) NOT NULL,
    salaire_brut NUMERIC(12,2),
    motif_fin VARCHAR(255),
    date_signature DATE,
    cree_le TIMESTAMP NOT NULL,
    cree_par VARCHAR(150) NOT NULL,
    version BIGINT,
    CHECK (date_fin_prevue IS NULL OR date_fin_prevue >= date_debut)
);
CREATE INDEX IF NOT EXISTS idx_contrat_dossier ON contrat_employe(dossier_id);
CREATE UNIQUE INDEX IF NOT EXISTS uk_contrat_en_cours ON contrat_employe(dossier_id) WHERE statut = 'EN_COURS';

CREATE TABLE IF NOT EXISTS onboarding (
    id BIGSERIAL PRIMARY KEY,
    dossier_id BIGINT NOT NULL UNIQUE REFERENCES dossier_employe(id),
    statut VARCHAR(20) NOT NULL,
    date_debut DATE NOT NULL,
    termine_le TIMESTAMP
);

CREATE TABLE IF NOT EXISTS onboarding_tache (
    id BIGSERIAL PRIMARY KEY,
    onboarding_id BIGINT NOT NULL REFERENCES onboarding(id) ON DELETE CASCADE,
    code VARCHAR(30) NOT NULL,
    statut VARCHAR(20) NOT NULL,
    fait_le TIMESTAMP,
    fait_par VARCHAR(150),
    CONSTRAINT uk_onboarding_tache UNIQUE (onboarding_id, code)
);

CREATE TABLE IF NOT EXISTS depart_employe (
    id BIGSERIAL PRIMARY KEY,
    dossier_id BIGINT NOT NULL REFERENCES dossier_employe(id),
    type VARCHAR(20) NOT NULL,
    motif VARCHAR(40),
    description TEXT,
    date_notification DATE NOT NULL,
    date_effet DATE NOT NULL,
    statut VARCHAR(20) NOT NULL,
    cree_le TIMESTAMP NOT NULL,
    cree_par VARCHAR(150) NOT NULL,
    cloture_le TIMESTAMP,
    cloture_par VARCHAR(150),
    version BIGINT,
    CHECK (date_effet >= date_notification)
);
CREATE INDEX IF NOT EXISTS idx_depart_dossier ON depart_employe(dossier_id);
CREATE UNIQUE INDEX IF NOT EXISTS uk_depart_en_cours ON depart_employe(dossier_id) WHERE statut = 'EN_COURS';

-- Documents de l'employé (contrat signé, lettre de démission...), en plus des pièces du candidat.
ALTER TABLE document ADD COLUMN IF NOT EXISTS dossier_id BIGINT REFERENCES dossier_employe(id);
ALTER TABLE document ADD COLUMN IF NOT EXISTS contrat_id BIGINT REFERENCES contrat_employe(id);
ALTER TABLE document ADD COLUMN IF NOT EXISTS depart_id BIGINT REFERENCES depart_employe(id);
CREATE INDEX IF NOT EXISTS idx_document_dossier ON document(dossier_id);

COMMIT;
