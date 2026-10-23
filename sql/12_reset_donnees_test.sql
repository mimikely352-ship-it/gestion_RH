-- DESTRUCTIF : efface les donnees RH, comptes, candidatures et referentiels
-- de la base connectee, puis installe un petit jeu de demonstration.
-- Arreter l'application et sauvegarder la base avant execution.
-- A executer avec psql, sur une base de TEST dont le schema existe deja.
-- Les fichiers physiques du dossier uploads ne sont pas supprimes.
\set ON_ERROR_STOP on
SET client_encoding = 'UTF8';
\conninfo

-- Chemin relatif au present script, independant du dossier du terminal.
\ir 11_entreprises_offres.sql

BEGIN;

-- Ces tables racines entrainent la remise a zero des tables dependantes :
-- postes, services, diplomes, candidats, documents, offres, candidatures,
-- entretiens, tests, evaluations, demandes, validations, ordres,
-- notifications, dossiers employes et affectations, lorsqu'ils existent.
-- Le schema et le suivi app_schema_migration sont conserves.
TRUNCATE TABLE direction, niveau_diplome, diplome_filiere, utilisateur, entreprise
RESTART IDENTITY CASCADE;

-- Referentiels et organisation de base, sans comptes ni mots de passe SQL.
\ir 02_seed.sql

INSERT INTO entreprise (nom) VALUES
    ('Entreprise Alpha - TEST'),
    ('Entreprise Beta - TEST');

-- Deux postes differents dans Alpha : le second depot doit etre refuse.
-- Un poste dans Beta : le depot doit etre accepte pour le meme candidat.
INSERT INTO offre_emploi
    (entreprise_id, poste_id, titre, description, nb_postes,
     experience_min_mois, seuil_acceptation, date_publication, date_limite, statut)
SELECT e.id, p.id, jeu.titre, jeu.description, 1,
       0, 60, CURRENT_DATE, CURRENT_DATE + 90, 'PUBLIEE'
FROM (VALUES
    ('Entreprise Alpha - TEST', 'Developpeur Java - Alpha',
     'Concevoir et maintenir des services Java. Debutants acceptes.', 'Développeur Backend Java'),
    ('Entreprise Alpha - TEST', 'Chauffeur - Alpha',
     'Assurer les transports et les livraisons. Debutants acceptes.', 'Chauffeur'),
    ('Entreprise Beta - TEST', 'Developpeur Java - Beta',
     'Participer au developpement des applications. Debutants acceptes.', 'Développeur Backend Java')
) AS jeu(entreprise, titre, description, poste)
JOIN entreprise e ON e.nom = jeu.entreprise
JOIN poste p ON p.intitule = jeu.poste;

-- Une anomalie d'insertion annule aussi la suppression des anciennes donnees.
DO $$
BEGIN
    IF (SELECT COUNT(*) FROM offre_emploi) <> 3 THEN
        RAISE EXCEPTION 'Le jeu de test doit contenir exactement trois offres. Remise a zero annulee.';
    END IF;
END $$;

COMMIT;

SELECT o.id, e.nom AS entreprise, o.titre, o.statut
FROM offre_emploi o JOIN entreprise e ON e.id = o.entreprise_id
ORDER BY o.id;

\echo 'Jeu de test installe : 2 entreprises et 3 offres.'
\echo 'Redemarrer en mode local avec app.demo-data.enabled=true.'
\echo 'Les comptes internes de demonstration seront recrees (password123).'
\echo 'Creer un nouveau compte candidat sur le site pour tester les depots.'
