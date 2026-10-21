# 02 - Base de données (PostgreSQL)

## 1. Schéma des relations
```
direction 1─N departement 1─N service 1─N section 1─N poste

utilisateur 1─1 candidat
utilisateur 1─1 employe
candidat    1─N candidature N─1 offre_emploi N─1 demande_recrutement N─1 poste
demande_recrutement 1─N validation
candidature 1─N entretien 1─N evaluation_candidat
candidature 1─1 proposition_embauche
candidat    1─0..1 employe
employe 1─N contrat (contrat_precedent_id → historique)
employe 1─N affectation N─1 poste
employe 1─1 onboarding 1─N onboarding_tache
document → candidat OU employe
notification N─1 utilisateur
```
> Les enums sont stockés en `VARCHAR` + `CHECK` (mappés en Java avec `@Enumerated(EnumType.STRING)`).

## 2. Script SQL
```sql
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
  departement_id BIGINT NOT NULL REFERENCES departement(id)
);
CREATE TABLE section (
  id BIGSERIAL PRIMARY KEY,
  nom VARCHAR(100) NOT NULL,
  service_id BIGINT NOT NULL REFERENCES service(id)
);
CREATE TABLE poste (
  id BIGSERIAL PRIMARY KEY,
  intitule VARCHAR(100) NOT NULL,
  description TEXT,
  section_id BIGINT NOT NULL REFERENCES section(id)
);

-- ========== UTILISATEURS ==========
CREATE TABLE utilisateur (
  id BIGSERIAL PRIMARY KEY,
  email VARCHAR(150) NOT NULL UNIQUE,
  mot_de_passe VARCHAR(255) NOT NULL,          -- hashé (BCrypt)
  role VARCHAR(20) NOT NULL
    CHECK (role IN ('CANDIDAT','MANAGER','RH','DIRECTION','EMPLOYE','ADMIN')),
  departement_id BIGINT REFERENCES departement(id), -- pour les managers
  actif BOOLEAN DEFAULT TRUE
);
CREATE TABLE candidat (
  id BIGSERIAL PRIMARY KEY,
  utilisateur_id BIGINT UNIQUE REFERENCES utilisateur(id),
  nom VARCHAR(100) NOT NULL,
  prenom VARCHAR(100) NOT NULL,
  telephone VARCHAR(20),
  date_naissance DATE,
  adresse VARCHAR(255)
);

-- ========== BLOC 1 : BESOIN ==========
CREATE TABLE demande_recrutement (
  id BIGSERIAL PRIMARY KEY,
  poste_id BIGINT NOT NULL REFERENCES poste(id),
  demandeur_id BIGINT NOT NULL REFERENCES utilisateur(id),
  effectif INT NOT NULL CHECK (effectif >= 1),
  motif VARCHAR(20) NOT NULL CHECK (motif IN ('CREATION','REMPLACEMENT')),
  budget NUMERIC(12,2),
  date_souhaitee DATE NOT NULL,
  statut VARCHAR(20) NOT NULL DEFAULT 'BROUILLON'
    CHECK (statut IN ('BROUILLON','SOUMISE','VALIDEE_RH','VALIDEE_DIRECTION','REJETEE')),
  date_creation TIMESTAMP DEFAULT NOW()
);

CREATE TABLE validation (
  id BIGSERIAL PRIMARY KEY,
  demande_id BIGINT NOT NULL REFERENCES demande_recrutement(id),
  valideur_id BIGINT NOT NULL REFERENCES utilisateur(id),
  niveau VARCHAR(20) NOT NULL CHECK (niveau IN ('RH','DIRECTION')),
  decision VARCHAR(10) NOT NULL CHECK (decision IN ('VALIDE','REJETE')),
  commentaire TEXT,
  date_validation TIMESTAMP DEFAULT NOW()
);

-- ========== BLOC 2 : RECRUTEMENT ==========
CREATE TABLE offre_emploi (
  id BIGSERIAL PRIMARY KEY,
  demande_id BIGINT NOT NULL REFERENCES demande_recrutement(id),
  titre VARCHAR(150) NOT NULL,
  description TEXT,
  nb_postes INT NOT NULL CHECK (nb_postes >= 1),
  date_publication DATE,
  date_limite DATE NOT NULL,
  statut VARCHAR(20) NOT NULL DEFAULT 'BROUILLON'
    CHECK (statut IN ('BROUILLON','PUBLIEE','CLOTUREE'))
);
CREATE TABLE candidature (
  id BIGSERIAL PRIMARY KEY,
  offre_id BIGINT NOT NULL REFERENCES offre_emploi(id),
  candidat_id BIGINT NOT NULL REFERENCES candidat(id),
  lettre_motivation TEXT,
  date_candidature TIMESTAMP DEFAULT NOW(),
  statut VARCHAR(20) NOT NULL DEFAULT 'RECUE'
    CHECK (statut IN ('RECUE','PRESELECTIONNEE','EN_ENTRETIEN','RETENUE','REFUSEE')),
  UNIQUE (offre_id, candidat_id)                -- RG7
);
CREATE TABLE entretien (
  id BIGSERIAL PRIMARY KEY,
  candidature_id BIGINT NOT NULL REFERENCES candidature(id),
  type VARCHAR(20) NOT NULL CHECK (type IN ('ENTRETIEN','TEST')),
  date_heure TIMESTAMP NOT NULL,
  lieu VARCHAR(150),
  statut VARCHAR(20) DEFAULT 'PLANIFIE' CHECK (statut IN ('PLANIFIE','REALISE','ANNULE'))
);
CREATE TABLE evaluation_candidat (
  id BIGSERIAL PRIMARY KEY,
  entretien_id BIGINT NOT NULL REFERENCES entretien(id),
  evaluateur_id BIGINT NOT NULL REFERENCES utilisateur(id),
  note NUMERIC(4,2) CHECK (note BETWEEN 0 AND 20),
  avis VARCHAR(15) CHECK (avis IN ('FAVORABLE','RESERVE','DEFAVORABLE')),
  commentaire TEXT
);

-- ========== BLOC 3 : EMBAUCHE ==========
CREATE TABLE proposition_embauche (
  id BIGSERIAL PRIMARY KEY,
  candidature_id BIGINT NOT NULL UNIQUE REFERENCES candidature(id),
  type_contrat VARCHAR(10) NOT NULL CHECK (type_contrat IN ('CE','CDD','CDI')),
  salaire_propose NUMERIC(12,2) NOT NULL,
  date_debut DATE NOT NULL,
  date_fin DATE,
  statut VARCHAR(10) NOT NULL DEFAULT 'ENVOYEE'
    CHECK (statut IN ('ENVOYEE','ACCEPTEE','REFUSEE')),
  date_reponse TIMESTAMP
);
CREATE TABLE employe (
  id BIGSERIAL PRIMARY KEY,
  matricule VARCHAR(20) NOT NULL UNIQUE,        -- EMP-2026-0001
  utilisateur_id BIGINT UNIQUE REFERENCES utilisateur(id),
  candidat_id BIGINT UNIQUE REFERENCES candidat(id),
  nom VARCHAR(100) NOT NULL,
  prenom VARCHAR(100) NOT NULL,
  date_embauche DATE NOT NULL,
  statut VARCHAR(10) DEFAULT 'ACTIF' CHECK (statut IN ('ACTIF','SORTI'))
);

-- ========== BLOC 4 : CONTRAT & ONBOARDING ==========
CREATE TABLE contrat (
  id BIGSERIAL PRIMARY KEY,
  employe_id BIGINT NOT NULL REFERENCES employe(id),
  type VARCHAR(10) NOT NULL CHECK (type IN ('CE','CDD','CDI')),
  date_debut DATE NOT NULL,
  date_fin DATE,
  salaire NUMERIC(12,2) NOT NULL,
  statut VARCHAR(10) NOT NULL DEFAULT 'ACTIF' CHECK (statut IN ('ACTIF','TERMINE')),
  contrat_precedent_id BIGINT REFERENCES contrat(id),
  CHECK (type = 'CDI' OR date_fin IS NOT NULL)   -- RG16
);
-- RG15 : un seul contrat actif par employé
CREATE UNIQUE INDEX uq_contrat_actif ON contrat(employe_id) WHERE statut = 'ACTIF';

CREATE TABLE affectation (
  id BIGSERIAL PRIMARY KEY,
  employe_id BIGINT NOT NULL REFERENCES employe(id),
  poste_id BIGINT NOT NULL REFERENCES poste(id),
  date_debut DATE NOT NULL,
  date_fin DATE
);
CREATE TABLE onboarding (
  id BIGSERIAL PRIMARY KEY,
  employe_id BIGINT NOT NULL UNIQUE REFERENCES employe(id),
  date_debut DATE NOT NULL,
  statut VARCHAR(10) DEFAULT 'EN_COURS' CHECK (statut IN ('EN_COURS','TERMINE'))
);
CREATE TABLE onboarding_tache (
  id BIGSERIAL PRIMARY KEY,
  onboarding_id BIGINT NOT NULL REFERENCES onboarding(id),
  libelle VARCHAR(150) NOT NULL,
  responsable VARCHAR(20) NOT NULL,             -- RH, MANAGER, ADMIN
  statut VARCHAR(10) DEFAULT 'A_FAIRE' CHECK (statut IN ('A_FAIRE','FAITE')),
  date_realisation DATE
);

-- ========== TRANSVERSE ==========
CREATE TABLE document (
  id BIGSERIAL PRIMARY KEY,
  type VARCHAR(30) NOT NULL,                    -- CV, CONTRAT_SIGNE, CIN, DIPLOME...
  nom_fichier VARCHAR(255) NOT NULL,
  chemin VARCHAR(255) NOT NULL,
  candidat_id BIGINT REFERENCES candidat(id),
  employe_id BIGINT REFERENCES employe(id),
  date_upload TIMESTAMP DEFAULT NOW(),
  CHECK (candidat_id IS NOT NULL OR employe_id IS NOT NULL)
);
CREATE TABLE notification (
  id BIGSERIAL PRIMARY KEY,
  utilisateur_id BIGINT NOT NULL REFERENCES utilisateur(id),
  message VARCHAR(255) NOT NULL,
  lu BOOLEAN DEFAULT FALSE,
  date_creation TIMESTAMP DEFAULT NOW()
);
```

## 3. Requête utile : places libres d'une offre
```sql
SELECT o.id, o.titre,
       o.nb_postes - COUNT(c.id) FILTER (WHERE c.statut = 'RETENUE') AS places_libres
FROM offre_emploi o
LEFT JOIN candidature c ON c.offre_id = o.id
GROUP BY o.id;
```

-- ================== FORM CV ==================

```sql
CREATE TABLE niveau_diplome (
  id  BIGSERIAL PRIMARY KEY,
  niveau VARCHAR(255) NOT NULL
)
CREATE TABLE diplome_filiaire (
  id  BIGSERIAL PRIMARY KEY,
  filiaire VARCHAR(255) NOT NULL
)

--- filiaire = Info
--- option = dev
--- Diplome = licence en informatique developpement

CREATE TABLE diplome_option (
  id BIGSERIAL PRIMARY KEY,
  id_diplome_filiaire BIGINT NOT NULL REFERENCES diplome_filiaire(id),
  options VARCHAR(255) NOT NULL
)

CREATE TABLE diplome (
  id BIGSERIAL PRIMARY KEY,
  id_niveau BIGINT NOT NULL REFERENCES niveau_diplome(id),
  id_diplome_option BIGINT NOT NULL REFERENCES diplome_option(id)
)