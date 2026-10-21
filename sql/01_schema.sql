-- ============================================================================
-- SIRH — Gestion RH : schéma PostgreSQL (V1)
-- ============================================================================

-- ========== ORGANISATION ==========
CREATE TABLE direction (
  id BIGSERIAL PRIMARY KEY,
  nom VARCHAR(100) NOT NULL UNIQUE
);

CREATE TABLE departement (
  id BIGSERIAL PRIMARY KEY,
  nom VARCHAR(100) NOT NULL,
  direction_id BIGINT NOT NULL REFERENCES direction(id)
);

CREATE TABLE service (
  id BIGSERIAL PRIMARY KEY,
  nom VARCHAR(100) NOT NULL,
  departement_id BIGINT NOT NULL REFERENCES departement(id),
  chef_service_id BIGINT -- FK vers utilisateur, ajoutée après la création de la table utilisateur
);

CREATE TABLE section (
  id BIGSERIAL PRIMARY KEY,
  nom VARCHAR(100) NOT NULL,
  service_id BIGINT NOT NULL REFERENCES service(id)
);

CREATE TABLE poste (
  id BIGSERIAL PRIMARY KEY,
  intitule VARCHAR(150) NOT NULL,
  description TEXT,
  section_id BIGINT NOT NULL REFERENCES section(id)
);

-- ========== RÉFÉRENTIEL DIPLÔME ==========
CREATE TABLE niveau_diplome (
  id BIGSERIAL PRIMARY KEY,
  niveau VARCHAR(100) NOT NULL UNIQUE          -- ex: Bac, Licence, Master, Doctorat
);

CREATE TABLE diplome_filiere (
  id BIGSERIAL PRIMARY KEY,
  filiere VARCHAR(150) NOT NULL UNIQUE         -- ex: Informatique
);

CREATE TABLE diplome_option (
  id BIGSERIAL PRIMARY KEY,
  diplome_filiere_id BIGINT NOT NULL REFERENCES diplome_filiere(id),
  option_nom VARCHAR(150) NOT NULL             -- ex: Développement
);

CREATE TABLE diplome (
  id BIGSERIAL PRIMARY KEY,
  niveau_id BIGINT NOT NULL REFERENCES niveau_diplome(id),
  diplome_option_id BIGINT NOT NULL REFERENCES diplome_option(id),
  UNIQUE (niveau_id, diplome_option_id)
);

-- ========== UTILISATEURS ==========
CREATE TABLE utilisateur (
  id BIGSERIAL PRIMARY KEY,
  email VARCHAR(150) NOT NULL UNIQUE,
  mot_de_passe VARCHAR(255) NOT NULL,          -- hashé (BCrypt)
  role VARCHAR(20) NOT NULL
    CHECK (role IN ('CANDIDAT','EMPLOYE','CHEF_SERVICE','DIRECTION','RH','DG')),
  service_id BIGINT REFERENCES service(id),    -- service de rattachement (employé, chef de service, direction...)
  actif BOOLEAN NOT NULL DEFAULT TRUE,
  date_creation TIMESTAMP NOT NULL DEFAULT NOW()
);

ALTER TABLE service ADD CONSTRAINT fk_service_chef
  FOREIGN KEY (chef_service_id) REFERENCES utilisateur(id);

-- ========== CANDIDAT ==========
CREATE TABLE candidat (
  id BIGSERIAL PRIMARY KEY,
  utilisateur_id BIGINT NOT NULL UNIQUE REFERENCES utilisateur(id),
  nom VARCHAR(100) NOT NULL,
  prenom VARCHAR(100) NOT NULL,
  telephone VARCHAR(20),
  date_naissance DATE,
  lieu_naissance VARCHAR(150),
  cin VARCHAR(30),
  adresse VARCHAR(255)
);

CREATE TABLE experience (
  id BIGSERIAL PRIMARY KEY,
  candidat_id BIGINT NOT NULL REFERENCES candidat(id),
  intitule VARCHAR(150) NOT NULL,
  entreprise VARCHAR(150),
  date_debut DATE NOT NULL,
  date_fin DATE,                               -- NULL = poste actuel
  description TEXT
);

CREATE TABLE candidat_diplome (
  candidat_id BIGINT NOT NULL REFERENCES candidat(id),
  diplome_id BIGINT NOT NULL REFERENCES diplome(id),
  PRIMARY KEY (candidat_id, diplome_id)
);

CREATE TABLE document (
  id BIGSERIAL PRIMARY KEY,
  type VARCHAR(30) NOT NULL,                   -- CV, AUTRE...
  nom_fichier VARCHAR(255) NOT NULL,
  chemin VARCHAR(255) NOT NULL,
  mime VARCHAR(100),
  taille BIGINT,
  candidat_id BIGINT REFERENCES candidat(id),
  date_upload TIMESTAMP NOT NULL DEFAULT NOW()
);

-- ========== RECRUTEMENT ==========
CREATE TABLE offre_emploi (
  id BIGSERIAL PRIMARY KEY,
  poste_id BIGINT NOT NULL REFERENCES poste(id),
  demande_origine_id BIGINT,                   -- FK vers demande, ajoutée après la création de la table demande
  titre VARCHAR(150) NOT NULL,
  description TEXT,
  nb_postes INT NOT NULL CHECK (nb_postes >= 1),
  experience_min_mois INT,                     -- NULL = pas d'exigence d'expérience
  seuil_acceptation INT NOT NULL DEFAULT 60 CHECK (seuil_acceptation BETWEEN 0 AND 100),
  date_publication DATE,
  date_limite DATE,
  statut VARCHAR(20) NOT NULL DEFAULT 'BROUILLON'
    CHECK (statut IN ('BROUILLON','PUBLIEE','CLOTUREE'))
);

CREATE TABLE offre_diplome_requis (
  offre_id BIGINT NOT NULL REFERENCES offre_emploi(id),
  diplome_id BIGINT NOT NULL REFERENCES diplome(id),
  PRIMARY KEY (offre_id, diplome_id)
);

CREATE TABLE candidature (
  id BIGSERIAL PRIMARY KEY,
  offre_id BIGINT NOT NULL REFERENCES offre_emploi(id),
  candidat_id BIGINT NOT NULL REFERENCES candidat(id),
  cv_document_id BIGINT REFERENCES document(id),
  score_matching NUMERIC(5,2),
  statut VARCHAR(25) NOT NULL DEFAULT 'RECUE'
    CHECK (statut IN ('RECUE','ACCEPTEE_ENTRETIEN','NON_RETENUE','EN_ENTRETIEN',
                       'REFUSEE_ENTRETIEN','EN_TEST','EN_EVALUATION','RETENUE','REFUSEE','EMBAUCHE')),
  date_candidature TIMESTAMP NOT NULL DEFAULT NOW(),
  UNIQUE (offre_id, candidat_id)
);

CREATE TABLE entretien (
  id BIGSERIAL PRIMARY KEY,
  candidature_id BIGINT NOT NULL REFERENCES candidature(id),
  date_heure TIMESTAMP NOT NULL,
  lieu VARCHAR(150),
  statut VARCHAR(15) NOT NULL DEFAULT 'PLANIFIE' CHECK (statut IN ('PLANIFIE','REALISE','ANNULE')),
  decision_rh VARCHAR(15) NOT NULL DEFAULT 'EN_ATTENTE' CHECK (decision_rh IN ('ACCEPTE','REFUSE','EN_ATTENTE')),
  commentaire TEXT
);

-- ========== TESTS (QCM par niveaux) ==========
CREATE TABLE test (
  id BIGSERIAL PRIMARY KEY,
  offre_id BIGINT NOT NULL REFERENCES offre_emploi(id),
  niveau INT NOT NULL CHECK (niveau BETWEEN 1 AND 5),
  titre VARCHAR(150) NOT NULL,
  UNIQUE (offre_id, niveau)
);

CREATE TABLE question_test (
  id BIGSERIAL PRIMARY KEY,
  test_id BIGINT NOT NULL REFERENCES test(id),
  enonce TEXT NOT NULL,
  ordre INT NOT NULL DEFAULT 0
);

CREATE TABLE choix_test (
  id BIGSERIAL PRIMARY KEY,
  question_id BIGINT NOT NULL REFERENCES question_test(id),
  libelle VARCHAR(255) NOT NULL,
  est_correct BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE TABLE passage_test (
  id BIGSERIAL PRIMARY KEY,
  candidature_id BIGINT NOT NULL REFERENCES candidature(id),
  test_id BIGINT NOT NULL REFERENCES test(id),
  note NUMERIC(5,2),
  decision_rh VARCHAR(15) NOT NULL DEFAULT 'EN_ATTENTE' CHECK (decision_rh IN ('REUSSI','ECHEC','EN_ATTENTE')),
  date_passage TIMESTAMP,
  UNIQUE (candidature_id, test_id)
);

CREATE TABLE reponse_test (
  id BIGSERIAL PRIMARY KEY,
  passage_id BIGINT NOT NULL REFERENCES passage_test(id),
  question_id BIGINT NOT NULL REFERENCES question_test(id),
  choix_id BIGINT REFERENCES choix_test(id)
);

-- ========== ÉVALUATIONS (QCM ou texte libre) ==========
CREATE TABLE evaluation (
  id BIGSERIAL PRIMARY KEY,
  candidature_id BIGINT NOT NULL REFERENCES candidature(id),
  titre VARCHAR(150) NOT NULL,
  format VARCHAR(10) NOT NULL CHECK (format IN ('QCM','TEXTE')),
  decision_rh VARCHAR(15) NOT NULL DEFAULT 'EN_ATTENTE' CHECK (decision_rh IN ('ACCEPTE','REFUSE','EN_ATTENTE')),
  date_evaluation TIMESTAMP
);

CREATE TABLE question_evaluation (
  id BIGSERIAL PRIMARY KEY,
  evaluation_id BIGINT NOT NULL REFERENCES evaluation(id),
  enonce TEXT NOT NULL,
  type VARCHAR(10) NOT NULL CHECK (type IN ('QCM','TEXTE')),
  ordre INT NOT NULL DEFAULT 0
);

CREATE TABLE choix_evaluation (
  id BIGSERIAL PRIMARY KEY,
  question_id BIGINT NOT NULL REFERENCES question_evaluation(id),
  libelle VARCHAR(255) NOT NULL,
  est_correct BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE TABLE reponse_evaluation (
  id BIGSERIAL PRIMARY KEY,
  evaluation_id BIGINT NOT NULL REFERENCES evaluation(id),
  question_id BIGINT NOT NULL REFERENCES question_evaluation(id),
  choix_id BIGINT REFERENCES choix_evaluation(id),
  texte_libre TEXT
);

-- ========== DEMANDES INTERNES (TICKETS) ==========
CREATE TABLE demande (
  id BIGSERIAL PRIMARY KEY,
  type VARCHAR(20) NOT NULL CHECK (type IN ('RECRUTEMENT','PROMOTION','TRANSFERT','FORMATION','MATERIEL')),
  auteur_id BIGINT NOT NULL REFERENCES utilisateur(id),
  poste_cible_id BIGINT REFERENCES poste(id),
  description TEXT NOT NULL,
  statut VARCHAR(20) NOT NULL DEFAULT 'SOUMISE'
    CHECK (statut IN ('SOUMISE','VALIDEE_CHEF','VALIDEE_DIRECTION','VALIDEE_RH','VALIDEE_DG',
                       'REJETEE','EN_EXECUTION','EXECUTEE')),
  date_creation TIMESTAMP NOT NULL DEFAULT NOW(),
  date_limite_execution TIMESTAMP
);

ALTER TABLE offre_emploi ADD CONSTRAINT fk_offre_demande_origine
  FOREIGN KEY (demande_origine_id) REFERENCES demande(id);

CREATE TABLE validation_demande (
  id BIGSERIAL PRIMARY KEY,
  demande_id BIGINT NOT NULL REFERENCES demande(id),
  etape VARCHAR(20) NOT NULL CHECK (etape IN ('CHEF_SERVICE','DIRECTION','RH','DG')),
  valideur_id BIGINT NOT NULL REFERENCES utilisateur(id),
  decision VARCHAR(10) NOT NULL CHECK (decision IN ('VALIDE','REJETE')),
  commentaire TEXT,
  date_validation TIMESTAMP NOT NULL DEFAULT NOW()
);

-- ========== ORDRES DU DG ==========
CREATE TABLE ordre (
  id BIGSERIAL PRIMARY KEY,
  dg_id BIGINT NOT NULL REFERENCES utilisateur(id),
  rh_id BIGINT NOT NULL REFERENCES utilisateur(id),
  description TEXT NOT NULL,
  delai_jours INT NOT NULL DEFAULT 7,
  date_emission TIMESTAMP NOT NULL DEFAULT NOW(),
  date_limite TIMESTAMP NOT NULL,
  statut VARCHAR(15) NOT NULL DEFAULT 'EMIS' CHECK (statut IN ('EMIS','EN_EXECUTION','EXECUTE'))
);

-- ========== NOTIFICATIONS ==========
CREATE TABLE notification (
  id BIGSERIAL PRIMARY KEY,
  utilisateur_id BIGINT NOT NULL REFERENCES utilisateur(id),
  message VARCHAR(255) NOT NULL,
  type VARCHAR(30),
  lien VARCHAR(255),
  lu BOOLEAN NOT NULL DEFAULT FALSE,
  date_creation TIMESTAMP NOT NULL DEFAULT NOW()
);

-- ========== INDEX UTILES ==========
CREATE INDEX idx_candidature_candidat ON candidature(candidat_id);
CREATE INDEX idx_candidature_offre ON candidature(offre_id);
CREATE INDEX idx_demande_auteur ON demande(auteur_id);
CREATE INDEX idx_notification_utilisateur_lu ON notification(utilisateur_id, lu);
CREATE INDEX idx_offre_statut ON offre_emploi(statut);
