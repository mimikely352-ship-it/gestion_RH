# Vérification et mise en service des blocs 1 à 7

Dépôt concerné : `C:\S5\GESTION\Gestion\Projet\gestion_RH`, branche `Cino`.
Ce bilan décrit le code actuel, pas l'ancien dossier `gestion_RH-main`.
La vérification SQL et le parcours sur une base migrée restent à réaliser par
l'utilisateur : aucun script de création, modification ou insertion n'a été exécuté.

## Couverture fonctionnelle

| Bloc | Fonctions et écrans présents | Contrôle de réception |
|---|---|---|
| 1 — Besoin | Poste, département déduit, effectif, création/remplacement, salaire mensuel, budget annuel, date souhaitée ; formulaires et validations historisées. | Le budget couvre effectif × salaire × 12 ; un rejet exige un motif ; le poste et l'effectif approuvés sont repris dans l'offre. |
| 2 — Recrutement | Offres publiques et RH, candidatures, entretiens successifs, tests, évaluations, notifications, prochaine étape affichée. | Un seul entretien planifié à la fois ; après décision, entretien suivant, tests ou refus. Clôture au quota et à l'échéance. |
| 3 — Embauche | Proposition avec réponse définitive ; à l'acceptation, dossier salarié, matricule, affectation, contrat et intégration dans une transaction. | Refus libérant le poste ; acceptation sans double embauche ; dossier visible avec le compte devenu salarié. |
| 4 — Contrats | Historique, lien au précédent, exemplaires signés, contrat à venir et activation automatique, checklist de sept tâches. | Contrat actuel conservé avant le renouvellement ; contrat annulé ou salarié parti non activé ; clôture de la checklist quand les tâches sont faites. |
| 5 — Dossier salarié | Identité, coordonnées, carrière, rémunérations, documents et leurs versions, pièces manquantes, habilitations et audit. | Les pièces uniques se remplacent sans écrasement ; les diplômes s'ajoutent. Vérifier les vues RH, titulaire, responsable et DG. |
| 6 — Temps | Régimes datés, exceptions, pointage, pauses, retards, heures supplémentaires validées, anomalies et export CSV. | Horaire figé au pointage ; correction RH motivée ; changement de régime limité à sa période avant le régime suivant. |
| 7 — Congés | Types, droits annuels, soldes réservés/acceptés, justificatifs, double validation, refus et annulation ; planning et variables de paie. | Le responsable et les RH conservent chacun leur commentaire ; chevauchements bloqués ; refus/annulation libérant les droits ; accès aux PDF limité au titulaire et aux RH. |

## Règles conservées dans cette application

Le circuit des besoins est **chef de service → direction → RH → DG**. Un employé
peut aussi soumettre un besoin via ses demandes ; ce n'est pas le circuit abrégé
du premier document `METIER.md`. Les offres indépendantes d'un besoin restent
possibles. Les congés sont en journées entières et dans une seule année ; il faut
scinder une période sur deux années. Les droits sont attribués par les RH : pas
d'acquisition ni de report automatique. Le pointage est diurne, une entrée et
une sortie par journée. L'export fournit des minutes, pas des bulletins de paie.

## Base de données : actions à effectuer manuellement

Arrêter l'ancienne application avant d'appliquer les migrations. Les valeurs
par défaut sont maintenant `app.schema-migrations.enabled=false` et
`app.demo-data.enabled=false` : ni migration ni compte de démonstration n'est
créé automatiquement. Vérifier aussi que votre fichier personnel
`config/application-local.properties` ne réactive pas ces options.

Depuis le dossier du dépôt, exécuter d'abord le diagnostic en lecture seule
(adapter `sirh` si votre base porte un autre nom) :

```powershell
psql -U postgres -d sirh -v ON_ERROR_STOP=1 -f sql/verification_blocs_1_a_7.sql
```

Le diagnostic contrôle les tables et colonnes principales ; il ne certifie pas
à lui seul tous les index, triggers et anciennes migrations. Appliquer seulement
les scripts manquants, dans l'ordre, en consultant leur contenu :

| Script | Objet |
|---|---|
| `18_proposition_embauche.sql` | Proposition et réponse du candidat. |
| `19_besoin_personnel.sql` | Données structurées du besoin. |
| `20_temps_conges_absences.sql` | Tables de planning, pointage, droits, congés et absences ; deux types initiaux. |
| `21_budget_recrutement.sql` | Budget annuel du besoin. |
| `22_embauche_contrats.sql` | Matricule, contrats reliés et conditions d'embauche automatique. |
| `23_conges_annulation.sql` | Motif d'annulation dans l'historique. |
| `24_conges_commentaire_responsable.sql` | Commentaire du responsable conservé séparément. |

Les prérequis sont les migrations antérieures du projet, notamment 16 et 17
pour les contrats et dossiers. Ne pas réimporter `01_schema.sql` sur une base
existante et ne pas rejouer le script 20 sur des tables déjà créées. Si les
migrations 18 à 23 sont déjà installées, la seule nouvelle migration de cette
correction est :

```powershell
psql -U postgres -d sirh -v ON_ERROR_STOP=1 -f sql/24_conges_commentaire_responsable.sql
```

Relancer ensuite le diagnostic. Si les migrations sont gérées manuellement,
laisser le mécanisme automatique désactivé : l'appliquer manuellement ne remplit
pas son registre `app_schema_migration`. Le script de jeu de test existant est
facultatif ; ne l'exécuter que dans une base de test dédiée après lecture.

## Réception des écrans

1. Chef/employé : déposer un besoin complet ; chaque valideur voit les mêmes
   effectif, budget, salaire et date. Un rejet vide est bloqué.
2. RH/candidat : publier l'offre, postuler, présélectionner, tenir un premier
   entretien, décider « entretien suivant », tenir le second, puis poursuivre
   les tests et l'évaluation. Vérifier les notifications et le suivi candidat.
3. RH/candidat : envoyer et accepter une proposition ; vérifier la fiche salarié,
   le matricule, l'affectation et le contrat. Tester aussi un refus.
4. RH/responsable : signer le contrat, cocher les tâches attribuées, programmer
   un renouvellement et vérifier son historique ainsi que les habilitations.
5. Salarié : pointer, indiquer une pause et vérifier les durées. RH : corriger
   avec motif et comparer le récapitulatif au CSV exporté.
6. RH : attribuer dix jours de droits à un salarié. Salarié : demander cinq
   jours planifiés ; vérifier cinq jours réservés et cinq jours disponibles.
7. Responsable : accepter avec commentaire. RH : décider avec un autre
   commentaire ; les deux restent lisibles. L'accord crée une absence sans
   double débit. Un refus rend les jours disponibles.
8. RH : annuler un congé accepté futur ; vérifier solde, disparition de l'absence,
   notification et motif. Tester une absence non rémunérée dans l'export.
9. Tester un solde insuffisant, un chevauchement, un PDF obligatoire manquant
   et l'accès au PDF d'un autre salarié. La saisie reste disponible après une
   erreur métier ; le PDF doit être sélectionné à nouveau.

## Vérifications techniques

Résultat du 12 novembre 2026 : **208 tests exécutés, aucun échec**, un test
PostgreSQL opt-in non exécuté ; **49 pages JSP compilées sans erreur** avec
Jasper ; syntaxe de `interface.js` vérifiée avec `node --check` ; WAR généré.
La compilation utilise un JDK 21 avec cible Java 17. Les rapports et le WAR de
cette vérification sont conservés dans `build-validation-blocs-20261112/`.

La compilation Java, les tests unitaires et contrôleurs, la compilation des JSP
et la vérification du JavaScript peuvent être exécutés sans base de données.
Le test PostgreSQL opt-in crée des données dans un schéma temporaire : il n'a
pas été lancé, conformément au choix de laisser les opérations SQL à l'utilisateur.
Les tests unitaires ne valident pas le rendu visuel ni la concurrence réelle
dans PostgreSQL ; terminer la réception ci-dessus après les migrations.
