# 03 - Conception de l'affichage (écrans)

> Le front (React, Angular, Vue… au choix) consomme l'API REST Spring Boot.
> Chaque espace est accessible selon le rôle de l'utilisateur connecté.

## 1. Espace public / Candidat

### Header (commun)
- **Accueil**
- **Connexion** (employé / personnel interne)
- **Inscription / Login candidat**
- **Bulle de notification** 🔔 (nombre de non-lues, visible si connecté)
- Lien **Mon espace** (dashboard candidat, si connecté)

### Page Accueil — liste des offres
- **Filtres :** Direction → Département → Service → Section → Poste (listes déroulantes en cascade)
- **Tableau des offres :**

| Poste | Département | Date création offre | Date échéance | Postes libres | Action |
|---|---|---|---|---|---|
| Chauffeur | Logistique | 01/10/2026 | 31/10/2026 | 2 | Détail |

### Page Détail offre
- Titre, description, localisation dans l'organisation, nombre de postes, date limite
- Bouton **Postuler** (redirige vers login si non connecté ; désactivé si déjà postulé ou date dépassée)

### Formulaire Postuler
- Lettre de motivation, upload CV (PDF)

### Inscription candidat
- Nom, prénom, email, téléphone, mot de passe

### Dashboard candidat
- **Mes candidatures :** offre, date, statut (badge coloré), détail
- **Entretiens à venir :** date, heure, lieu
- **Proposition d'embauche :** boutons Accepter / Refuser
- **Notifications**
- **Mon profil :** infos personnelles + CV

## 2. Espace interne (après Connexion)
Menu latéral adapté au rôle.

### Manager
- **Mes demandes de recrutement :** liste + statut
- **Nouvelle demande :** poste, effectif, motif, budget, date souhaitée → Enregistrer / Soumettre
- **Onboarding :** tâches qui lui sont assignées (cocher « Faite »)

### RH
- **Demandes à valider :** liste → Valider / Rejeter (commentaire)
- **Offres :** créer une offre depuis une demande validée, publier, clôturer
- **Candidatures d'une offre :** liste, changer statut (Présélectionner / Refuser / Retenir), voir CV
- **Entretiens :** planifier, saisir une évaluation (note, avis, commentaire)
- **Propositions d'embauche :** créer (type contrat, salaire, date début) et suivre
- **Employés :** liste (matricule, nom, poste, contrat actif) → fiche employé
- **Fiche employé :** infos, historique des contrats, affectations, documents, bouton « Renouveler / Nouveau contrat »
- **Onboarding :** progression par employé (ex : 4/7 tâches)

### Direction
- **Demandes validées par RH :** Valider / Rejeter
- **Tableau de bord simple :** nb demandes, offres ouvertes, embauches du mois

### Employé
- **Mon dossier :** matricule, poste, contrats, documents
- **Mon onboarding :** checklist

### Admin
- **Organisation :** CRUD Direction / Département / Service / Section / Poste
- **Utilisateurs :** créer les comptes Manager, RH, Direction

## 3. Codes couleur des statuts
| Couleur | Statuts |
|---|---|
| Gris | BROUILLON, RECUE, A_FAIRE |
| Bleu | SOUMISE, PUBLIEE, EN_ENTRETIEN, ENVOYEE, EN_COURS |
| Vert | VALIDEE_*, RETENUE, ACCEPTEE, ACTIF, FAITE, TERMINE |
| Rouge | REJETEE, REFUSEE |
