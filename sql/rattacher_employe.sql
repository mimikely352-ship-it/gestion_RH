-- Correction ciblee d'un compte existant, dans psql.
-- Consulter les services, puis saisir l'identifiant du service reel de l'employe.
\set ON_ERROR_STOP on
SELECT s.id, s.nom, u.email AS responsable
FROM service s LEFT JOIN utilisateur u ON u.id = s.chef_service_id
ORDER BY s.nom;
\prompt 'Email du compte employe : ' email_employe
\prompt 'Identifiant du service de rattachement : ' service_employe
BEGIN;
UPDATE utilisateur u SET service_id = s.id
FROM service s JOIN utilisateur chef ON chef.id = s.chef_service_id
WHERE u.email = :'email_employe' AND u.role = 'EMPLOYE'
  AND s.id = :'service_employe'::bigint
  AND chef.role = 'CHEF_SERVICE' AND chef.actif = TRUE
RETURNING u.email, s.nom AS service;
-- UPDATE 1 : rattachement effectue. UPDATE 0 : compte ou service non admissible.
COMMIT;
