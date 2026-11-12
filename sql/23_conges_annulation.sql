-- Bloc 7 : le motif d'annulation d'un congé est conservé avec la demande, pour être
-- consultable dans l'historique par le salarié et les RH.
-- Idempotent : peut être exécuté à la main puis rejoué par l'application sans effet.
\set ON_ERROR_STOP on
\encoding UTF8
BEGIN;

ALTER TABLE demande_conge ADD COLUMN IF NOT EXISTS motif_annulation VARCHAR(500);

-- Reprise des annulations déjà effectuées : le motif figurait seulement au journal du dossier
-- (« Annulation congé <id> : <motif> »).
UPDATE demande_conge c
   SET motif_annulation = left(substring(a.detail FROM length('Annulation congé ' || c.id || ' : ') + 1), 500)
  FROM audit_dossier a
 WHERE c.statut = 'ANNULE' AND c.motif_annulation IS NULL
   AND a.dossier_id = c.dossier_id AND a.action = 'CONGE_MODIFIE'
   AND a.detail LIKE 'Annulation congé ' || c.id || ' : %';

COMMIT;
