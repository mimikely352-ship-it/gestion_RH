-- ============================================================================
-- SIRH — Gestion RH : données de référence (diplômes, organisation démo)
-- Les comptes utilisateurs de démonstration (avec mot de passe hashé) sont créés
-- au démarrage de l'application par com.sirh.config.DataInitializer, pas ici,
-- car le hash BCrypt doit être généré par l'encodeur de l'application.
-- ============================================================================

-- psql : ce fichier est en UTF-8, quel que soit l'encodage du terminal Windows.
SET client_encoding = 'UTF8';

-- ========== RÉFÉRENTIEL DIPLÔME ==========
INSERT INTO niveau_diplome (niveau) VALUES
  ('Baccalauréat'), ('Licence'), ('Master'), ('Doctorat'), ('BTS/DUT');

INSERT INTO diplome_filiere (filiere) VALUES
  ('Informatique'), ('Gestion'), ('Comptabilité'), ('Ressources Humaines'), ('Logistique'), ('Droit');

INSERT INTO diplome_option (diplome_filiere_id, option_nom) VALUES
  ((SELECT id FROM diplome_filiere WHERE filiere = 'Informatique'), 'Développement'),
  ((SELECT id FROM diplome_filiere WHERE filiere = 'Informatique'), 'Réseaux et systèmes'),
  ((SELECT id FROM diplome_filiere WHERE filiere = 'Gestion'), 'Gestion des entreprises'),
  ((SELECT id FROM diplome_filiere WHERE filiere = 'Comptabilité'), 'Comptabilité générale'),
  ((SELECT id FROM diplome_filiere WHERE filiere = 'Ressources Humaines'), 'Gestion RH'),
  ((SELECT id FROM diplome_filiere WHERE filiere = 'Logistique'), 'Transport et logistique'),
  ((SELECT id FROM diplome_filiere WHERE filiere = 'Droit'), 'Droit des affaires');

-- Diplôme = croisement niveau x option (ex : Licence en Informatique - Développement)
INSERT INTO diplome (niveau_id, diplome_option_id)
SELECT n.id, o.id
FROM niveau_diplome n
CROSS JOIN diplome_option o
WHERE (n.niveau, o.option_nom) IN (
  ('Licence', 'Développement'),
  ('Master', 'Développement'),
  ('BTS/DUT', 'Réseaux et systèmes'),
  ('Licence', 'Gestion des entreprises'),
  ('Master', 'Gestion RH'),
  ('Licence', 'Comptabilité générale'),
  ('BTS/DUT', 'Transport et logistique'),
  ('Master', 'Droit des affaires')
);

-- ========== ORGANISATION DE DÉMONSTRATION ==========
INSERT INTO direction (nom) VALUES ('Direction Générale');

INSERT INTO departement (nom, direction_id) VALUES
  ('Département Informatique', (SELECT id FROM direction WHERE nom = 'Direction Générale')),
  ('Département Logistique', (SELECT id FROM direction WHERE nom = 'Direction Générale'));

INSERT INTO service (nom, departement_id) VALUES
  ('Service Développement', (SELECT id FROM departement WHERE nom = 'Département Informatique')),
  ('Service Transport', (SELECT id FROM departement WHERE nom = 'Département Logistique'));

INSERT INTO section (nom, service_id) VALUES
  ('Section Backend', (SELECT id FROM service WHERE nom = 'Service Développement')),
  ('Section Chauffeurs', (SELECT id FROM service WHERE nom = 'Service Transport'));

INSERT INTO poste (intitule, description, section_id) VALUES
  ('Développeur Backend Java', 'Conception et développement des services métier.', (SELECT id FROM section WHERE nom = 'Section Backend')),
  ('Chauffeur', 'Transport et livraison de marchandises.', (SELECT id FROM section WHERE nom = 'Section Chauffeurs'));
