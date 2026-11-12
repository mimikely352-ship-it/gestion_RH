-- Blocs 3 et 4 : matricule EMP-AAAA-0001, embauche créée à l'acceptation de la proposition,
-- contrats « à venir » et renouvellements reliés au contrat précédent.
-- Idempotent : peut être exécuté à la main puis rejoué par l'application sans effet.
\set ON_ERROR_STOP on
BEGIN;

-- ===== Matricule EMP-AAAA-NNNN : année d'entrée + compteur (séquence) =====
CREATE SEQUENCE IF NOT EXISTS matricule_seq;

-- Anciens matricules (EMP00001...) renumérotés dans l'ordre de création des dossiers.
UPDATE dossier_employe d
   SET matricule = 'EMP-' || to_char(COALESCE(d.date_entree, CURRENT_DATE), 'YYYY') || '-'
                   || lpad(r.rang::text, GREATEST(4, length(r.rang::text)), '0')
  FROM (SELECT id, row_number() OVER (ORDER BY id) AS rang FROM dossier_employe) r
 WHERE r.id = d.id AND (d.matricule IS NULL OR d.matricule ~ '^EMP[0-9]{5}$');

-- La séquence repart après le plus grand compteur attribué (sans jamais reculer).
SELECT setval('matricule_seq', GREATEST(m.n, 1), m.n > 0)
  FROM (SELECT GREATEST(
          COALESCE((SELECT max(substring(matricule FROM '^EMP-[0-9]{4}-([0-9]+)$')::bigint) FROM dossier_employe), 0),
          (SELECT CASE WHEN is_called THEN last_value ELSE 0 END FROM matricule_seq)) AS n) m;

CREATE OR REPLACE FUNCTION attribuer_matricule() RETURNS trigger LANGUAGE plpgsql AS
'DECLARE n BIGINT; BEGIN IF NEW.matricule IS NULL THEN n := nextval(''matricule_seq''); NEW.matricule := ''EMP-'' || to_char(COALESCE(NEW.date_entree, CURRENT_DATE), ''YYYY'') || ''-'' || lpad(n::text, GREATEST(4, length(n::text)), ''0''); END IF; RETURN NEW; END;';
DROP TRIGGER IF EXISTS trg_dossier_matricule ON dossier_employe;
CREATE TRIGGER trg_dossier_matricule BEFORE INSERT ON dossier_employe
    FOR EACH ROW EXECUTE FUNCTION attribuer_matricule();

-- ===== Contrats : renouvellement relié au précédent, contrat « à venir » =====
ALTER TABLE contrat_employe ADD COLUMN IF NOT EXISTS contrat_precedent_id BIGINT REFERENCES contrat_employe(id);
CREATE UNIQUE INDEX IF NOT EXISTS uk_contrat_a_venir ON contrat_employe(dossier_id) WHERE statut = 'A_VENIR';

-- Reprise : chaque contrat existant est relié au contrat qui le précède dans le dossier.
UPDATE contrat_employe c SET contrat_precedent_id = p.precedent
  FROM (SELECT id, lag(id) OVER (PARTITION BY dossier_id ORDER BY date_debut, id) AS precedent FROM contrat_employe) p
 WHERE p.id = c.id AND c.contrat_precedent_id IS NULL AND p.precedent IS NOT NULL;

-- Réparation : un renouvellement saisi à l'avance était passé « en cours » avant son début
-- et avait terminé le contrat précédent par anticipation.
UPDATE contrat_employe SET statut = 'A_VENIR' WHERE statut = 'EN_COURS' AND date_debut > CURRENT_DATE;
UPDATE contrat_employe p SET statut = 'EN_COURS', date_fin_effective = NULL, motif_fin = NULL
 WHERE p.statut = 'TERMINE' AND p.date_fin_effective >= CURRENT_DATE
   AND EXISTS (SELECT 1 FROM contrat_employe s WHERE s.contrat_precedent_id = p.id AND s.statut = 'A_VENIR')
   AND NOT EXISTS (SELECT 1 FROM contrat_employe x WHERE x.dossier_id = p.dossier_id AND x.statut = 'EN_COURS');

-- ===== Proposition d'embauche : conditions complètes pour l'embauche automatique =====
ALTER TABLE candidature ADD COLUMN IF NOT EXISTS proposition_role VARCHAR(20);
ALTER TABLE candidature ADD COLUMN IF NOT EXISTS proposition_service_id BIGINT REFERENCES service(id);
ALTER TABLE candidature ADD COLUMN IF NOT EXISTS proposition_envoyee_par VARCHAR(150);

COMMIT;
