ALTER TABLE candidature ADD COLUMN IF NOT EXISTS proposition_statut VARCHAR(20)
  CHECK (proposition_statut IN ('EN_ATTENTE','ACCEPTEE','REFUSEE'));
ALTER TABLE candidature ADD COLUMN IF NOT EXISTS proposition_type_contrat VARCHAR(25);
ALTER TABLE candidature ADD COLUMN IF NOT EXISTS proposition_salaire NUMERIC(19,2) CHECK (proposition_salaire > 0);
ALTER TABLE candidature ADD COLUMN IF NOT EXISTS proposition_debut DATE;
ALTER TABLE candidature ADD COLUMN IF NOT EXISTS proposition_fin DATE;
ALTER TABLE candidature ADD COLUMN IF NOT EXISTS proposition_envoyee_le TIMESTAMP;
ALTER TABLE candidature ADD COLUMN IF NOT EXISTS proposition_repondue_le TIMESTAMP;
