-- Reprise des alertes RH manquantes pour les candidatures existantes.
-- Executer avec psql, application arretee. Peut etre relance sans doublons.
\set ON_ERROR_STOP on
SET client_encoding = 'UTF8';
BEGIN;
INSERT INTO notification (utilisateur_id, message, type, lien, lu)
SELECT u.id,
       'Candidature pour « ' || left(o.titre, 120) || ' ». Consultez le dossier.',
       'CANDIDATURE', '/rh/candidatures/' || c.id, FALSE
FROM candidature c
JOIN offre_emploi o ON o.id = c.offre_id
CROSS JOIN utilisateur u
WHERE u.role = 'RH' AND u.actif = TRUE
  AND NOT EXISTS (
    SELECT 1 FROM notification n
    WHERE n.utilisateur_id = u.id AND n.type = 'CANDIDATURE'
      AND n.lien = '/rh/candidatures/' || c.id
  );
COMMIT;
