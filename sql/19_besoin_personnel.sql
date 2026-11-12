-- Donnees facultatives sur les anciennes demandes ; requises pour les nouveaux besoins.
ALTER TABLE demande ADD COLUMN IF NOT EXISTS effectif_demande INTEGER CHECK (effectif_demande > 0);
ALTER TABLE demande ADD COLUMN IF NOT EXISTS salaire_mensuel_par_poste NUMERIC(19,2) CHECK (salaire_mensuel_par_poste > 0);
ALTER TABLE demande ADD COLUMN IF NOT EXISTS date_souhaitee DATE;
ALTER TABLE demande ADD COLUMN IF NOT EXISTS motif_besoin VARCHAR(20) CHECK (motif_besoin IN ('CREATION','REMPLACEMENT'));
