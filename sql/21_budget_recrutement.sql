-- Budget global de recrutement (enveloppe salariale annuelle) d'un besoin de personnel.
-- Facultatif sur les anciennes demandes ; obligatoire pour les nouveaux besoins (controle applicatif).
-- Idempotent : peut etre execute a la main puis rejoue par l'application sans effet.
ALTER TABLE demande ADD COLUMN IF NOT EXISTS budget_global_recrutement NUMERIC(19,2)
    CHECK (budget_global_recrutement > 0);
