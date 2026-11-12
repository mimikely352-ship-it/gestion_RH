CREATE TABLE regime_travail (
 id BIGSERIAL PRIMARY KEY, dossier_id BIGINT NOT NULL REFERENCES dossier_employe(id),
 depuis DATE NOT NULL, debut TIME NOT NULL, fin TIME NOT NULL, pause_minutes INTEGER NOT NULL,
 jours VARCHAR(20) NOT NULL, UNIQUE(dossier_id,depuis),
 CHECK(fin>debut), CHECK(pause_minutes>=0 AND pause_minutes<EXTRACT(EPOCH FROM(fin-debut))/60)
);
CREATE TABLE jour_planning (
 id BIGSERIAL PRIMARY KEY, dossier_id BIGINT NOT NULL REFERENCES dossier_employe(id), jour DATE NOT NULL,
 repos BOOLEAN NOT NULL, debut TIME, fin TIME, pause_minutes INTEGER NOT NULL DEFAULT 0,
 UNIQUE(dossier_id,jour), CHECK(pause_minutes>=0),
 CHECK(repos OR (debut IS NOT NULL AND fin IS NOT NULL AND fin>debut AND pause_minutes<EXTRACT(EPOCH FROM(fin-debut))/60))
);
CREATE TABLE pointage_jour (
 id BIGSERIAL PRIMARY KEY, dossier_id BIGINT NOT NULL REFERENCES dossier_employe(id), jour DATE NOT NULL,
 entree TIMESTAMP NOT NULL, sortie TIMESTAMP, pause_minutes INTEGER NOT NULL DEFAULT 0,
 debut_prevu TIME, minutes_prevues INTEGER NOT NULL, supplementaires_validees INTEGER NOT NULL DEFAULT 0,
 modifie_par VARCHAR(150), motif_correction VARCHAR(255), version BIGINT NOT NULL DEFAULT 0,
 UNIQUE(dossier_id,jour), CHECK(entree::date=jour), CHECK(sortie IS NULL OR (sortie>entree AND sortie<=entree+INTERVAL '1 day')),
 CHECK(pause_minutes>=0 AND minutes_prevues>=0 AND supplementaires_validees>=0)
);
CREATE UNIQUE INDEX pointage_ouvert_unique ON pointage_jour(dossier_id) WHERE sortie IS NULL;
CREATE TABLE type_conge (
 id BIGSERIAL PRIMARY KEY, nom VARCHAR(80) NOT NULL UNIQUE, deduit_solde BOOLEAN NOT NULL,
 remunere BOOLEAN NOT NULL, justificatif_requis BOOLEAN NOT NULL, actif BOOLEAN NOT NULL DEFAULT TRUE
);
CREATE UNIQUE INDEX type_conge_nom_unique ON type_conge(lower(nom));
CREATE TABLE droit_conge (
 id BIGSERIAL PRIMARY KEY, dossier_id BIGINT NOT NULL REFERENCES dossier_employe(id),
 type_id BIGINT NOT NULL REFERENCES type_conge(id), annee INTEGER NOT NULL, jours INTEGER NOT NULL,
 motif VARCHAR(255) NOT NULL, modifie_par VARCHAR(150) NOT NULL,
 UNIQUE(dossier_id,type_id,annee), CHECK(jours BETWEEN 0 AND 366), CHECK(annee BETWEEN 2000 AND 2200)
);
CREATE TABLE demande_conge (
 id BIGSERIAL PRIMARY KEY, dossier_id BIGINT NOT NULL REFERENCES dossier_employe(id),
 type_id BIGINT NOT NULL REFERENCES type_conge(id), debut DATE NOT NULL, fin DATE NOT NULL,
 jours INTEGER NOT NULL, statut VARCHAR(20) NOT NULL, motif VARCHAR(500) NOT NULL,
 responsable_id BIGINT REFERENCES utilisateur(id), justificatif_chemin VARCHAR(255), soumise_le TIMESTAMP NOT NULL,
 decision_manager_le TIMESTAMP, decision_manager_par VARCHAR(150), decision_le TIMESTAMP,
 decision_par VARCHAR(150), commentaire VARCHAR(500), annulee_le TIMESTAMP, annulee_par VARCHAR(150),
 version BIGINT NOT NULL DEFAULT 0,
 CHECK(fin>=debut AND EXTRACT(YEAR FROM fin)=EXTRACT(YEAR FROM debut)), CHECK(jours BETWEEN 1 AND 366),
 CHECK(statut IN ('ATTENTE_MANAGER','ATTENTE_RH','ACCEPTE','REFUSE','ANNULE'))
);
CREATE INDEX demande_conge_dossier_periode ON demande_conge(dossier_id,debut,fin);
CREATE INDEX demande_conge_validation ON demande_conge(statut,responsable_id);
CREATE TABLE absence_employe (
 id BIGSERIAL PRIMARY KEY, dossier_id BIGINT NOT NULL REFERENCES dossier_employe(id),
 demande_id BIGINT UNIQUE REFERENCES demande_conge(id), debut DATE NOT NULL, fin DATE NOT NULL,
 libelle VARCHAR(120) NOT NULL, remuneree BOOLEAN NOT NULL, active BOOLEAN NOT NULL DEFAULT TRUE,
 justificatif_chemin VARCHAR(255), enregistree_par VARCHAR(150) NOT NULL,
 CHECK(fin>=debut AND EXTRACT(YEAR FROM fin)=EXTRACT(YEAR FROM debut))
);
CREATE INDEX absence_dossier_periode ON absence_employe(dossier_id,debut,fin) WHERE active;
INSERT INTO type_conge(nom,deduit_solde,remunere,justificatif_requis,actif) VALUES
 ('Congé annuel',TRUE,TRUE,FALSE,TRUE),('Congé sans solde',FALSE,FALSE,FALSE,TRUE);
