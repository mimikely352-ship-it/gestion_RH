# Gestion RH:

	-> Direction -> Departement -> Service -> Section -> Employer

- Manifestation de besoin de personnel
- Recrutement
Ex: Departement logistique a besoin de chauffeur
- Vie de salariee
- Gestion de carriere (Evolution)
- Rupture de contrat

## Objectif:
- Comprendre les processus RH avant de concevoir les fonctionnalites du logiciel.
- Transforner un processus metier en regkes de gestion, donnees, workflow et interface.
- Concevoir un SIRH (System d'Information des ressources humaine) couvrant tout le cycle de vie du salarie.
- Eviter une conception limite a de simples ecrans CRUD.
- Faire evoluer un meme cas pratique jusqu'a l'architecture et au MVP.

## Bloc 1: Expression du personnel
- Un manager exprime un besoin: creation ou remplacement d'un poste.
- Information: poste, departement, effectif, motif, budget, date souhaitee.
- Workflow: Manager -> RH -> Direction -> ouverture du recrutement.
- Objets metier: departement, poste, demandeRevrutement, validation.
- Point de conception: un POSTE existe independament de l'EMPLOYER.

## Bloc 2: Recrutement
- Transformer une demande validee en offre d'emploi.
- Publer l'offre et recevoir plusieur candidature.
- Preselectionner, organiser les entretients et les tests.
- Concerver les evaliations et la decision finale.
	Objets: offreEmploi, Candidat, Candidature, Entretien, EvaluationCandidat.

## Bloc 3: Section et embauche
- Le candidat retenu recoit une proposition d'embauche.
- Apres acceptation: creation du dossier salarie et du matricule.
- Créer le contrat, le poste et l'affectation.
- Point de conception: CANDIDAT != EMPLOYE.
- Une personne peut avoir plusieurs candidatures avant de devenir salarie.

## Bloc 4: Contrat et onboarding
- Un salarie peut avoir plusieurs contrat successifs: CDD, CE, renouvellement, CDI…
- Ne pas ecraser l'historique en stockant seulement employe, type_contrat.
- Onboarding: contrat signe, badge, compte, materiel, formation, inscription paie.
- Utiliser une checklist et un workflow d'integration.
	Objet: Contrat, Affectation, Onboarding, OnboardingTache, Document.

***
	Creation automatique de Compte bancaire et autre apres validation
***


