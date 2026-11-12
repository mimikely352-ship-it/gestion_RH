# Proposition d'embauche

## Parcours

1. RH : ouvrir une candidature au statut `RETENUE`.
2. Renseigner le contrat, le salaire brut mensuel en ariary, l'entrée, la fin
   prévue pour un contrat à durée déterminée, le rôle attribué (employé, chef de
   service, direction) et, si le poste n'en a pas, le service d'accueil. Envoyer.
   L'envoi est refusé si tous les postes de l'offre sont pourvus ou déjà proposés,
   si le candidat a déjà un dossier salarié ou si le budget du besoin est dépassé.
3. Candidat : ouvrir la notification ou **Mes candidatures → Proposition
   d'embauche**, puis accepter ou refuser.
4. À l'acceptation, le système crée automatiquement, en une seule transaction
   (METIER.md, bloc 3) : le compte employé (même identifiant, nouveau rôle), le
   dossier salarié et son matricule `EMP-AAAA-0001`, l'affectation au poste, le
   premier contrat aux conditions acceptées (« à venir » si l'entrée est future)
   et la checklist d'intégration en 7 tâches. Le candidat arrive directement sur
   son dossier salarié ; les RH reçoivent la notification « Nouvel employé ».
5. Poursuivre la signature du contrat et la checklist depuis le dossier salarié.

L'envoi n'est pas une embauche. Un refus libère le poste pour un autre candidat
retenu. La réponse est horodatée et définitive. Une seule proposition est
autorisée par candidature : aucune modification des conditions après envoi.
La consultation et la réponse sont réservées au candidat destinataire.
Les notifications sont internes à l'application ; aucun email n'est envoyé.

Reprise de l'existant : une proposition acceptée avant cette version (sans rôle
enregistré) se finalise depuis la candidature avec **Créer le dossier salarié et
embaucher**. Les embauches déjà réalisées sont conservées.

## Contrats à venir

Un contrat dont la date de début est future (embauche, renouvellement saisi à
l'avance) a le statut « à venir » : le contrat en cours reste applicable jusqu'à
la veille, puis la bascule est automatique (au démarrage et chaque nuit à 0 h 10).
Un renouvellement référence le contrat qu'il remplace. Un contrat à venir peut
être annulé depuis le dossier salarié ; un départ clôturé l'annule aussi.

## Installation

La migration additive `sql/18_proposition_embauche.sql` ajoute les colonnes de
proposition à `candidature`, sans supprimer de données. Elle est embarquée par
Ant. Appliquer aussi `sql/22_embauche_contrats.sql` pour les conditions complètes,
le matricule et les contrats à venir. Les migrations sont manuelles par défaut.
Ne pas modifier une migration après son application.

En production, les migrations automatiques sont désactivées : appliquer le
script avec le processus de déploiement avant de démarrer cette version.

Depuis le dossier du projet, après arrêt de l'ancienne application :

```bat
ant -Dbuild.dir=build-complements run
```

## Vérifications

Tests unitaires : propriétaire de la proposition, contrats et dates invalides,
envoi unique, acceptation, refus, réponse définitive, blocage de l'embauche sans
acceptation ou avant l'entrée, création du contrat avec les conditions acceptées.
Les tests ciblés des services recrutement, demande, évaluation et embauche
passent. Les vues JSP compilent ; le parcours avec deux sessions RH/candidat
reste à valider dans le navigateur sur une base migrée.

Le besoin de personnel utilise un salaire brut mensuel par poste. Voir
[Besoin de personnel](BESOIN_PERSONNEL.md) pour la saisie et les validations.
