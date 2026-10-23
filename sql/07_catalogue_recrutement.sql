-- Catalogue de depart, modifiable selon les besoins de l'entreprise.
-- Apres 06 sur une base existante. Relancable sans supprimer les donnees.
\set ON_ERROR_STOP on
SET client_encoding = 'UTF8';
BEGIN;
CREATE TEMP TABLE catalogue_metier (filiere text, specialite text, niveaux text[]) ON COMMIT DROP;
INSERT INTO catalogue_metier VALUES
 ('Informatique', 'Développement', ARRAY['BTS/DUT','Licence','Master','Doctorat']),
 ('Informatique', 'Réseaux et systèmes', ARRAY['BTS/DUT','Licence','Master']),
 ('Informatique', 'Cybersécurité et données', ARRAY['Licence','Master','Doctorat']),
 ('Comptabilité', 'Comptabilité générale', ARRAY['Baccalauréat','BTS/DUT','Licence','Master']),
 ('Finance', 'Banque et analyse financière', ARRAY['BTS/DUT','Licence','Master','Doctorat']),
 ('Commerce', 'Vente et négociation', ARRAY['CAP','BEP','Baccalauréat','BTS/DUT','Licence','Master']),
 ('Marketing', 'Communication et marketing numérique', ARRAY['BTS/DUT','Licence','Master']),
 ('Ressources Humaines', 'Gestion RH', ARRAY['BTS/DUT','Licence','Master']),
 ('Gestion', 'Gestion des entreprises', ARRAY['Baccalauréat','BTS/DUT','Licence','Master']),
 ('Administration', 'Secrétariat et assistanat', ARRAY['BEP','Baccalauréat','BTS/DUT','Licence']),
 ('Droit', 'Droit des affaires', ARRAY['Licence','Master','Doctorat']),
 ('Logistique', 'Transport et logistique', ARRAY['CAP','Baccalauréat','BTS/DUT','Licence','Master']),
 ('Transport', 'Conduite professionnelle', ARRAY['Certificat professionnel','CAP']),
 ('Bâtiment', 'Maçonnerie et construction', ARRAY['CAP','BEP','Baccalauréat','BTS/DUT']),
 ('Génie civil', 'Études et conduite de travaux', ARRAY['BTS/DUT','Licence','Master','Diplôme d’ingénieur']),
 ('Électricité', 'Installation et maintenance électrique', ARRAY['CAP','BEP','Baccalauréat','BTS/DUT','Licence']),
 ('Mécanique', 'Maintenance automobile et industrielle', ARRAY['CAP','BEP','Baccalauréat','BTS/DUT','Licence']),
 ('Industrie', 'Production et qualité', ARRAY['Baccalauréat','BTS/DUT','Licence','Master','Diplôme d’ingénieur']),
 ('Agriculture', 'Production agricole et élevage', ARRAY['CAP','Baccalauréat','BTS/DUT','Licence','Master']),
 ('Agroalimentaire', 'Transformation et sécurité alimentaire', ARRAY['BTS/DUT','Licence','Master']),
 ('Hôtellerie et restauration', 'Cuisine et service', ARRAY['CAP','BEP','Baccalauréat','BTS/DUT','Licence']),
 ('Tourisme', 'Accueil et gestion touristique', ARRAY['Baccalauréat','BTS/DUT','Licence','Master']),
 ('Santé', 'Soins infirmiers', ARRAY['Licence','Master']),
 ('Médecine', 'Médecine générale', ARRAY['Doctorat']),
 ('Éducation', 'Enseignement et pédagogie', ARRAY['Licence','Master','Doctorat']),
 ('Design', 'Graphisme et création visuelle', ARRAY['Certificat professionnel','BTS/DUT','Licence','Master']),
 ('Sécurité', 'Prévention et sécurité', ARRAY['Certificat professionnel','CAP','Baccalauréat']),
 ('Services', 'Entretien et services à la personne', ARRAY['Certificat professionnel','CAP','BEP']);

INSERT INTO niveau_diplome(niveau)
SELECT DISTINCT unnest(niveaux) FROM catalogue_metier ON CONFLICT (niveau) DO NOTHING;
INSERT INTO diplome_filiere(filiere)
SELECT DISTINCT filiere FROM catalogue_metier ON CONFLICT (filiere) DO NOTHING;
INSERT INTO diplome_option(diplome_filiere_id, option_nom)
SELECT f.id, c.specialite FROM catalogue_metier c
JOIN diplome_filiere f ON f.filiere = c.filiere
WHERE NOT EXISTS (SELECT 1 FROM diplome_option o WHERE o.diplome_filiere_id = f.id AND o.option_nom = c.specialite);
INSERT INTO diplome(niveau_id, diplome_option_id)
SELECT n.id, o.id FROM catalogue_metier c
JOIN diplome_filiere f ON f.filiere = c.filiere
JOIN diplome_option o ON o.diplome_filiere_id = f.id AND o.option_nom = c.specialite
JOIN niveau_diplome n ON n.niveau = ANY(c.niveaux)
ON CONFLICT (niveau_id, diplome_option_id) DO NOTHING;

INSERT INTO poste(intitule, categorie, description)
SELECT c.intitule, c.categorie, c.description FROM (VALUES
 ('Développeur web', 'Informatique', 'Concevoir et maintenir les applications web.'),
 ('Administrateur systèmes et réseaux', 'Informatique', 'Administrer les systèmes, les réseaux et les accès.'),
 ('Analyste de données', 'Informatique', 'Analyser les données et produire des indicateurs.'),
 ('Technicien support informatique', 'Informatique', 'Accompagner les utilisateurs et résoudre les incidents.'),
 ('Comptable', 'Finance et comptabilité', 'Assurer la tenue des comptes et les rapprochements.'),
 ('Analyste financier', 'Finance et comptabilité', 'Analyser les résultats et préparer les prévisions.'),
 ('Commercial terrain', 'Commerce', 'Prospecter, conseiller et développer le portefeuille clients.'),
 ('Conseiller de vente', 'Commerce', 'Accueillir les clients et les accompagner dans leurs achats.'),
 ('Responsable marketing', 'Marketing', 'Préparer et suivre les campagnes marketing.'),
 ('Chargé de communication', 'Communication', 'Créer les contenus et animer les supports de communication.'),
 ('Gestionnaire RH', 'Ressources humaines', 'Gérer les dossiers du personnel et accompagner les recrutements.'),
 ('Assistant administratif', 'Administration', 'Organiser les dossiers et assurer le suivi administratif.'),
 ('Réceptionniste', 'Accueil', 'Accueillir les visiteurs et gérer les demandes.'),
 ('Juriste', 'Droit', 'Préparer les contrats et conseiller les équipes.'),
 ('Magasinier', 'Logistique', 'Réceptionner, stocker et préparer les marchandises.'),
 ('Responsable logistique', 'Logistique', 'Coordonner les stocks et les opérations de transport.'),
 ('Chauffeur-livreur', 'Transport', 'Assurer les livraisons et le suivi des documents de transport.'),
 ('Maçon', 'Bâtiment', 'Réaliser les travaux de construction selon les plans.'),
 ('Conducteur de travaux', 'Bâtiment', 'Organiser et suivre les travaux et les équipes sur chantier.'),
 ('Électricien', 'Maintenance', 'Installer, vérifier et entretenir les équipements électriques.'),
 ('Mécanicien', 'Maintenance', 'Diagnostiquer et réparer les équipements mécaniques.'),
 ('Opérateur de production', 'Industrie', 'Participer à la production et contrôler la qualité.'),
 ('Technicien agricole', 'Agriculture', 'Suivre les cultures et accompagner les opérations agricoles.'),
 ('Cuisinier', 'Restauration', 'Préparer les plats et respecter les règles d’hygiène.'),
 ('Serveur', 'Restauration', 'Accueillir les clients et assurer le service.'),
 ('Agent de voyage', 'Tourisme', 'Conseiller les clients et organiser leurs déplacements.'),
 ('Infirmier', 'Santé', 'Assurer les soins relevant des qualifications requises.'),
 ('Enseignant', 'Éducation', 'Préparer les cours et accompagner les apprentissages.'),
 ('Designer graphique', 'Création', 'Concevoir les supports graphiques et les identités visuelles.'),
 ('Agent de sécurité', 'Sécurité', 'Assurer la surveillance et la prévention des incidents.'),
 ('Agent d’entretien', 'Services', 'Assurer la propreté et l’entretien des locaux.')
) AS c(intitule, categorie, description)
WHERE NOT EXISTS (SELECT 1 FROM poste p WHERE lower(p.intitule) = lower(c.intitule));
COMMIT;
