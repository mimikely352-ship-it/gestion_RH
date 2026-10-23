-- Application arretee. Appliquer apres 09. Ne pas remplacer les dates historiques absentes.
\set ON_ERROR_STOP on
BEGIN;
ALTER TABLE demande ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE demande ADD COLUMN IF NOT EXISTS affectation_origine_id BIGINT;
ALTER TABLE demande ADD COLUMN IF NOT EXISTS date_effet DATE;
ALTER TABLE poste ADD COLUMN IF NOT EXISTS mobilite_ouverte BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE ordre ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE entretien ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE evaluation ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE passage_test ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE utilisateur ADD COLUMN IF NOT EXISTS tokens_valides_depuis TIMESTAMP;

CREATE TABLE IF NOT EXISTS dossier_employe (
 id BIGSERIAL PRIMARY KEY, utilisateur_id BIGINT NOT NULL UNIQUE REFERENCES utilisateur(id),
 nom VARCHAR(100) NOT NULL, prenom VARCHAR(100) NOT NULL, date_entree DATE, version BIGINT NOT NULL DEFAULT 0
);
CREATE TABLE IF NOT EXISTS affectation_employe (
 id BIGSERIAL PRIMARY KEY, dossier_id BIGINT NOT NULL REFERENCES dossier_employe(id),
 poste_id BIGINT NOT NULL REFERENCES poste(id), service_id BIGINT REFERENCES service(id),
 poste_libelle VARCHAR(150) NOT NULL, service_libelle VARCHAR(100), date_debut DATE, date_fin DATE,
 actuelle BOOLEAN NOT NULL, origine VARCHAR(20) NOT NULL, source_id BIGINT,
 CHECK(date_fin IS NULL OR date_debut IS NULL OR date_fin >= date_debut),
 CHECK(NOT actuelle OR date_fin IS NULL), UNIQUE(origine, source_id)
);
CREATE UNIQUE INDEX IF NOT EXISTS affectation_unique_actuelle ON affectation_employe(dossier_id) WHERE actuelle;
CREATE TABLE IF NOT EXISTS correction_date_entree (
 id BIGSERIAL PRIMARY KEY, dossier_id BIGINT NOT NULL REFERENCES dossier_employe(id),
 ancienne_date DATE, nouvelle_date DATE NOT NULL, auteur VARCHAR(150) NOT NULL, horodatage TIMESTAMP NOT NULL
);

INSERT INTO dossier_employe(utilisateur_id, nom, prenom, date_entree)
SELECT u.id, c.nom, c.prenom, MIN(a.date_entree)
FROM utilisateur u JOIN candidat c ON c.utilisateur_id=u.id
JOIN candidature a ON a.candidat_id=c.id AND a.statut='EMBAUCHE'
GROUP BY u.id,c.nom,c.prenom ON CONFLICT(utilisateur_id) DO NOTHING;

-- Sources prouvees. Les intitules sont figes a la migration ; leur valeur avant celle-ci n'est pas connue.
WITH sources AS (
 SELECT e.id AS dossier_id, a.offre_id, o.poste_id, a.date_entree AS debut,
 'EMBAUCHE'::varchar AS origine, a.id AS source_id
 FROM dossier_employe e JOIN candidat c ON c.utilisateur_id=e.utilisateur_id
 JOIN candidature a ON a.candidat_id=c.id AND a.statut='EMBAUCHE' JOIN offre_emploi o ON o.id=a.offre_id
 UNION ALL
 SELECT e.id, NULL, d.poste_cible_id, COALESCE(d.date_effet,d.date_execution::date),d.type,d.id
 FROM dossier_employe e JOIN demande d ON d.auteur_id=e.utilisateur_id
 WHERE d.statut='EXECUTEE' AND d.type IN('TRANSFERT','PROMOTION') AND d.poste_cible_id IS NOT NULL
), classees AS (
 SELECT s.*, COUNT(*) OVER(PARTITION BY dossier_id) AS total,
 COUNT(*) FILTER(WHERE debut IS NULL) OVER(PARTITION BY dossier_id) AS inconnues,
 ROW_NUMBER() OVER(PARTITION BY dossier_id ORDER BY debut DESC NULLS LAST,source_id DESC) AS rang,
 LEAD(debut) OVER(PARTITION BY dossier_id ORDER BY debut NULLS FIRST,source_id) AS fin
 FROM sources s
)
INSERT INTO affectation_employe(dossier_id,poste_id,service_id,poste_libelle,service_libelle,date_debut,date_fin,actuelle,origine,source_id)
SELECT c.dossier_id,p.id,s.id,p.intitule,s.nom,c.debut,
CASE WHEN (c.total=1 OR c.inconnues=0) AND c.rang=1 THEN NULL ELSE c.fin END,
(c.total=1 OR c.inconnues=0) AND c.rang=1,c.origine,c.source_id
FROM classees c JOIN poste p ON p.id=c.poste_id LEFT JOIN section sec ON sec.id=p.section_id
LEFT JOIN service s ON s.id=sec.service_id
ON CONFLICT(origine,source_id) DO NOTHING;

UPDATE demande d SET affectation_origine_id=a.id
FROM dossier_employe e JOIN affectation_employe a ON a.dossier_id=e.id AND a.actuelle
WHERE d.auteur_id=e.utilisateur_id AND d.type IN('TRANSFERT','PROMOTION')
 AND d.statut NOT IN('EXECUTEE','REJETEE') AND d.affectation_origine_id IS NULL;

CREATE INDEX IF NOT EXISTS idx_demande_statut_type ON demande(statut,type);
COMMIT;
