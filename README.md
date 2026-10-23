# Oria RH — Gestion des talents

## Fiabilisation des parcours

- Dossier employé et affectations datées conservés dans `dossier_employe` et `affectation_employe`.
  Les libellés du poste et du service sont figés au moment du mouvement.
- Transfert : demande directe aux RH, une seule demande de mouvement ouverte par employé,
  acceptation/refus, puis réalisation avec une date effective passée ou du jour.
  Le poste reste inchangé tant que les RH n'ont pas confirmé la réalisation.
- Les postes ouverts à la mobilité se gèrent dans **RH → Mobilité interne**, sans annonce publique obligatoire.
- À l'embauche, le service connu du poste est repris ; sinon les RH sélectionnent le service d'accueil.
- Le dossier candidat reste consultable après embauche. Les formations réalisées apparaissent dans le parcours.
- Une demande de recrutement est réalisée à la **publication** de l'offre associée, pas à l'enregistrement d'un brouillon.
- QCM : contrôle de l'offre, de l'étape, des niveaux précédents et de l'appartenance des choix aux questions.
  Une soumission est définitive ; une évaluation décidée n'accepte plus de réponses.
- Les décisions concurrentes sur demandes, ordres, entretiens et évaluations sont protégées par version.

### Migration et anciennes données

En développement, `ant run` / `build.bat run` appliquent automatiquement les migrations **09 puis 10**
avant la validation Hibernate. Elles sont incluses dans le WAR par `build.xml` et suivies dans
`app_schema_migration` avec un checksum et un verrou PostgreSQL. Le schéma initial doit déjà exister.
Ne plus modifier une migration après son application : ajouter une nouvelle migration.

`sql/10_fiabilisation_metier.sql` reprend les anciennes embauches et les mouvements exécutés.
Les dates inconnues restent inconnues. Si plusieurs mouvements anciens n'ont pas de date,
l'affectation est signalée comme à vérifier : aucun poste courant n'est inventé.
La correction d'une date d'entrée est tracée avec le compte RH et l'ancienne valeur.

Pour une installation contrôlée, arrêter l'application, sauvegarder la base, puis appliquer dans psql :

```sql
\i C:/S5/GESTION/Gestion/Projet/gestion_RH/sql/09_dates_parcours_employes.sql
\i C:/S5/GESTION/Gestion/Projet/gestion_RH/sql/10_fiabilisation_metier.sql
```

### Sécurité et production

Les API métier `/api/**` exigent désormais `Authorization: Bearer <jeton>` : le cookie navigateur
seul n'y authentifie plus la requête. Les formulaires JSP conservent la protection CSRF.
La déconnexion est un POST protégé et révoque les anciens jetons du compte.
Les comptes désactivés et les jetons portant un ancien rôle sont refusés.

Le profil `prod` (`--spring.profiles.active=prod`) exige `SIRH_DB_URL`, `SIRH_DB_USER`,
`SIRH_DB_PASSWORD` et `SIRH_JWT_SECRET` sans valeur de repli, active le cookie Secure et désactive
la création des comptes de démonstration ainsi que les migrations automatiques.
Les migrations doivent alors être appliquées par l'opérateur avant le démarrage.
Les comptes de démonstration **déjà présents** doivent être retirés ou sécurisés explicitement.
HTTPS, sauvegardes restaurables, analyse antivirus des documents et supervision restent à configurer
dans l'infrastructure. Cette version ne fournit pas d'isolation entre plusieurs entreprises.

Voir [le bilan des corrections](docs/CORRECTIONS_METIER.md) pour le périmètre et la validation.

### Circuit des transferts

Employé → décision RH (acceptation ou refus) → réalisation confirmée par les RH.
Le poste et le service changent à la réalisation, pas à la demande ni à l’acceptation.
Les transferts utilisent `SOUMISE`, puis `VALIDEE_RH`, puis `EN_EXECUTION` et `EXECUTEE`,
ou `REJETEE`. Les transferts anciens encore en validation sont repris directement par les RH.
Les autres types de demandes conservent leur circuit hiérarchique. Aucun changement SQL pour ce circuit.

Le service de l’employé est repris automatiquement depuis la section du poste à l’embauche.
Pour les anciennes embauches sans service, ouvrir « Nouvelle demande » complète ce lien
depuis l’embauche ou le dernier mouvement exécuté, si les données sont non ambiguës.
Une demande de transfert ne change l’affectation qu’au moment de son exécution par les RH.
Le script historique `rattacher_employe.sql` ne doit plus être utilisé après la migration 10 : le dossier employé est désormais la source de l’affectation.

## Identité visuelle et logo

Le nom visible est **Oria RH**, en accord avec le monogramme du logo choisi.
Les noms techniques, la base `sirh`, le WAR `sirh.war`,
les routes et les comptes existants sont conservés.

La charte vert forêt, sauge et crème est centralisée dans
`src/main/webapp/assets/css/theme.css`, chargé après les styles des composants.
Modifier les variables `--marque`, `--marque-fonce`, `--foret`, `--papier`,
`--surface` et `--sauge` pour changer les couleurs. Les fichiers CSS à la racine
du projet sont des maquettes historiques ; les JSP chargent les fichiers sous
`src/main/webapp/assets/css/`.

Le logo choisi est `Projet/uploads/logo.jpg`, soit `../uploads/logo.jpg` depuis
`gestion_RH`. Remplacer ce fichier met à jour le logo après actualisation du
navigateur. Le service ne publie que ce fichier, pas le dossier des CV.
Une copie est incluse dans `src/main/webapp/assets/brand/logo.jpg` pour que le
logo reste disponible dans le WAR si le fichier externe n'est pas présent.
Pour changer son emplacement, définir `SIRH_BRAND_LOGO` avec un chemin absolu
vers un JPEG, PNG ou WebP. Pour changer le nom affiché, modifier `app.brand.name`
dans `application.properties`. En déploiement Tomcat, un chemin absolu est conseillé
si le logo doit continuer à être lu depuis le dossier uploads externe.

`ini.jpg` sert uniquement de référence de charte ; il n'est pas utilisé comme
logo ni comme illustration d'une offre. Aucune migration de base n'est requise.

Application de gestion RH : recrutement (offres, candidatures, tests QCM, évaluations), demandes
internes avec circuit hiérarchique (Employé → Chef de service → Direction → RH → DG), ordres du DG,
notifications. Vues JSP + API REST.

**Stack :** Java 17 · Spring Boot 3.5 · JSP/JSTL (pas de Thymeleaf) · PostgreSQL · build **Ant** ·
déploiement WAR sur **Tomcat 10.1+ externe**.

## Démarrage rapide (TL;DR)

```bash
# 1. Base de données (une fois)
DROP DATABASE IF EXISTS sirh WITH (FORCE);
CREATE DATABASE sirh;
\c sirh

\set ON_ERROR_STOP on
\i 'C:/S5/GESTION/Gestion/Projet/gestion_RH/sql/01_schema.sql'
\i 'C:/S5/GESTION/Gestion/Projet/gestion_RH/sql/02_seed.sql'
\dt

sudo -u postgres createdb sirh
sudo -u postgres psql -d sirh -f sql/01_schema.sql
sudo -u postgres psql -d sirh -f sql/02_seed.sql

# 2. Config : adapter src/main/resources/application.properties
#    (spring.datasource.username / spring.datasource.password au minimum)
sudo update-alternatives --config java
sudo update-alternatives --config javac

chmod +x build.sh

# 3. Build
./build.sh war     # Linux/macOS/Git Bash

cd /d C:\S5\GESTION\Gestion\Projet\gestion_RH
build.bat war         # Windows (cmd/PowerShell)

# 4. Déploiement manuel sur Tomcat
# Copier build/dist/sirh.war dans $CATALINA_HOME/webapps/ (le renommer ROOT.war pour servir à la racine)
sudo cp gestion_RH/build/dist/sirh.war /opt/tomcat/webapps/
# puis (re)démarrer Tomcat
```
/opt/tomcat/bin/shutdown.sh && /opt/tomcat/bin/startup.sh

./shutdown.sh
./startup.sh

Application accessible sur `http://<serveur>:8080/` (ou `/sirh/` si le WAR garde son nom).

## Mise à jour du recrutement RH sur une base existante

La page d'offre publique utilise `offre-candidat.css` et `offre-candidat.js` :
descriptif avec retours à la ligne, missions et profil séparés pour les nouvelles
annonces, critères lisibles et formulaire de dépôt distinct. Les pièces PDF sont
vérifiées côté navigateur et serveur. Le bouton indique l'envoi en cours et bloque
les doubles clics. Les offres clôturées, non publiées ou expirées n'affichent pas de
formulaire de candidature. Cette présentation conserve les routes et la protection
CSRF existantes ; elle ne nécessite aucune migration.

### Création d'offre en trois étapes

Le formulaire présente une seule étape à la fois : « Le poste », « L'annonce »,
puis « Vérification et publication ». L'intitulé sert directement de titre à
l'annonce. Choisir un poste du catalogue préremplit les informations disponibles ;
adapter l'annonce ne modifie pas ce poste. Les missions et le profil sont saisis
séparément puis conservés dans la description existante. Les descriptions anciennes
restent intégralement visibles dans les missions lors de la modification.

Les boutons Retour et Modifier conservent la saisie. Les erreurs s'affichent au
niveau des champs, avec focus sur la première erreur. Les boutons se bloquent
pendant l'enregistrement pour éviter les doubles clics. La publication depuis le
formulaire enregistre puis publie dans une seule transaction, avec confirmation.
Les routes, droits RH et protections CSRF sont conservés.

Le modèle actuel ne stocke pas le type de contrat, la durée d'un CDD, le lieu,
le temps de travail, la rémunération, la date de prise de poste ou les avantages.
Ces champs ne sont pas affichés comme fonctionnels. Leur ajout nécessiterait une
évolution du modèle et une migration. Aucune migration n'est requise pour cette
refonte. La visibilité existante est publique après publication, sans paramètre
de diffusion interne distinct.

Les listes RH des candidatures et entretiens utilisent des dates et statuts lisibles.
La feuille `mes-candidatures.css` est chargée sur le suivi candidat, avec une frise
de progression et une indication adaptée à l'étape du dossier.

La fiche RH regroupe les informations clés, les missions, les diplômes, les pièces
et les actions dans des cartes distinctes. Les dates sont affichées au format
jour/mois/année et les statuts sont formulés pour les utilisateurs (« À examiner »,
« Retenue pour entretien »). La liste des offres dispose d'une recherche par titre,
métier ou catégorie et d'un filtre par état. Sur mobile, le bouton Menu conserve
l'accès à la navigation et aux notifications. Un lien d'évitement, des indicateurs
de focus et des en-têtes de tableau facilitent la navigation au clavier.
Cette refonte de l'interface ne nécessite aucune migration supplémentaire.

### Catalogue et dossier complémentaire

Après les migrations `04` et `06`, arrêter l'application et exécuter dans psql :

```sql
\c sirh
\i 'C:/S5/GESTION/Gestion/Projet/gestion_RH/sql/07_catalogue_recrutement.sql'
\i 'C:/S5/GESTION/Gestion/Projet/gestion_RH/sql/08_dossiers_complementaires.sql'
```

`07` ajoute 31 métiers et élargit le référentiel de diplômes à 26 filières
(104 diplômes sur une base initialisée avec `02`). Il est relançable sans doublons.
C'est un catalogue de départ, sans équivalence automatique entre diplômes.
`08` ajoute les pièces demandées par offre et les documents associés à chaque
candidature. Il s'applique une seule fois ; il ne modifie pas les anciens dossiers.
Sur une nouvelle base créée avec le schéma `01` actuel, importer `02` puis `07` :
ne pas appliquer `08`, déjà inclus dans `01`.

Le formulaire comporte un catalogue de métiers, un mode de création libre,
une recherche de diplômes par texte et niveau, et un récapitulatif en direct.
Les missions suggérées pour un métier restent personnalisables.
Pour demander un dossier complémentaire, saisir un libellé par ligne : les pièces
restent entièrement définies par l'employeur. Aucune pièce n'est imposée par défaut.
Maximum : 8 pièces de 100 caractères chacune, un PDF de 5 Mo par pièce.
Les pièces exigées doivent être présentes pour déposer la candidature.
Le candidat et les RH peuvent consulter les PDF liés au dossier ; le remplacement
du CV du profil ne remplace pas les pièces déjà déposées. Si le dépôt échoue,
la transaction annule les documents et nettoie les fichiers copiés pour ce dépôt.
Une erreur de dépôt impose de sélectionner les fichiers à nouveau.

Vérifications : tests unitaires, compilation des JSP, migrations sur bases de test,
parcours Chrome sur ordinateur et mobile, création/publication et dépôt avec pièce.

Un brouillon possède une action « Modifier le brouillon » : titre, missions,
nombre de personnes, expérience, date limite et diplômes peuvent être corrigés
avant publication. Le poste du catalogue reste inchangé. Les critères d'une
offre publiée ou clôturée ne peuvent pas être modifiés par cette action.
Un brouillon dont la date limite est passée doit être corrigé avant publication.
Cette évolution ne nécessite aucune migration supplémentaire.

Pour créer une offre personnalisée sans section interne, appliquer également
`sql/06_postes_personnalises.sql` après `04` sur une base existante. Le mode
« Créer un poste personnalisé » est proposé par défaut : saisir l'intitulé et
les missions suffit avec le nombre de personnes prérempli à 1. La catégorie,
la section, les diplômes et l'expérience sont facultatifs. Le titre de l'annonce
reprend l'intitulé si vous le laissez vide. Le mode « Utiliser un poste existant »
permet de réutiliser le catalogue sans créer un nouveau poste. Les erreurs métier
s'affichent dans le formulaire en conservant la saisie. Une nouvelle installation
avec `01_schema.sql` ne nécessite pas la migration `06`.

Pour reprendre les notifications RH manquantes des candidatures déjà déposées,
exécuter aussi `sql/05_notifications_candidatures_rh.sql` dans psql sur `sirh`.
Ce script peut être relancé sans recréer une alerte existante pour le même dossier
et le même destinataire. Il ne modifie pas les statuts des candidatures.
Les nouveaux dépôts notifient le candidat et chaque compte RH actif.

Arrêter l'application, puis exécuter une fois dans **SQL Shell (psql)** :

```sql
\c sirh
\i 'C:/S5/GESTION/Gestion/Projet/gestion_RH/sql/04_recrutement_rh.sql'
```

Le schéma `01_schema.sql` inclut déjà ces changements pour une nouvelle base :
ne pas appliquer `04` après une nouvelle installation avec `01`.
La migration conserve les statuts existants. Elle reprend les diplômes actuels
pour les anciens dossiers (leur sélection historique n'est pas connue) et ne
leur attribue pas arbitrairement le dernier CV. Si des expériences minimales
négatives existent, la migration s'annule : corriger ces valeurs avant de la relancer.

Les nouveaux dépôts restent « Reçue », quel que soit le score. Les RH décident
de la présélection avec notification au candidat. Le CV et les associations aux
diplômes sont conservés au dépôt ; les libellés du référentiel restent partagés.
Le formulaire d'offre permet de choisir un poste existant ou de créer un poste
avec catégorie libre et section facultatives.

Vérification manuelle : créer une offre avec un nouveau poste, vérifier qu'une
expérience négative est refusée, déposer une candidature, puis tester chacune
des deux décisions RH sur des dossiers distincts et consulter les notifications.

L'administration des diplômes, leur ajout au profil après inscription, les
nouvelles tentatives après refus et le fonctionnement multi-entreprises restent
des évolutions séparées.

## Pourquoi `build.sh` / `build.bat` et pas `ant` directement ?

Si la machine a plusieurs JDK installés et que `JAVA_HOME` ne pointe pas vers un JDK 17, `ant war`
seul peut compiler avec le mauvais JDK. Les scripts `build.sh` / `build.bat` forcent `JAVA_HOME` sur
un JDK 17 **pour la durée de la commande uniquement** (ils ne modifient rien globalement sur la
machine). Si `JAVA_HOME` est déjà un JDK 17 sur votre poste, `ant war` fonctionne aussi directement.

Le chemin du JDK 17 utilisé par les scripts peut être surchargé avec la variable d'environnement
`SIRH_JDK17_HOME` si votre installation n'est pas au chemin par défaut (voir le haut de `build.bat`
/ `build.sh`).

## Cibles Ant disponibles

| Commande | Effet |
|---|---|
| `./build.sh resolve` | Résout les dépendances (Ivy → Maven Central) |
| `./build.sh compile` | Compile les sources Java |
| `./build.sh test` | Exécute les tests unitaires (JUnit 5) |
| `./build.sh war` | Build complet → `build/dist/sirh.war` (cible par défaut) |
| `./build.sh run` | Lance l'appli en Tomcat embarqué, pratique pour tester en local sans déployer sur un vrai Tomcat |
| `./build.sh clean` | Nettoie `build/` |
| `./build.sh clean-all` | `clean` + supprime aussi les dépendances résolues (`lib/`) |

Premier build : nécessite un accès réseau (Ivy télécharge les dépendances depuis Maven Central).
Les builds suivants réutilisent le cache local.

## Comptes de démonstration

Créés automatiquement au premier démarrage (voir `app.demo-data.enabled` dans
`application.properties` — à mettre à `false` en production) :

| Rôle | Email | Mot de passe |
|---|---|---|
| DG | dg@sirh.local | password123 |
| RH | rh@sirh.local | password123 |
| Chef de service | chef-service@sirh.local | password123 |
| Direction | direction@sirh.local | password123 |
| Employé | employe@sirh.local | password123 |

Les candidats s'inscrivent eux-mêmes via `/candidats/inscription`.

## Documentation complète

Voir [docs/INSTALLATION.md](docs/INSTALLATION.md) pour : la liste des prérequis, pourquoi Tomcat
doit être en version 10.1+, la configuration détaillée (`application.properties`), le dossier
d'upload des CV, et une checklist de vérification après déploiement.

## Structure du projet

```
src/main/java/com/sirh/...     Code métier (un package par module : organisation, utilisateur,
                                candidat, recrutement, evaluation, embauche, demande, ordre,
                                notification, commun, config)
src/main/webapp/
  WEB-INF/tags/                 Taglibs JSP réutilisables (ui:list, ui:formulaire, ui:statut...)
  WEB-INF/jsp/                  Vues, une arborescence par espace (public, candidat, rh, dg, ...)
  assets/                       CSS/JS (charte graphique du projet)
sql/                             Scripts SQL (schéma + données de référence)
docs/INSTALLATION.md             Guide d'installation détaillé
build.xml, ivy.xml                Build Ant + dépendances
```
# Dates du parcours des employés

Avant de démarrer cette version sur une base existante, appliquer dans psql :

```sql
\i C:/S5/GESTION/Gestion/Projet/gestion_RH/sql/09_dates_parcours_employes.sql
```

La page RH « Employés et historique » affiche uniquement les comptes EMPLOYE actifs.
Pour les anciennes embauches, renseigner la date réelle d’entrée via « Voir le parcours ».
La date de candidature et la création du compte ne sont pas des dates d’entrée.

# Candidature depuis une offre

Les visiteurs peuvent consulter `/offres` sans compte. Le bouton **Postuler** ouvre le formulaire qui crée le compte et dépose la candidature en un seul envoi. Un candidat déjà inscrit peut se connecter en conservant l'offre choisie.

Voir [le parcours, les règles métier et les fichiers modifiés](docs/PARCOURS_CANDIDATURE.md). Aucune migration SQL supplémentaire n'est nécessaire pour ce parcours.

