# Blocs 6 et 7 — Temps de travail, congés et absences

## Démarrage

Arrêter l'ancienne application, puis lancer depuis le dossier `gestion_RH` :

```bat
ant -Dbuild.dir=build-bloc7 run
```

Les migrations automatiques sont désactivées par défaut. Appliquer manuellement
les migrations manquantes avant de démarrer : `20_temps_conges_absences.sql`,
`23_conges_annulation.sql`, puis `24_conges_commentaire_responsable.sql`, après
leurs prérequis. Le script 20 crée les tables et ne doit pas être rejoué si elles
existent déjà. Le [bilan des blocs 1 à 7](VALIDATION_BLOCS_1_A_7.md) précise les
contrôles et commandes. Aucun script SQL n'est exécuté par les tests unitaires.

Le menu **Temps et congés** rassemble les nouveaux écrans. Un salarié doit avoir
un dossier RH et un compte actif pour pointer ou demander un congé.

## Bloc 6 — Temps et planning

### RH

**Temps et congés → Pointages et planning**, puis choisir un salarié :

- Consulter le planning et les pointages sur une période de 62 jours maximum.
- Définir un régime hebdomadaire avec date d'effet, début, fin, pause et jours
  travaillés. Les régimes précédents sont conservés.
- Définir une exception datée : repos, jour férié ou horaires particuliers.
- Corriger un pointage avec un motif, la pause réellement prise et les minutes
  supplémentaires validées. Les corrections sont journalisées dans le dossier.
- Exporter les variables de présence au format CSV, pour la période affichée.

Sans régime spécifique : lundi–vendredi, 09 h–18 h et pause prévue de 60 minutes,
soit 480 minutes de travail. Il s'agit d'un réglage initial interne, modifiable
par les RH. Le planning ne peut pas être changé sur des jours déjà pointés ou
réservés par un congé/une absence ; régler d'abord la demande concernée.

### Salarié

**Mon pointage** : enregistrer l'entrée puis, à la sortie, indiquer la pause
réellement prise en minutes. L'heure vient du serveur, pas du navigateur.
Une seule entrée et sortie sont autorisées par journée. Un pointage resté ouvert
bloque une nouvelle entrée jusqu'à sa clôture ou une correction RH.
La sortie doit être saisie le jour de l'entrée. Si elle a été oubliée, les RH
corrigent le pointage du jour concerné ; une sortie le lendemain est refusée.

Temps travaillé = durée entre entrée et sortie moins pause réelle.
Retard = écart positif avec le début prévu. Le dépassement est comparé à la durée
prévue ; il ne devient une variable supplémentaire validée qu'après accord RH.
Le planning applicable est figé dans le pointage à l'entrée.
Les minutes prévues affichées dans le suivi et exportées pour un jour pointé
proviennent de cette valeur figée, même si le planning consulté est différent.

Le suivi signale les sorties manquantes, temps incomplets, pointages hors planning
et jours passés sans pointage ni absence enregistrée. Un pointage ouvert aujourd'hui
apparaît **En cours**, pas comme une absence.

## Bloc 7 — Congés et absences

### Paramétrage RH

**Congés et absences** :

1. Choisir un salarié et une année.
2. Définir son total de droits annuels par type, avec un motif.
3. Ajouter au besoin un type de congé : décompté ou non, rémunéré ou non,
   justificatif obligatoire ou facultatif.

Deux types sont initialisés : congé annuel rémunéré et congé sans solde.
**Aucun nombre de jours n'est attribué automatiquement.** Le congé annuel reste
bloqué tant que les RH n'ont pas attribué suffisamment de droits. La saisie d'un
total remplace le total annuel ; elle ne rajoute pas ce nombre au solde précédent.
Le total ne peut pas descendre sous les demandes réservées et acceptées.
Chaque révision est journalisée. Les règles d'un type créé restent fixes afin
de ne pas modifier les anciennes décisions.

### Demande et validation

1. Le salarié ouvre **Mes congés**, consulte son solde et soumet ses dates.
2. Le serveur contrôle le solde, les chevauchements, les pointages existants
   et le justificatif PDF requis par le type.
3. Les jours sont réservés dès le dépôt. Deux demandes simultanées ne peuvent
   pas dépenser le même solde : les écritures sont sérialisées par dossier.
4. Le responsable concerné valide, puis les RH prennent la décision finale.
   Sans responsable distinct actif, la demande est adressée directement aux RH.
   Les RH peuvent reprendre une demande dont le responsable a changé ou disparu.
   Une personne ne peut pas valider son propre congé.
5. L'accord RH crée une absence dans le planning et transforme la réservation
   en jours acceptés, sans les débiter une seconde fois.

Un refus exige un motif et libère les jours. Le salarié peut annuler une demande
en attente. Seuls les RH peuvent annuler un congé accepté commençant après
aujourd'hui : l'absence est désactivée et les jours redeviennent disponibles.
Les décisions et annulations restent horodatées et consultables.
Le commentaire du responsable reste visible après la décision finale : il est
conservé séparément du commentaire RH, dans l'historique salarié et la file RH.
Si le dépôt échoue, le type, les dates et le motif sont conservés. Le fichier
doit être sélectionné à nouveau ; l'écran l'indique. Le justificatif obligatoire
est signalé dès le choix du type, puis contrôlé de nouveau par le serveur.

Les demandes sont en journées entières et doivent rester dans une seule année.
Pour une période à cheval sur deux années, déposer deux demandes. Les jours
non travaillés sont exclus du décompte par défaut ; les exceptions du planning
sont prises en compte. La période complète est réservée contre les chevauchements.

### Autres absences et justificatifs

Les RH peuvent enregistrer une absence rémunérée ou non, avec un PDF facultatif,
et annuler une absence manuelle en indiquant le motif. L'absence ne peut pas
chevaucher un pointage ou un congé réservé. Pour un congé accepté, utiliser
l'annulation de la demande, pas celle d'une absence manuelle.

Les PDF sont limités à 5 Mo et leur en-tête est vérifié. Le téléchargement est
réservé au propriétaire et aux RH, y compris lorsque l'identifiant est connu.
Un responsable peut valider sa demande d'équipe mais ne peut pas télécharger
les justificatifs personnels. Les consultations sont journalisées. Un fichier
nouvellement déposé est supprimé si la transaction est annulée.

## Paramètres internes

Dans `application.properties` :

```properties
app.presence.zone=${SIRH_TIME_ZONE:Africa/Nairobi}
app.conges.jours-calendaires=${SIRH_CONGES_JOURS_CALENDAIRES:false}
app.conges.validation-manager=${SIRH_CONGES_VALIDATION_MANAGER:true}
```

Par défaut : jours planifiés et double validation. Les paramètres peuvent être
changés pour les nouvelles demandes ; les jours déjà réservés restent ceux
enregistrés lors du dépôt. Les droits sont des données RH configurées, sans
calcul automatique d'acquisition ni report d'une année à l'autre.

## Variables de paie et périmètre

Le CSV contient les minutes prévues, travaillées, en retard, de dépassement,
supplémentaires validées et d'absence non rémunérée, ainsi que les anomalies.
Les dates futures sont exclues de l'export. Les absences non rémunérées utilisent
les minutes du planning, pas un tarif salarial.

Les jours « Absence à vérifier » ne sont pas déduits automatiquement : les RH
doivent d'abord confirmer une absence. Les lignes de pointage en cours ou
incomplètes gardent leur signalement dans l'export.

Il s'agit de variables préparées pour la paie, pas d'un moteur de paie : aucun
bulletin, taux de majoration ou montant de retenue n'est calculé. Cette version
gère un horaire diurne et un pointage par jour, avec pause saisie à la sortie ;
elle ne gère pas les rotations de nuit, demi-journées ou badgeuses externes.

## Vérifications

Tests métier sans base de données :

```bat
ant -Dbuild.dir=build-bloc7 test-presence
```

Test PostgreSQL facultatif (CMD), dans un schéma temporaire créé puis supprimé :

```bat
set "SIRH_PRESENCE_INTEGRATION=true"
ant -Dbuild.dir=build-bloc7 test-presence
set "SIRH_PRESENCE_INTEGRATION="
```

La connexion de test utilise `SIRH_TEST_DB_URL`, `SIRH_TEST_DB_USER` et
`SIRH_TEST_DB_PASSWORD`, ou par défaut `localhost:5432/sirh`, `postgres`, sans mot
de passe. Le test vérifie les migrations et les repositories, les validations,
les soldes et réservations concurrentes, les pages HTTP authentifiées, le CSV
et les refus d'accès aux justificatifs. Il ne crée pas de comptes dans le schéma
de l'application et utilise un serveur sur un port temporaire distinct.
