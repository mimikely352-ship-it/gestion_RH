# 01 - Analyse métier (SIRH)

## 1. Périmètre du projet

1. Expression du besoin de personnel
2. Recrutement
3. Sélection et embauche
4. Contrat et onboarding


## 2. Organisation
```
Direction → Département → Service → Section → Poste → (Employé affecté)
```
**Règle clé :** un **POSTE existe indépendamment de l'EMPLOYÉ**. Un poste peut être vacant, occupé, ou occupé successivement par plusieurs employés.

## 3. Acteurs
| Acteur | Rôle |
|---|---|
| Candidat | S'inscrit, consulte les offres, postule, suit ses candidatures |
| Manager | Exprime un besoin de recrutement |
| RH | Valide les demandes, publie les offres, gère candidatures, entretiens, embauche, onboarding |
| Direction | Validation finale des demandes |
| Employé | Consulte son dossier, ses contrats, ses tâches d'onboarding |
| Admin(DG) | Gère l'organisation (directions, départements…) et les comptes |

## 4. Bloc 1 — Demande de recrutement
**Infos :** poste, département, effectif, motif (CREATION / REMPLACEMENT), budget, date souhaitée.

**Workflow :**
```
BROUILLON → SOUMISE → VALIDEE_RH → VALIDEE_DIRECTION → (offre créée)
                  ↘ REJETEE      ↘ REJETEE
```
**Règles de gestion :**
- seul un Manager peut créer une demande, pour un poste de son département.
- effectif ≥ 1, date souhaitée ≥ date du jour.
- chaque validation/rejet est historisé (qui, quand, décision, commentaire).
- un rejet exige un commentaire.
- une offre ne peut être créée qu'à partir d'une demande VALIDEE_DIRECTION.

## 5. Bloc 2 — Recrutement
**Statut offre :** `BROUILLON → PUBLIEE → CLOTUREE`

**Statut candidature :**
```
RECUE → PRESELECTIONNEE → EN_ENTRETIEN → RETENUE
   ↘ REFUSEE (possible à chaque étape)
```
**Règles :**
- on ne peut postuler qu'à une offre PUBLIEE et avant la date limite.
- un candidat ne postule qu'une seule fois par offre.
- une candidature peut avoir plusieurs entretiens/tests, chacun avec une ou plusieurs évaluations (note /20, avis, commentaire).
- le candidat est notifié à chaque changement de statut.
- l'offre est clôturée automatiquement quand le nombre de postes est pourvu ou la date limite dépassée.

## 6. Bloc 3 — Sélection et embauche
**Règle clé : CANDIDAT ≠ EMPLOYÉ.** Une personne peut avoir plusieurs candidatures avant de devenir salariée.

**Statut proposition :** `ENVOYEE → ACCEPTEE / REFUSEE`

**Règles :**
- une proposition n'est possible que pour une candidature RETENUE.
- à l'acceptation, le système crée **automatiquement** (en une seule transaction) :
  - le dossier employé + **matricule** unique (format `EMP-AAAA-0001`)
  - le compte utilisateur employé
  - le premier contrat
  - l'affectation au poste
  - l'onboarding avec ses tâches
- si refus, le RH peut proposer à un autre candidat retenu.

## 7. Bloc 4 — Contrat et onboarding
**Règles contrat :**
- un employé peut avoir **plusieurs contrats successifs** (CDD, CE, renouvellement, CDI). On ne modifie jamais un ancien contrat : on en crée un nouveau.
- un seul contrat ACTIF à la fois par employé.
- CDD / CE → date de fin obligatoire ; CDI → pas de date de fin.
- un renouvellement référence le contrat précédent (qui passe à TERMINE).

**Statut contrat :** `ACTIF → TERMINE`

**Onboarding (checklist générée automatiquement) :**
| Tâche | Responsable |
|---|---|
| Signature du contrat | RH |
| Création du badge | RH |
| Création compte informatique | Admin |
| Remise du matériel | Manager |
| Formation d'intégration | Manager |
| Inscription paie | RH |
| Ouverture compte bancaire | RH |

- l'onboarding passe à TERMINE quand toutes les tâches sont FAITE.
- les documents (CV, contrat signé, pièces d'identité) sont rattachés au candidat ou à l'employé.

## 8. Objets métier
`Direction, Departement, Service, Section, Poste, Utilisateur, DemandeRecrutement, Validation, OffreEmploi, Candidat, Candidature, Entretien, EvaluationCandidat, PropositionEmbauche, Employe, Contrat, Affectation, Onboarding, OnboardingTache, Document, Notification`
