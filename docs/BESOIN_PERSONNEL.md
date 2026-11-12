# Besoin de personnel

## Données

Pour une nouvelle demande de recrutement, renseigner :

- Poste existant (le département est lu depuis sa hiérarchie).
- Motif : création de poste ou remplacement.
- Effectif demandé, entier strictement positif.
- Salaire brut mensuel prévu **pour un poste**, en ariary, strictement positif,
  avec deux décimales maximum.
- Budget global : enveloppe salariale annuelle positive couvrant au minimum
  `effectif × salaire mensuel par poste × 12`.
- Date souhaitée, aujourd'hui ou ultérieure lors du dépôt.
- Description du besoin.

Exemple : pour 3 postes à 1 250 000 Ar chacun, saisir **3** et **1 250 000**.
Le salaire enregistré n'est pas multiplié par l'effectif. Il ne représente
ni un budget global ni un coût annuel. Il sert de valeur initiale pour la
proposition d'embauche. Le budget est saisi séparément ; les propositions
en attente et les embauches consomment cette enveloppe. Un refus de proposition
libère sa réservation. Le salaire peut être négocié dans le budget disponible.

## Utilisation

Le chef de service utilise **Besoins de personnel → Nouveau besoin**.
L'employé peut utiliser **Mes demandes → Nouvelle demande → RECRUTEMENT**.
Le service de l'auteur et son responsable doivent être renseignés.

Le circuit existant est conservé : chef de service, direction, RH, puis DG.
Les données du besoin sont affichées à chaque validation et restent inchangées.
Le dépôt par un chef ne valide pas automatiquement sa demande : il passe par
le même circuit et retrouve l'étape chef dans sa liste des validations.

Après accord du DG, les RH exécutent la demande et créent l'offre. Le poste et
l'effectif sont préremplis. Pour les nouveaux besoins renseignés, un poste ou
un effectif différent est refusé côté serveur. La date souhaitée est informative
et n'est pas utilisée comme date limite de candidature.

Les anciennes demandes sont conservées. Les champs absents affichent
« Non renseigné ». La nouvelle saisie obligatoire ne réécrit pas l'historique.

## Installation et vérifications

Migrations additives : `sql/19_besoin_personnel.sql` puis
`sql/21_budget_recrutement.sql`, après leurs prérequis. Elles doivent être
appliquées manuellement avant le démarrage : les migrations automatiques sont
désactivées par défaut. Voir [le bilan des blocs](VALIDATION_BLOCS_1_A_7.md).

Arrêter l'ancienne application avant de lancer la version mise à jour :

```bat
ant -Dbuild.dir=build-besoins run
```

Tests ciblés : salaire conservé pendant les quatre validations, contrôles des
champs requis et des montants/dates, données de recrutement ignorées pour les
autres types, auteur issu du compte connecté, conservation des valeurs après
erreur, poste et effectif approuvés respectés lors de la création d'offre.
Les vues JSP et le JavaScript ont été vérifiés. Le parcours complet avec les
différents comptes et une base migrée reste à tester dans le navigateur.
