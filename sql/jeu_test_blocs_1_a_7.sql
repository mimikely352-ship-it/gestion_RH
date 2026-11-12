-- ============================================================================
-- Oria RH — Jeu de données de test des blocs 1 à 7
-- ============================================================================
-- À QUOI SERT CE SCRIPT
--   Il crée dans la base sirh un jeu de données cohérent pour tester chaque bloc
--   avec plusieurs comptes : besoins de personnel (bloc 1), offres et candidatures
--   à chaque étape (bloc 2), propositions d'embauche (bloc 3), salariés avec
--   contrats, intégration et rémunération (blocs 4 et 5), planning et pointages
--   (bloc 6), droits et demandes de congé, absences (bloc 7).
--   Ce sont les situations utilisées pendant les tests de mise au point.
--
-- PRÉREQUIS
--   1. Base sirh installée selon le README (parcours B : 12_reset_donnees_test.sql).
--   2. Application démarrée au moins une fois avec cette version : migrations 09 à
--      22 appliquées et comptes de démonstration créés (rh@sirh.local, ...).
--   Le script vérifie ces prérequis et s'arrête sans rien modifier s'ils manquent.
--
-- EXÉCUTION (depuis le dossier gestion_RH)
--   psql -h localhost -p 5432 -U postgres -d sirh -v ON_ERROR_STOP=1 -f sql/jeu_test_blocs_1_a_7.sql
--
-- REJOUABLE
--   Le script supprime d'abord l'ancien jeu de test et tout ce qui en est issu
--   pendant les essais (embauche d'un candidat de test, pointages, congés...),
--   puis le recrée à neuf. Les données réelles et les comptes de démonstration
--   ne sont pas touchés. Une seule transaction : en cas d'erreur, rien ne change.
--
-- SUPPRIMER SEULEMENT LE JEU DE TEST
--   psql -h localhost -p 5432 -U postgres -d sirh -v ON_ERROR_STOP=1 -v nettoyage_seul=1 -f sql/jeu_test_blocs_1_a_7.sql
--
-- REPÈRES
--   Comptes de test : adresses en @test.sirh.local, mot de passe « password123 »
--   (comme les comptes de démonstration).
--   Besoins et offres de test : description ou titre commençant par « [Test] ».
--   Les dates sont calculées à partir du jour d'exécution.
--   Aucun fichier PDF n'est créé : les documents et justificatifs se déposent
--   depuis l'application. Les personnes sont fictives.
-- ============================================================================

\set ON_ERROR_STOP on
\encoding UTF8
-- Haché BCrypt de « password123 », produit par l'encodeur de l'application.
\set mot_de_passe '$2a$10$QwKRkaWLG/T9OIU4kmF.ZOSm.qXoBjDkOcEMOcoKjcGX7xHSGJCUG'

BEGIN;
SET LOCAL client_min_messages = warning;

-- ============================================================================
-- 0. PRÉREQUIS (le script s'arrête ici si un élément manque)
-- ============================================================================
DO $$
DECLARE
    manquants text;
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns
                   WHERE table_schema = current_schema() AND table_name = 'contrat_employe'
                     AND column_name = 'contrat_precedent_id')
       OR NOT EXISTS (SELECT 1 FROM information_schema.columns
                   WHERE table_schema = current_schema() AND table_name = 'candidature'
                     AND column_name = 'proposition_role') THEN
        RAISE EXCEPTION 'Base non à jour : démarrez une fois l''application (migrations jusqu''à 22) avant ce script.';
    END IF;

    SELECT string_agg(e, ', ') INTO manquants
      FROM unnest(ARRAY['rh@sirh.local', 'dg@sirh.local', 'direction@sirh.local',
                        'chef-service@sirh.local', 'employe@sirh.local']) AS e
     WHERE NOT EXISTS (SELECT 1 FROM utilisateur u WHERE u.email = e);
    IF manquants IS NOT NULL THEN
        RAISE EXCEPTION 'Comptes de démonstration absents : %. Démarrez une fois l''application (app.demo-data.enabled=true).', manquants;
    END IF;

    SELECT string_agg(element, ', ') INTO manquants FROM (
        SELECT 'service « Service Développement » avec un chef' AS element
         WHERE NOT EXISTS (SELECT 1 FROM service WHERE nom = 'Service Développement' AND chef_service_id IS NOT NULL)
        UNION ALL SELECT 'service « Service Transport » avec un chef'
         WHERE NOT EXISTS (SELECT 1 FROM service WHERE nom = 'Service Transport' AND chef_service_id IS NOT NULL)
        UNION ALL SELECT 'poste « Développeur Backend Java »'
         WHERE NOT EXISTS (SELECT 1 FROM poste WHERE intitule = 'Développeur Backend Java')
        UNION ALL SELECT 'poste « Chauffeur »'
         WHERE NOT EXISTS (SELECT 1 FROM poste WHERE intitule = 'Chauffeur')
        UNION ALL SELECT 'entreprise « Entreprise Alpha - TEST »'
         WHERE NOT EXISTS (SELECT 1 FROM entreprise WHERE nom = 'Entreprise Alpha - TEST')
        UNION ALL SELECT 'entreprise « Entreprise Beta - TEST »'
         WHERE NOT EXISTS (SELECT 1 FROM entreprise WHERE nom = 'Entreprise Beta - TEST')
        UNION ALL SELECT 'type de congé « Congé annuel »'
         WHERE NOT EXISTS (SELECT 1 FROM type_conge WHERE nom = 'Congé annuel')
    ) absents;
    IF manquants IS NOT NULL THEN
        RAISE EXCEPTION 'Référentiel incomplet (%) : installez la base selon le parcours B du README.', manquants;
    END IF;
END $$;

-- ============================================================================
-- 1. SUPPRESSION DE L'ANCIEN JEU DE TEST
--    Repères : comptes @test.sirh.local, besoins et offres « [Test] », et tout ce
--    qui en découle (y compris ce que l'application a créé pendant les essais).
-- ============================================================================
CREATE TEMP TABLE jeu_utilisateurs ON COMMIT DROP AS
    SELECT id FROM utilisateur WHERE email LIKE '%@test.sirh.local';
CREATE TEMP TABLE jeu_demandes ON COMMIT DROP AS
    SELECT id FROM demande
     WHERE description LIKE '[Test]%' OR auteur_id IN (SELECT id FROM jeu_utilisateurs);
CREATE TEMP TABLE jeu_offres ON COMMIT DROP AS
    SELECT id FROM offre_emploi
     WHERE titre LIKE '[Test]%' OR demande_origine_id IN (SELECT id FROM jeu_demandes);
CREATE TEMP TABLE jeu_candidats ON COMMIT DROP AS
    SELECT id FROM candidat WHERE utilisateur_id IN (SELECT id FROM jeu_utilisateurs);
CREATE TEMP TABLE jeu_candidatures ON COMMIT DROP AS
    SELECT id FROM candidature
     WHERE candidat_id IN (SELECT id FROM jeu_candidats) OR offre_id IN (SELECT id FROM jeu_offres);
CREATE TEMP TABLE jeu_tests ON COMMIT DROP AS
    SELECT id FROM test WHERE offre_id IN (SELECT id FROM jeu_offres);
CREATE TEMP TABLE jeu_dossiers ON COMMIT DROP AS
    SELECT id FROM dossier_employe WHERE utilisateur_id IN (SELECT id FROM jeu_utilisateurs);

-- 1.a Dossiers salariés de test (blocs 4 à 7), y compris ceux nés d'une embauche de test
DELETE FROM absence_employe      WHERE dossier_id IN (SELECT id FROM jeu_dossiers);
DELETE FROM demande_conge        WHERE dossier_id IN (SELECT id FROM jeu_dossiers);
DELETE FROM droit_conge          WHERE dossier_id IN (SELECT id FROM jeu_dossiers);
DELETE FROM pointage_jour        WHERE dossier_id IN (SELECT id FROM jeu_dossiers);
DELETE FROM jour_planning        WHERE dossier_id IN (SELECT id FROM jeu_dossiers);
DELETE FROM regime_travail       WHERE dossier_id IN (SELECT id FROM jeu_dossiers);
DELETE FROM onboarding           WHERE dossier_id IN (SELECT id FROM jeu_dossiers);  -- tâches supprimées en cascade
DELETE FROM remuneration_employe WHERE dossier_id IN (SELECT id FROM jeu_dossiers);
DELETE FROM document
 WHERE dossier_id IN (SELECT id FROM jeu_dossiers)
    OR contrat_id IN (SELECT id FROM contrat_employe WHERE dossier_id IN (SELECT id FROM jeu_dossiers))
    OR depart_id  IN (SELECT id FROM depart_employe  WHERE dossier_id IN (SELECT id FROM jeu_dossiers));
UPDATE contrat_employe SET contrat_precedent_id = NULL WHERE dossier_id IN (SELECT id FROM jeu_dossiers);
DELETE FROM contrat_employe        WHERE dossier_id IN (SELECT id FROM jeu_dossiers);
DELETE FROM depart_employe         WHERE dossier_id IN (SELECT id FROM jeu_dossiers);
DELETE FROM affectation_employe    WHERE dossier_id IN (SELECT id FROM jeu_dossiers);
DELETE FROM audit_dossier          WHERE dossier_id IN (SELECT id FROM jeu_dossiers);
DELETE FROM correction_date_entree WHERE dossier_id IN (SELECT id FROM jeu_dossiers);
DELETE FROM dossier_employe        WHERE id IN (SELECT id FROM jeu_dossiers);

-- 1.b Candidatures de test et leurs étapes (entretiens, tests, évaluations, pièces)
DELETE FROM reponse_test
 WHERE passage_id IN (SELECT id FROM passage_test WHERE candidature_id IN (SELECT id FROM jeu_candidatures)
                                                     OR test_id IN (SELECT id FROM jeu_tests))
    OR question_id IN (SELECT id FROM question_test WHERE test_id IN (SELECT id FROM jeu_tests));
DELETE FROM passage_test
 WHERE candidature_id IN (SELECT id FROM jeu_candidatures) OR test_id IN (SELECT id FROM jeu_tests);
DELETE FROM reponse_evaluation
 WHERE evaluation_id IN (SELECT id FROM evaluation WHERE candidature_id IN (SELECT id FROM jeu_candidatures));
DELETE FROM choix_evaluation
 WHERE question_id IN (SELECT q.id FROM question_evaluation q JOIN evaluation e ON e.id = q.evaluation_id
                        WHERE e.candidature_id IN (SELECT id FROM jeu_candidatures));
DELETE FROM question_evaluation
 WHERE evaluation_id IN (SELECT id FROM evaluation WHERE candidature_id IN (SELECT id FROM jeu_candidatures));
DELETE FROM evaluation           WHERE candidature_id IN (SELECT id FROM jeu_candidatures);
DELETE FROM entretien            WHERE candidature_id IN (SELECT id FROM jeu_candidatures);
DELETE FROM candidature_document WHERE candidature_id IN (SELECT id FROM jeu_candidatures);
DELETE FROM candidature_diplome  WHERE candidature_id IN (SELECT id FROM jeu_candidatures);
DELETE FROM candidature          WHERE id IN (SELECT id FROM jeu_candidatures);

-- 1.c Tests, offres et besoins de test
DELETE FROM choix_test    WHERE question_id IN (SELECT id FROM question_test WHERE test_id IN (SELECT id FROM jeu_tests));
DELETE FROM question_test WHERE test_id IN (SELECT id FROM jeu_tests);
DELETE FROM test          WHERE id IN (SELECT id FROM jeu_tests);
DELETE FROM offre_diplome_requis WHERE offre_id IN (SELECT id FROM jeu_offres);
DELETE FROM offre_piece_requise  WHERE offre_id IN (SELECT id FROM jeu_offres);
DELETE FROM offre_emploi         WHERE id IN (SELECT id FROM jeu_offres);
DELETE FROM validation_demande
 WHERE demande_id IN (SELECT id FROM jeu_demandes) OR valideur_id IN (SELECT id FROM jeu_utilisateurs);
DELETE FROM demande WHERE id IN (SELECT id FROM jeu_demandes);

-- 1.d Profils candidats et comptes de test
DELETE FROM candidat_diplome WHERE candidat_id IN (SELECT id FROM jeu_candidats);
DELETE FROM experience       WHERE candidat_id IN (SELECT id FROM jeu_candidats);
DELETE FROM document         WHERE candidat_id IN (SELECT id FROM jeu_candidats);
DELETE FROM candidat         WHERE id IN (SELECT id FROM jeu_candidats);
DELETE FROM notification     WHERE utilisateur_id IN (SELECT id FROM jeu_utilisateurs);
UPDATE service       SET chef_service_id     = NULL WHERE chef_service_id     IN (SELECT id FROM jeu_utilisateurs);
UPDATE departement   SET chef_departement_id = NULL WHERE chef_departement_id IN (SELECT id FROM jeu_utilisateurs);
UPDATE direction     SET directeur_id        = NULL WHERE directeur_id        IN (SELECT id FROM jeu_utilisateurs);
UPDATE demande_conge SET responsable_id      = NULL WHERE responsable_id      IN (SELECT id FROM jeu_utilisateurs);
DELETE FROM utilisateur WHERE id IN (SELECT id FROM jeu_utilisateurs);

-- Facultatif : les deux demandes créées par employe@sirh.local lors du test du 12/11/2026
-- (« Remplacement chauffeur » et « Formation Excel », statut SOUMISE). Retirez les « -- »
-- des trois lignes suivantes pour les supprimer aussi.
-- DELETE FROM validation_demande WHERE demande_id IN (SELECT d.id FROM demande d JOIN utilisateur u ON u.id = d.auteur_id
--        WHERE u.email = 'employe@sirh.local' AND d.description IN ('Remplacement chauffeur', 'Formation Excel'));
-- DELETE FROM demande d USING utilisateur u WHERE u.id = d.auteur_id AND u.email = 'employe@sirh.local' AND d.description IN ('Remplacement chauffeur', 'Formation Excel');

\if :{?nettoyage_seul}
COMMIT;
\echo 'Jeu de test supprimé. Aucune donnée de test recréée (option nettoyage_seul).'
\quit
\endif

-- ============================================================================
-- 2. REPÈRES DE DATES (calculés à partir du jour d'exécution)
--    jeu_jours : jours ouvrés passés, du plus récent (rang 1) au plus ancien.
--    jeu_dates : lundi d'une semaine de congés située au moins 14 jours plus tard,
--                choisi pour que la semaine et le lundi suivant restent dans la même année.
-- ============================================================================
CREATE TEMP TABLE jeu_jours ON COMMIT DROP AS
SELECT (row_number() OVER (ORDER BY d DESC))::int AS rang, d::date AS jour
  FROM generate_series(current_date - 30, current_date - 1, interval '1 day') AS d
 WHERE extract(isodow FROM d) BETWEEN 1 AND 5;

CREATE TEMP TABLE jeu_dates ON COMMIT DROP AS
SELECT CASE WHEN extract(year FROM l + 7) = extract(year FROM l) THEN l ELSE l + 14 END AS lundi_conges,
       (SELECT jour FROM jeu_jours WHERE rang = 1) AS j1,
       (SELECT jour FROM jeu_jours WHERE rang = 2) AS j2,
       (SELECT jour FROM jeu_jours WHERE rang = 3) AS j3
  FROM (SELECT current_date + 14 + ((8 - extract(isodow FROM current_date + 14)::int) % 7) AS l) AS calcul;

-- ============================================================================
-- 3. COMPTES DE TEST (mot de passe : password123)
-- ============================================================================
INSERT INTO utilisateur (email, mot_de_passe, role, service_id, actif, date_creation) VALUES
    ('candidat.recu@test.sirh.local',        :'mot_de_passe', 'CANDIDAT', NULL, true, now() - interval '20 days'),
    ('candidat.entretien@test.sirh.local',   :'mot_de_passe', 'CANDIDAT', NULL, true, now() - interval '30 days'),
    ('candidat.retenu@test.sirh.local',      :'mot_de_passe', 'CANDIDAT', NULL, true, now() - interval '40 days'),
    ('candidat.proposition@test.sirh.local', :'mot_de_passe', 'CANDIDAT', NULL, true, now() - interval '45 days');

-- Deux salariés : l'un en CDD (Service Développement), l'autre en CDI (Service Transport).
INSERT INTO utilisateur (email, mot_de_passe, role, service_id, actif, date_creation)
SELECT v.email, :'mot_de_passe', 'EMPLOYE', s.id, true, now() - v.anciennete
  FROM (VALUES ('salarie.cdd@test.sirh.local', 'Service Développement', interval '120 days'),
               ('salarie.cdi@test.sirh.local', 'Service Transport',     interval '400 days')) AS v(email, service, anciennete)
  JOIN service s ON s.nom = v.service;

-- Profils des candidats (repris dans le dossier salarié lors de l'embauche).
INSERT INTO candidat (utilisateur_id, nom, prenom, telephone, date_naissance, lieu_naissance, cin, adresse)
SELECT u.id, v.nom, v.prenom, v.telephone, v.naissance::date, v.lieu, v.cin, v.adresse
  FROM (VALUES
    ('candidat.recu@test.sirh.local',        'Rasoa',    'Andry', '0341100001', '1998-04-12', 'Antananarivo', '101231000001', 'Lot II A 12 Analakely, Antananarivo'),
    ('candidat.entretien@test.sirh.local',   'Randria',  'Fara',  '0341100002', '1996-09-30', 'Fianarantsoa', '201011000002', 'Lot 45 Ambalavao, Fianarantsoa'),
    ('candidat.retenu@test.sirh.local',      'Rakotobe', 'Mamy',  '0341100003', '1994-01-18', 'Mahajanga',    '401011000003', 'Lot 7 Mahabibo, Mahajanga'),
    ('candidat.proposition@test.sirh.local', 'Ratsima',  'Lova',  '0341100004', '1997-07-07', 'Antsirabe',    '102011000004', 'Lot 3 Mahazina, Antsirabe')
  ) AS v(email, nom, prenom, telephone, naissance, lieu, cin, adresse)
  JOIN utilisateur u ON u.email = v.email;

-- ============================================================================
-- 4. BLOC 1 — EXPRESSION DU BESOIN (budget global = enveloppe salariale annuelle)
-- ============================================================================
-- Besoin A : validé par tout le circuit, offre publiée (donc exécuté). Budget 30 000 000 Ar/an
--            pour 2 postes à 1 200 000 Ar/mois (coût annuel prévu : 28 800 000 Ar).
INSERT INTO demande (type, auteur_id, poste_cible_id, description, statut, date_creation, date_limite_execution, date_execution,
                     effectif_demande, salaire_mensuel_par_poste, budget_global_recrutement, date_souhaitee, motif_besoin)
SELECT 'RECRUTEMENT', a.id, p.id, '[Test] Renfort de l’équipe Java : deux développeurs backend', 'EXECUTEE',
       now() - interval '25 days', now() - interval '14 days', now() - interval '15 days',
       2, 1200000.00, 30000000.00, current_date + 30, 'CREATION'
  FROM utilisateur a, poste p
 WHERE a.email = 'chef-service@sirh.local' AND p.intitule = 'Développeur Backend Java';

-- Besoin B : déposé par un employé, à valider par le chef de service (première étape).
INSERT INTO demande (type, auteur_id, poste_cible_id, description, statut, date_creation,
                     effectif_demande, salaire_mensuel_par_poste, budget_global_recrutement, date_souhaitee, motif_besoin)
SELECT 'RECRUTEMENT', a.id, p.id, '[Test] Remplacement d’un chauffeur parti à la retraite', 'SOUMISE',
       now() - interval '2 days', 1, 700000.00, 9000000.00, current_date + 45, 'REMPLACEMENT'
  FROM utilisateur a, poste p
 WHERE a.email = 'employe@sirh.local' AND p.intitule = 'Chauffeur';

-- Besoin C : validé par la DG, prêt à être transformé en offre par les RH.
INSERT INTO demande (type, auteur_id, poste_cible_id, description, statut, date_creation, date_limite_execution,
                     effectif_demande, salaire_mensuel_par_poste, budget_global_recrutement, date_souhaitee, motif_besoin)
SELECT 'RECRUTEMENT', a.id, p.id, '[Test] Renfort transport : un chauffeur', 'VALIDEE_DG',
       now() - interval '10 days', now() + interval '3 days',
       1, 800000.00, 12000000.00, current_date + 30, 'CREATION'
  FROM utilisateur a, poste p
 WHERE a.email = 'chef-service@sirh.local' AND p.intitule = 'Chauffeur';

-- Historique du circuit de validation (chef de service → direction → RH → DG) pour A et C.
INSERT INTO validation_demande (demande_id, etape, valideur_id, decision, commentaire, date_validation)
SELECT d.id, v.etape, u.id, 'VALIDE', v.commentaire, d.date_creation + v.delai
  FROM demande d
 CROSS JOIN (VALUES ('CHEF_SERVICE', 'chef-service@sirh.local', 'Besoin confirmé pour le service.', interval '1 day'),
                    ('DIRECTION',    'direction@sirh.local',    'Accord de la direction.',          interval '2 days'),
                    ('RH',           'rh@sirh.local',           'Poste et budget vérifiés.',        interval '3 days'),
                    ('DG',           'dg@sirh.local',           'Validé.',                          interval '4 days')
            ) AS v(etape, valideur, commentaire, delai)
  JOIN utilisateur u ON u.email = v.valideur
 WHERE d.description IN ('[Test] Renfort de l’équipe Java : deux développeurs backend',
                         '[Test] Renfort transport : un chauffeur');

-- ============================================================================
-- 5. BLOC 2 — RECRUTEMENT (offres, candidatures à chaque étape, entretiens)
-- ============================================================================
-- Offre issue du besoin A : 2 postes, publiée, avec un test de niveau 1.
INSERT INTO offre_emploi (poste_id, demande_origine_id, titre, description, nb_postes, experience_min_mois,
                          seuil_acceptation, date_publication, date_limite, statut, entreprise_id)
SELECT p.id, d.id, '[Test] Développeur Java — renfort',
       'Développement de services backend Java et Spring au sein de l’équipe Développement.',
       2, 12, 60, current_date - 15, current_date + 30, 'PUBLIEE', e.id
  FROM poste p, demande d, entreprise e
 WHERE p.intitule = 'Développeur Backend Java'
   AND d.description = '[Test] Renfort de l’équipe Java : deux développeurs backend'
   AND e.nom = 'Entreprise Alpha - TEST';

-- Offre dont la date limite est dépassée : clôturée automatiquement au prochain démarrage.
INSERT INTO offre_emploi (poste_id, titre, description, nb_postes, seuil_acceptation,
                          date_publication, date_limite, statut, entreprise_id)
SELECT p.id, '[Test] Chauffeur — offre expirée', 'Offre de test dont la date limite est passée.',
       1, 60, current_date - 40, current_date - 1, 'PUBLIEE', e.id
  FROM poste p, entreprise e
 WHERE p.intitule = 'Chauffeur' AND e.nom = 'Entreprise Beta - TEST';

INSERT INTO test (offre_id, niveau, titre)
SELECT id, 1, '[Test] QCM Java — niveau 1' FROM offre_emploi WHERE titre = '[Test] Développeur Java — renfort';

-- Une candidature par étape : reçue, en entretien, retenue, proposition envoyée.
INSERT INTO candidature (offre_id, candidat_id, score_matching, statut, date_candidature)
SELECT o.id, c.id, v.score, v.statut, now() - v.anciennete
  FROM (VALUES ('candidat.recu@test.sirh.local',        72.50, 'RECUE',        interval '3 days'),
               ('candidat.entretien@test.sirh.local',   81.00, 'EN_ENTRETIEN', interval '12 days'),
               ('candidat.retenu@test.sirh.local',      88.00, 'RETENUE',      interval '20 days'),
               ('candidat.proposition@test.sirh.local', 90.00, 'RETENUE',      interval '25 days')
       ) AS v(email, score, statut, anciennete)
  JOIN utilisateur u ON u.email = v.email
  JOIN candidat c ON c.utilisateur_id = u.id
  JOIN offre_emploi o ON o.titre = '[Test] Développeur Java — renfort';

-- Candidat en entretien : entretien n° 1 réalisé (suite : entretien suivant),
-- entretien n° 2 passé hier, en attente de la décision RH.
INSERT INTO entretien (candidature_id, date_heure, lieu, statut, decision_rh, commentaire)
SELECT ca.id, v.quand, v.lieu, v.statut, v.decision, v.commentaire
  FROM candidature ca
  JOIN candidat c ON c.id = ca.candidat_id
  JOIN utilisateur u ON u.id = c.utilisateur_id
 CROSS JOIN (VALUES (current_date - 5 + time '10:00', 'Salle A — RH',             'REALISE',  'ACCEPTE',    'Bon échange ; entretien technique à prévoir.'),
                    (current_date - 1 + time '14:00', 'Visio — équipe technique', 'PLANIFIE', 'EN_ATTENTE', NULL)
            ) AS v(quand, lieu, statut, decision, commentaire)
 WHERE u.email = 'candidat.entretien@test.sirh.local';

-- ============================================================================
-- 6. BLOC 3 — PROPOSITION D'EMBAUCHE EN ATTENTE DE RÉPONSE
--    Son acceptation par le candidat crée automatiquement le salarié.
--    Budget du besoin A : 30 000 000 − 12 × 1 200 000 = 15 600 000 Ar/an restants.
-- ============================================================================
UPDATE candidature ca
   SET proposition_statut      = 'EN_ATTENTE',
       proposition_type_contrat = 'CDD',
       proposition_salaire     = 1200000.00,
       proposition_debut       = current_date + 7,
       proposition_fin         = (current_date + 7 + interval '6 months')::date - 1,
       proposition_envoyee_le  = now() - interval '1 day',
       proposition_role        = 'EMPLOYE',
       proposition_service_id  = (SELECT id FROM service WHERE nom = 'Service Développement'),
       proposition_envoyee_par = 'rh@sirh.local'
  FROM candidat c
  JOIN utilisateur u ON u.id = c.utilisateur_id
 WHERE ca.candidat_id = c.id AND u.email = 'candidat.proposition@test.sirh.local';

INSERT INTO notification (utilisateur_id, message, type, lien, lu, date_creation)
SELECT u.id, 'Une proposition d’embauche vous attend.', 'EMBAUCHE', '/candidat/propositions/' || ca.id, false, now() - interval '1 day'
  FROM candidature ca
  JOIN candidat c ON c.id = ca.candidat_id
  JOIN utilisateur u ON u.id = c.utilisateur_id
 WHERE u.email = 'candidat.proposition@test.sirh.local';

-- ============================================================================
-- 7. BLOCS 4 ET 5 — SALARIÉS : DOSSIER, AFFECTATION, CONTRATS, RÉMUNÉRATION, INTÉGRATION
--    Le matricule (EMP-AAAA-0001) est attribué par la base à l'insertion du dossier.
-- ============================================================================
INSERT INTO dossier_employe (utilisateur_id, nom, prenom, date_entree, sexe, date_naissance, lieu_naissance,
                             nationalite, cin, adresse, telephone, situation_familiale, nombre_enfants)
SELECT u.id, v.nom, v.prenom, current_date - v.anciennete, v.sexe, v.naissance::date, v.lieu,
       'Malagasy', v.cin, v.adresse, v.telephone, v.situation, v.enfants
  FROM (VALUES ('salarie.cdd@test.sirh.local', 'Rakoto', 'Hery',     120, 'M', '1995-06-21', 'Antsirabe', '101211000101',
                'Lot IVG 45 Behoririka, Antananarivo', '0341200101', 'CELIBATAIRE', 0),
               ('salarie.cdi@test.sirh.local', 'Rabe',   'Voahangy', 400, 'F', '1990-02-03', 'Toamasina', '501011000202',
                'Lot 12 Ambohijatovo, Antananarivo',   '0331200202', 'MARIE',       2)
       ) AS v(email, nom, prenom, anciennete, sexe, naissance, lieu, cin, adresse, telephone, situation, enfants)
  JOIN utilisateur u ON u.email = v.email;

INSERT INTO affectation_employe (dossier_id, poste_id, service_id, poste_libelle, service_libelle, date_debut, actuelle, origine)
SELECT d.id, p.id, s.id, p.intitule, s.nom, d.date_entree, true, 'EMBAUCHE'
  FROM (VALUES ('salarie.cdd@test.sirh.local', 'Développeur Backend Java', 'Service Développement'),
               ('salarie.cdi@test.sirh.local', 'Chauffeur',                'Service Transport')) AS v(email, poste, service)
  JOIN utilisateur u ON u.email = v.email
  JOIN dossier_employe d ON d.utilisateur_id = u.id
  JOIN poste p ON p.intitule = v.poste
  JOIN service s ON s.nom = v.service;

-- Salarié CDD : contrat en cours qui se termine dans 20 jours (alerte « Contrats à échéance »).
INSERT INTO contrat_employe (dossier_id, type, date_debut, date_fin_prevue, statut, salaire_brut,
                             date_signature, cree_le, cree_par, version)
SELECT d.id, 'CDD', d.date_entree, current_date + 20, 'EN_COURS', 1200000.00,
       d.date_entree, d.date_entree + time '09:00', 'rh@sirh.local', 0
  FROM dossier_employe d JOIN utilisateur u ON u.id = d.utilisateur_id
 WHERE u.email = 'salarie.cdd@test.sirh.local';

-- Salarié CDI : CDD initial de 6 mois, terminé, puis CDI qui le référence (renouvellement).
INSERT INTO contrat_employe (dossier_id, type, date_debut, date_fin_prevue, date_fin_effective, statut, salaire_brut,
                             motif_fin, date_signature, cree_le, cree_par, version)
SELECT d.id, 'CDD', d.date_entree, d.date_entree + 179, d.date_entree + 179, 'TERMINE', 900000.00,
       'Remplacé par un contrat CDI à partir du ' || to_char(d.date_entree + 180, 'DD/MM/YYYY'),
       d.date_entree, d.date_entree + time '09:00', 'rh@sirh.local', 0
  FROM dossier_employe d JOIN utilisateur u ON u.id = d.utilisateur_id
 WHERE u.email = 'salarie.cdi@test.sirh.local';

INSERT INTO contrat_employe (dossier_id, type, date_debut, statut, salaire_brut, date_signature,
                             cree_le, cree_par, version, contrat_precedent_id)
SELECT d.id, 'CDI', d.date_entree + 180, 'EN_COURS', 1000000.00, d.date_entree + 175,
       d.date_entree + 175 + time '10:00', 'rh@sirh.local', 0,
       (SELECT c.id FROM contrat_employe c WHERE c.dossier_id = d.id AND c.type = 'CDD')
  FROM dossier_employe d JOIN utilisateur u ON u.id = d.utilisateur_id
 WHERE u.email = 'salarie.cdi@test.sirh.local';

-- Historique de rémunération : salaire de chaque contrat, puis une augmentation du salarié CDI.
INSERT INTO remuneration_employe (dossier_id, contrat_id, date_effet, salaire_brut, motif, cree_le, cree_par)
SELECT c.dossier_id, c.id, c.date_debut, c.salaire_brut, 'Salaire du contrat ' || c.type, c.cree_le, c.cree_par
  FROM contrat_employe c
  JOIN dossier_employe d ON d.id = c.dossier_id
  JOIN utilisateur u ON u.id = d.utilisateur_id
 WHERE u.email IN ('salarie.cdd@test.sirh.local', 'salarie.cdi@test.sirh.local');

INSERT INTO remuneration_employe (dossier_id, contrat_id, date_effet, salaire_brut, motif, cree_le, cree_par)
SELECT c.dossier_id, c.id, current_date - 30, 1050000.00, 'Augmentation annuelle', now() - interval '35 days', 'rh@sirh.local'
  FROM contrat_employe c
  JOIN dossier_employe d ON d.id = c.dossier_id
  JOIN utilisateur u ON u.id = d.utilisateur_id
 WHERE u.email = 'salarie.cdi@test.sirh.local' AND c.type = 'CDI';

-- Intégration : 3 tâches sur 7 pour le salarié CDD (le chef de service peut cocher matériel et
-- formation), intégration terminée pour le salarié CDI.
INSERT INTO onboarding (dossier_id, statut, date_debut, termine_le)
SELECT d.id, v.statut, d.date_entree,
       CASE WHEN v.statut = 'TERMINE' THEN d.date_entree + 20 + time '16:00' END
  FROM (VALUES ('salarie.cdd@test.sirh.local', 'EN_COURS'),
               ('salarie.cdi@test.sirh.local', 'TERMINE')) AS v(email, statut)
  JOIN utilisateur u ON u.email = v.email
  JOIN dossier_employe d ON d.utilisateur_id = u.id;

INSERT INTO onboarding_tache (onboarding_id, code, statut, fait_le, fait_par)
SELECT o.id, t.code,
       CASE WHEN o.statut = 'TERMINE' OR t.code IN ('CONTRAT_SIGNE', 'COMPTE', 'BADGE') THEN 'FAIT' ELSE 'A_FAIRE' END,
       CASE WHEN o.statut = 'TERMINE' OR t.code IN ('CONTRAT_SIGNE', 'COMPTE', 'BADGE')
            THEN d.date_entree + t.jour + time '10:00' END,
       CASE WHEN o.statut = 'TERMINE' OR t.code IN ('CONTRAT_SIGNE', 'COMPTE', 'BADGE') THEN t.par END
  FROM onboarding o
  JOIN dossier_employe d ON d.id = o.dossier_id
  JOIN utilisateur u ON u.id = d.utilisateur_id
 CROSS JOIN (VALUES ('CONTRAT_SIGNE',    'Automatique',             0),
                    ('BADGE',            'rh@sirh.local',           1),
                    ('COMPTE',           'Automatique',             0),
                    ('MATERIEL',         'chef-service@sirh.local', 2),
                    ('FORMATION',        'chef-service@sirh.local', 5),
                    ('INSCRIPTION_PAIE', 'rh@sirh.local',           3),
                    ('COMPTE_BANCAIRE',  'rh@sirh.local',          10)) AS t(code, par, jour)
 WHERE u.email IN ('salarie.cdd@test.sirh.local', 'salarie.cdi@test.sirh.local');

-- ============================================================================
-- 8. BLOC 6 — PLANNING ET POINTAGES (30 derniers jours ouvrés)
-- ============================================================================
-- Salarié CDI : horaire 08:00–17:00, pause 60 min, du lundi au vendredi ; repos exceptionnel au 4e jour ouvré passé.
INSERT INTO regime_travail (dossier_id, depuis, debut, fin, pause_minutes, jours)
SELECT d.id, d.date_entree, time '08:00', time '17:00', 60, '1,2,3,4,5'
  FROM dossier_employe d JOIN utilisateur u ON u.id = d.utilisateur_id
 WHERE u.email = 'salarie.cdi@test.sirh.local';

INSERT INTO jour_planning (dossier_id, jour, repos, debut, fin, pause_minutes)
SELECT d.id, j.jour, true, NULL, NULL, 0
  FROM dossier_employe d JOIN utilisateur u ON u.id = d.utilisateur_id
  JOIN jeu_jours j ON j.rang = 4
 WHERE u.email = 'salarie.cdi@test.sirh.local';

-- Salarié CDD (horaire par défaut 09:00–18:00, pause 60 min, soit 480 min prévues) :
--   rang 1   : entrée sans sortie → « Sortie manquante » ; une sortie aujourd'hui est refusée,
--              une nouvelle entrée est bloquée : correction RH attendue.
--   rangs 2-3 : congé annuel accepté (voir section 9).
--   rang 4   : ni pointage ni absence → « Absence à vérifier ».
--   rang 5   : dépassement corrigé par les RH, 90 min supplémentaires validées.
--   rang 6   : arrivée en retard de 40 min → « Temps incomplet ».
--   rang 7+  : journées normales.
INSERT INTO pointage_jour (dossier_id, jour, entree, sortie, pause_minutes, debut_prevu, minutes_prevues,
                           supplementaires_validees, modifie_par, motif_correction)
SELECT d.id, j.jour,
       j.jour + CASE j.rang WHEN 1 THEN time '09:05' WHEN 5 THEN time '08:55' WHEN 6 THEN time '09:40' ELSE time '09:00' END,
       CASE j.rang WHEN 1 THEN NULL::timestamp
                   WHEN 5 THEN j.jour + time '19:30'
                   WHEN 6 THEN j.jour + time '18:10'
                   ELSE j.jour + time '18:00' END,
       CASE WHEN j.rang = 1 THEN 0 ELSE 60 END,
       time '09:00', 480,
       CASE WHEN j.rang = 5 THEN 90 ELSE 0 END,
       CASE WHEN j.rang = 5 THEN 'rh@sirh.local' END,
       CASE WHEN j.rang = 5 THEN 'Heures supplémentaires validées : clôture de sprint' END
  FROM dossier_employe d JOIN utilisateur u ON u.id = d.utilisateur_id
  JOIN jeu_jours j ON j.rang = 1 OR j.rang >= 5
 WHERE u.email = 'salarie.cdd@test.sirh.local';

-- Salarié CDI (480 min prévues) :
--   rang 2   : absence non rémunérée enregistrée par les RH (voir section 9).
--   rang 3   : 08:05–17:20, pause 45 min → 30 min de dépassement non validées.
--   rang 4   : jour de repos du planning.
--   autres   : journées normales.
INSERT INTO pointage_jour (dossier_id, jour, entree, sortie, pause_minutes, debut_prevu, minutes_prevues)
SELECT d.id, j.jour,
       j.jour + CASE j.rang WHEN 1 THEN time '07:58' WHEN 3 THEN time '08:05' ELSE time '08:00' END,
       j.jour + CASE j.rang WHEN 1 THEN time '17:02' WHEN 3 THEN time '17:20' ELSE time '17:00' END,
       CASE WHEN j.rang = 3 THEN 45 ELSE 60 END,
       time '08:00', 480
  FROM dossier_employe d JOIN utilisateur u ON u.id = d.utilisateur_id
  JOIN jeu_jours j ON j.rang = 1 OR j.rang = 3 OR j.rang >= 5
 WHERE u.email = 'salarie.cdi@test.sirh.local';

-- ============================================================================
-- 9. BLOC 7 — DROITS, DEMANDES DE CONGÉ ET ABSENCES
-- ============================================================================
-- Droits annuels au congé annuel (24 et 30 jours), pour chaque année concernée par les dates du jeu.
INSERT INTO droit_conge (dossier_id, type_id, annee, jours, motif, modifie_par)
SELECT d.id, t.id, a.annee, v.jours, 'Droits annuels ' || a.annee || ' (jeu de test)', 'rh@sirh.local'
  FROM (VALUES ('salarie.cdd@test.sirh.local', 24),
               ('salarie.cdi@test.sirh.local', 30)) AS v(email, jours)
  JOIN utilisateur u ON u.email = v.email
  JOIN dossier_employe d ON d.utilisateur_id = u.id
  JOIN type_conge t ON t.nom = 'Congé annuel'
 CROSS JOIN (SELECT DISTINCT extract(year FROM x)::int AS annee
               FROM (SELECT current_date AS x
                     UNION ALL SELECT jour FROM jeu_jours
                     UNION ALL SELECT lundi_conges + 7 FROM jeu_dates) AS dates) AS a;

-- Salarié CDD : congé accepté sur les 2e et 3e jours ouvrés passés (absence rémunérée dans le planning).
INSERT INTO demande_conge (dossier_id, type_id, debut, fin, jours, statut, motif, responsable_id, soumise_le,
                           decision_manager_le, decision_manager_par, decision_le, decision_par)
SELECT d.id, t.id, p.debut, p.fin,
       (SELECT count(*) FROM jeu_jours WHERE jour BETWEEN p.debut AND p.fin)::int,
       'ACCEPTE', 'Événement familial', s.chef_service_id,
       p.debut - 12 + time '10:00', p.debut - 11 + time '09:30', 'chef-service@sirh.local',
       p.debut - 10 + time '15:00', 'rh@sirh.local'
  FROM dossier_employe d
  JOIN utilisateur u ON u.id = d.utilisateur_id
  JOIN service s ON s.id = u.service_id
  JOIN type_conge t ON t.nom = 'Congé annuel'
 CROSS JOIN (SELECT CASE WHEN extract(year FROM j3) = extract(year FROM j2) THEN j3 ELSE j2 END AS debut,
                    j2 AS fin
               FROM jeu_dates) AS p
 WHERE u.email = 'salarie.cdd@test.sirh.local';

-- Salarié CDD : 3 jours (lundi à mercredi) en attente du chef de service.
INSERT INTO demande_conge (dossier_id, type_id, debut, fin, jours, statut, motif, responsable_id, soumise_le)
SELECT d.id, t.id, j.lundi_conges, j.lundi_conges + 2, 3, 'ATTENTE_MANAGER', 'Vacances en famille',
       s.chef_service_id, now() - interval '1 day'
  FROM dossier_employe d
  JOIN utilisateur u ON u.id = d.utilisateur_id
  JOIN service s ON s.id = u.service_id
  JOIN type_conge t ON t.nom = 'Congé annuel'
 CROSS JOIN jeu_dates j
 WHERE u.email = 'salarie.cdd@test.sirh.local';

-- Salarié CDI : 2 jours (jeudi et vendredi) validés par le chef, en attente de la décision RH.
INSERT INTO demande_conge (dossier_id, type_id, debut, fin, jours, statut, motif, responsable_id, soumise_le,
                           decision_manager_le, decision_manager_par)
SELECT d.id, t.id, j.lundi_conges + 3, j.lundi_conges + 4, 2, 'ATTENTE_RH', 'Démarches administratives',
       s.chef_service_id, now() - interval '2 days', now() - interval '3 hours', 'chef-service@sirh.local'
  FROM dossier_employe d
  JOIN utilisateur u ON u.id = d.utilisateur_id
  JOIN service s ON s.id = u.service_id
  JOIN type_conge t ON t.nom = 'Congé annuel'
 CROSS JOIN jeu_dates j
 WHERE u.email = 'salarie.cdi@test.sirh.local';

-- Salarié CDI : 1 jour refusé par les RH, avec le motif du refus.
INSERT INTO demande_conge (dossier_id, type_id, debut, fin, jours, statut, motif, responsable_id, soumise_le,
                           decision_manager_le, decision_manager_par, decision_le, decision_par, commentaire)
SELECT d.id, t.id, j.lundi_conges + 7, j.lundi_conges + 7, 1, 'REFUSE', 'Rendez-vous personnel',
       s.chef_service_id, now() - interval '4 days', now() - interval '3 days', 'chef-service@sirh.local',
       now() - interval '2 days', 'rh@sirh.local', 'Période de forte activité : merci de proposer une autre date.'
  FROM dossier_employe d
  JOIN utilisateur u ON u.id = d.utilisateur_id
  JOIN service s ON s.id = u.service_id
  JOIN type_conge t ON t.nom = 'Congé annuel'
 CROSS JOIN jeu_dates j
 WHERE u.email = 'salarie.cdi@test.sirh.local';

-- Absences : celle du congé accepté (rémunérée) et une absence non rémunérée saisie par les RH
-- (comptée en « Absence_non_payee_minutes » dans l'export CSV de la paie).
INSERT INTO absence_employe (dossier_id, demande_id, debut, fin, libelle, remuneree, active, enregistree_par)
SELECT c.dossier_id, c.id, c.debut, c.fin, t.nom, t.remunere, true, 'rh@sirh.local'
  FROM demande_conge c
  JOIN type_conge t ON t.id = c.type_id
  JOIN dossier_employe d ON d.id = c.dossier_id
  JOIN utilisateur u ON u.id = d.utilisateur_id
 WHERE c.statut = 'ACCEPTE' AND u.email = 'salarie.cdd@test.sirh.local';

INSERT INTO absence_employe (dossier_id, debut, fin, libelle, remuneree, active, enregistree_par)
SELECT d.id, j.j2, j.j2, 'Absence non justifiée', false, true, 'rh@sirh.local'
  FROM dossier_employe d JOIN utilisateur u ON u.id = d.utilisateur_id
 CROSS JOIN jeu_dates j
 WHERE u.email = 'salarie.cdi@test.sirh.local';

-- ============================================================================
-- 10. RÉCAPITULATIF : comptes à utiliser (mot de passe : password123)
-- ============================================================================
SELECT u.email AS "Compte", u.role AS "Rôle", coalesce(d.matricule, '') AS "Matricule", v.a_tester AS "À tester"
  FROM (VALUES
    (1,  'rh@sirh.local',                        'Besoins, offres, candidatures, propositions, dossiers salariés, pointages, congés'),
    (2,  'chef-service@sirh.local',              'Valider le besoin « Remplacement d’un chauffeur » et le congé du salarié CDD ; cocher matériel et formation'),
    (3,  'direction@sirh.local',                 'Étape Direction des besoins ; consultation professionnelle des dossiers'),
    (4,  'dg@sirh.local',                        'Validation finale des besoins ; consultation complète des dossiers'),
    (5,  'candidat.recu@test.sirh.local',        'Candidature reçue : présélection par les RH'),
    (6,  'candidat.entretien@test.sirh.local',   'Entretien n° 2 passé : décision RH (entretien suivant, tests ou refus)'),
    (7,  'candidat.retenu@test.sirh.local',      'Retenu : les RH envoient la proposition (budget restant 15 600 000 Ar/an)'),
    (8,  'candidat.proposition@test.sirh.local', 'Proposition en attente : l’accepter crée automatiquement le salarié'),
    (9,  'salarie.cdd@test.sirh.local',          'Sortie refusée le lendemain, congé en attente du chef, contrat à échéance, intégration 3/7'),
    (10, 'salarie.cdi@test.sirh.local',          'Congé en attente RH, congé refusé, absence non payée, CDD puis CDI, intégration terminée')
  ) AS v(ordre, email, a_tester)
  JOIN utilisateur u ON u.email = v.email
  LEFT JOIN dossier_employe d ON d.utilisateur_id = u.id
 ORDER BY v.ordre;

COMMIT;

\echo 'Jeu de test créé. Mot de passe de tous les comptes : password123.'
\echo 'L’offre « [Test] Chauffeur — offre expirée » sera clôturée au prochain démarrage de l’application.'
