# Oria RH — Installation et démarrage

**Avant de lancer l'application, préparer la base PostgreSQL `sirh` et importer les scripts SQL dans l'ordre indiqué ci-dessous.** La configuration locale utilise `sirh`, pas `sirh_local`.

Le fichier `README.md` personnel est conservé séparément. Les commandes de ce guide utilisent des chemins relatifs au dossier `gestion_RH`.

## 1. Prérequis et emplacement du projet

Installer PostgreSQL 13 ou supérieur (avec `psql`), un JDK 17 et Apache Ant 1.10 ou supérieur. Une connexion Internet est nécessaire au premier build. Tomcat 10.1 externe est nécessaire uniquement pour déployer un WAR.

Configurer `JAVA_HOME` vers votre JDK 17, et ajouter les dossiers `bin` de Java, Ant et PostgreSQL au `PATH`. Ouvrir un nouveau terminal et vérifier :

```text
java -version
javac -version
ant -version
psql --version
```

Ouvrir le terminal dans le dossier contenant `build.xml`, `src` et `sql`. Adapter le chemin à votre ordinateur :

**Windows — cmd :**

```bat
cd /d "C:\chemin\vers\Projet\gestion_RH"
```

**Windows — PowerShell :**

```powershell
cd "C:\chemin\vers\Projet\gestion_RH"
```

**Linux / macOS :**

```bash
cd "/chemin/vers/Projet/gestion_RH"
```

Si le terminal est déjà dans le dossier parent du projet, `cd gestion_RH` suffit. Toutes les commandes suivantes partent de `gestion_RH`. Ne pas lancer l'application pendant les imports ou une remise à zéro.

## 2. Base de données à créer avant tout démarrage

Démarrer le service PostgreSQL. Pour une nouvelle installation, créer la base **une seule fois** :

```text
psql -h localhost -p 5432 -U postgres -d postgres -v ON_ERROR_STOP=1 -c "CREATE DATABASE sirh;"
```

Si `sirh` existe déjà, ne pas relancer sa création. Choisir ensuite **un seul** des parcours A, B ou C. Exécuter les commandes séparément ; en cas d'erreur, corriger avant de continuer.

| Script | Contenu / utilisation |
|---|---|
| `01_schema.sql` | Tables initiales ; uniquement sur une base vide. |
| `02_seed.sql` | Données de référence : diplômes, direction, départements, services, sections et postes. |
| `07_catalogue_recrutement.sql` | Catalogue complémentaire de métiers et diplômes ; relançable. |
| `11_entreprises_offres.sql` | Table des entreprises et lien des offres ; import manuel obligatoire. |
| `12_reset_donnees_test.sql` | Efface les anciennes données et comptes, importe lui-même `11` et `02`, puis crée deux entreprises et trois offres de test. |
| `09` et `10` | Migrations appliquées automatiquement au démarrage local avec la configuration par défaut ; pas de commande supplémentaire dans ces parcours. |

Les modifications `04`, `06` et `08` sont déjà intégrées au schéma `01` actuel : ne pas les importer après une nouvelle installation. `03`, `05` et `rattacher_employe.sql` ne font pas partie du parcours de démarrage d'une base neuve.

### A. Nouvelle installation avec vos propres entreprises

Sur une base `sirh` vide, importer dans cet ordre :

```text
psql -h localhost -p 5432 -U postgres -d sirh -v ON_ERROR_STOP=1 -f sql/01_schema.sql
psql -h localhost -p 5432 -U postgres -d sirh -v ON_ERROR_STOP=1 -f sql/02_seed.sql
psql -h localhost -p 5432 -U postgres -d sirh -v ON_ERROR_STOP=1 -f sql/07_catalogue_recrutement.sql
psql -h localhost -p 5432 -U postgres -d sirh -v ON_ERROR_STOP=1 -f sql/11_entreprises_offres.sql
```

Ouvrir ensuite une session SQL :

```text
psql -h localhost -p 5432 -U postgres -d sirh
```

Dans **psql**, enregistrer votre entreprise en remplaçant le nom d'exemple :

```sql
INSERT INTO entreprise (nom) VALUES ('Nom réel de votre entreprise');
SELECT id, nom FROM entreprise ORDER BY nom;
\q
```

Ce parcours ne crée pas d'offre publiée. Après le démarrage, les RH créent les offres sur le site en choisissant l'entreprise correspondante.

### B. Installation de démonstration ou remise à zéro pour tester

**Le script `12` supprime les données métier, les référentiels et tous les comptes de la base ciblée.** Arrêter l'application et sauvegarder les données à conserver avant de l'utiliser. Il conserve le schéma et le suivi des migrations. Les fichiers physiques sous `uploads` restent présents, mais leurs références en base sont supprimées.

Sur une base vide uniquement, importer d'abord le schéma :

```text
psql -h localhost -p 5432 -U postgres -d sirh -v ON_ERROR_STOP=1 -f sql/01_schema.sql
```

Si le schéma est déjà installé, passer directement à ces commandes :

```text
psql -h localhost -p 5432 -U postgres -d sirh -v ON_ERROR_STOP=1 -f sql/12_reset_donnees_test.sql
psql -h localhost -p 5432 -U postgres -d sirh -v ON_ERROR_STOP=1 -f sql/07_catalogue_recrutement.sql
```

Ne pas importer `02` après `12` : il est déjà inclus. Le script `12` inclut également `11`. L'import de `07` après le reset complète le catalogue.

À la fin du reset, vérifier que les trois offres s'affichent :

| Entreprise | Offre |
|---|---|
| Entreprise Alpha - TEST | Developpeur Java - Alpha |
| Entreprise Alpha - TEST | Chauffeur - Alpha |
| Entreprise Beta - TEST | Developpeur Java - Beta |

Les numéros des offres peuvent varier : se repérer avec leur titre. Les lignes `NOTICE: TRUNCATE cascade` sont normales. Attendre le `COMMIT` et le message « Jeu de test installe : 2 entreprises et 3 offres ».

### C. Base existante dont les données doivent être conservées

Ne pas utiliser `01`, `02` ou `12`. Vérifier les migrations déjà appliquées en suivant le [README existant](README.md), puis importer `11` si nécessaire :

```text
psql -h localhost -p 5432 -U postgres -d sirh -v ON_ERROR_STOP=1 -f sql/11_entreprises_offres.sql
```

Dans psql, créer vos entreprises, consulter leurs identifiants et attribuer chaque ancienne offre à la bonne entreprise :

```sql
INSERT INTO entreprise (nom) VALUES ('Nom réel de votre entreprise');
SELECT id, nom FROM entreprise ORDER BY nom;
SELECT id, titre, entreprise_id FROM offre_emploi ORDER BY id;

-- Exemple : remplacer ces identifiants par ceux de votre base.
UPDATE offre_emploi SET entreprise_id = 1 WHERE id IN (1, 2);

SELECT id, titre FROM offre_emploi WHERE entreprise_id IS NULL;
```

Une entreprise doit avoir un seul enregistrement partagé par ses offres. Une offre sans entreprise ne peut plus être publiée ni recevoir de candidature. Les anciennes candidatures restent conservées et comptent dans la limite d'une candidature par entreprise.

### Si `psql` est introuvable sous Windows

Utiliser le chemin de votre installation PostgreSQL à la place de `psql`. Exemple avec PostgreSQL 18, pour le reset de test uniquement :

**Dans cmd :**

```bat
"C:\PostgreSQL\18\bin\psql.exe" -h localhost -p 5432 -U postgres -d sirh -v ON_ERROR_STOP=1 -f "sql\12_reset_donnees_test.sql"
```

**Dans PowerShell :**

```powershell
& 'C:\PostgreSQL\18\bin\psql.exe' -h localhost -p 5432 -U postgres -d sirh -v ON_ERROR_STOP=1 -f 'sql\12_reset_donnees_test.sql'
```

Adapter le chemin de `psql.exe` à votre installation. Le préfixe `&` s'utilise ici dans PowerShell, pas dans cmd. Les commandes commençant par `\` et les requêtes SQL s'exécutent dans psql ; les commandes `psql ...` s'exécutent dans le terminal du système.

## 3. Vérifier la configuration de l'application

Créer le dossier `config` à la racine de `gestion_RH`, puis le fichier `config/application-local.properties`. Ce fichier personnel est ignoré par Git et n'est pas inclus dans le WAR. Le profil `local` est utilisé par défaut lorsqu'aucun autre profil n'est activé.

Dans ce fichier, adapter les identifiants de PostgreSQL :

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/sirh
spring.datasource.username=postgres
spring.datasource.password=VOTRE_MOT_DE_PASSE_POSTGRESQL
app.demo-data.enabled=true
```

Il est aussi possible de fournir `SIRH_DB_URL`, `SIRH_DB_USER` et `SIRH_DB_PASSWORD` via l'environnement, sans créer ce fichier local. Le dépôt ne contient pas de mot de passe PostgreSQL par défaut. Pour un déploiement WAR, fournir la connexion dans l'environnement du serveur ou dans sa configuration externe ; le fichier local de votre poste n'est pas embarqué.

Le serveur, le port et la base doivent être **les mêmes que ceux utilisés pour les imports**. Le mot de passe PostgreSQL est celui de votre compte de base de données ; il est distinct du mot de passe des comptes RH sur le site.

`app.demo-data.enabled=true` permet de créer les comptes internes de démonstration au démarrage local. Les comptes déjà existants ne voient pas leur mot de passe réinitialisé. Le dossier `app.upload.dir=uploads/` doit être accessible en écriture.

## 4. Démarrer l'application après les imports

Depuis `gestion_RH`, avec le JDK 17 configuré :

```text
ant run
```

Attendre le message `Started Application`, puis ouvrir **http://localhost:8080/login**. Garder le terminal ouvert. `Ctrl+C` arrête l'application. Après un démarrage normal, Ant continue à fonctionner tant que le serveur tourne.

Les scripts `build.bat` et `build.sh` appellent eux aussi Ant, mais contiennent un chemin de JDK par défaut. Pour les utiliser sur votre machine, définir `SIRH_JDK17_HOME` vers votre JDK 17 ; utiliser `build.bat run` dans cmd, `./build.bat run` dans PowerShell ou `bash build.sh run` sous Linux/macOS.

Ne lancer qu'une instance sur le port 8080. Après une remise à zéro, redémarrer l'application pour recréer les comptes de démonstration.

## 5. Comptes de démonstration et test

Le mot de passe commun est exactement `password123` (11 caractères, sans espace ni point-virgule).

| Rôle | Adresse e-mail | Mot de passe |
|---|---|---|
| RH | rh@sirh.local | password123 |
| DG | dg@sirh.local | password123 |
| Direction | direction@sirh.local | password123 |
| Chef de service | chef-service@sirh.local | password123 |
| Employé | employe@sirh.local | password123 |

Avec le parcours B, créer un nouveau candidat depuis une offre sur **http://localhost:8080/offres**, puis vérifier :

1. Candidater à « Developpeur Java - Alpha » : dépôt accepté.
2. Avec le même candidat, candidater à « Chauffeur - Alpha » : dépôt bloqué, car même entreprise.
3. Candidater à « Developpeur Java - Beta » : dépôt accepté, car entreprise différente.
4. Consulter « Mes candidatures » : deux dossiers doivent apparaître.

La limite est d'une candidature par entreprise, tous statuts confondus, y compris après refus. « Dépôt accepté » signifie que le dossier est reçu, pas que le candidat est sélectionné ou embauché. Le compte employé de démonstration ne possède pas automatiquement un historique d'embauche.

## 6. Générer un WAR pour Tomcat

Après la préparation de la base et de la configuration :

```text
ant war
```

Copier `build/dist/sirh.war` dans `webapps` de Tomcat 10.1, puis démarrer Tomcat. L'adresse est **http://localhost:8080/sirh/**. Un déploiement sous le nom `ROOT.war` sert l'application à la racine, selon la configuration du serveur.

Arrêter `ant run` avant de démarrer un Tomcat sur le même port. En production, suivre les consignes du profil `prod` dans le [README existant](README.md) ; les migrations `09` et `10` ne sont pas automatiques avec ce profil et les comptes de démonstration sont désactivés.

## 7. Résoudre les erreurs courantes

| Erreur | Solution |
|---|---|
| `missing table [entreprise]` | Importer `11` ou utiliser le parcours B sur la base réellement configurée. |
| Connexion à `sirh_local` alors que l'application utilise `sirh` | Aligner la base des imports et `spring.datasource.url`. |
| `Port 8080 was already in use` | Arrêter l'instance déjà lancée avant de relancer. |
| `& était inattendu` | Vous êtes dans cmd : utiliser la commande sans `&`, avec des guillemets doubles. |
| `psql` ou `ant` introuvable | Corriger le `PATH` ou utiliser le chemin complet de l'exécutable. |
| `build.xml` ou `sql/...` introuvable | Revenir dans le dossier `gestion_RH`. |
| Base ou table déjà existante | Ne pas réimporter le schéma sur une base déjà initialisée ; choisir le bon parcours. |
| Connexion PostgreSQL refusée | Vérifier le service PostgreSQL, le serveur et le port. |
| Identifiants RH refusés | Vérifier l'e-mail et le mot de passe, puis le démarrage avec les données de démonstration activées. |
| Ancienne interface affichée | Actualiser avec `Ctrl+F5` et vérifier que le serveur utilise le dernier build. |

Pour davantage de détails : [guide d'installation](docs/INSTALLATION.md).


## Logique métier et fonctionnalités

### Correspondance avec les supports de conception

**Règles métier cibles précisées pour le projet :** un employé peut demander une promotion ou un changement de poste (transfert traité directement par les RH). Les demandes de recrutement, de formation et de matériel ne doivent pas être proposées dans son espace. Après une démission enregistrée, son compte doit lui permettre de se reconnecter comme candidat et de constituer un nouveau profil. Les RH examinent ce nouveau profil et les nouvelles candidatures sans reprendre les anciens dossiers dans la sélection. Les anciens dossiers restent archivés pour l'historique, séparément du nouveau cycle de candidature.

**Circuit de démission retenu :** l'employé dépose une demande de démission et les RH la confirment. Le dépôt de la demande seul ne change ni son rôle ni son affectation. Après confirmation RH, son affectation est clôturée et son compte retrouve l'accès candidat. Il constitue alors un nouveau profil destiné à un nouvel examen par les RH, sans reprise des anciens dossiers dans la sélection. La demande de démission est une démarche de départ, distincte des demandes de mobilité (promotion ou transfert).

**État actuel : ces règles ne sont pas encore entièrement implémentées.** Le formulaire employé expose encore tous les types de demandes ; la promotion suit le circuit hiérarchique existant. La demande de démission, sa confirmation RH, le retour au rôle candidat et la séparation des anciens et nouveaux profils restent à développer. Les fonctionnalités décrites ci-dessous correspondent au code existant, et non à la totalité de ces règles cibles.

L'application reprend le parcours **besoin en personnel → recrutement → sélection → embauche**, avec des fonctionnalités supplémentaires. Elle ne couvre pas encore tout le cycle de vie du salarié présenté dans les photos du support.

Cette comparaison repose sur la lecture du code actuel et les éléments lisibles des photos fournies. Elle ne constitue pas une validation de tous les parcours en exécution. Les photos 1 et 2 présentent une architecture possible, les photos 4 et 7 le même bloc recrutement, et les photos 6 et 9 une vue générale du cycle.

| Élément du support | Présence dans l'application et écarts |
|---|---|
| Bloc 1 — Expression du besoin | Demande de type `RECRUTEMENT`, poste cible, motif en description et historique des décisions. Le poste existe indépendamment de son occupant. Le budget, la date souhaitée et la distinction création/remplacement n'ont pas de champs dédiés ; le nombre de postes est renseigné dans l'offre. |
| Circuit Manager → RH → Direction | Circuit différent : auteur → chef de service → Direction → RH → DG, puis exécution par les RH. |
| Bloc 2 — Demande validée transformée en offre | Une offre peut être liée à une demande de recrutement approuvée par le DG. La demande est exécutée à la publication de l'offre. Les RH peuvent aussi créer une offre sans demande préalable. |
| Publication et candidatures | Brouillon, publication, clôture, date limite et plusieurs candidats par offre. Chaque offre est liée à une entreprise. Un candidat choisit un seul poste par entreprise ; il peut candidater dans des entreprises différentes. |
| Présélection, entretiens, tests et décision | Présélection RH, entretiens, tests QCM, évaluations et décision RH conservés. Un score de correspondance aide l'examen ; il ne remplace pas la décision RH. |
| Bloc 3 — Sélection et embauche | Les RH finalisent une candidature retenue : dossier employé, date d'entrée et affectation au poste/service. Il manque une étape distincte de proposition puis d'acceptation par le candidat, ainsi qu'un matricule métier dédié. |
| Distinction candidat / employé | Profil candidat et dossier employé distincts, liés au même compte. Le compte reçoit un rôle interne à l'embauche et le dossier candidat reste conservé. |
| Bloc 4 — Contrat et onboarding | Historique des affectations présent. Les contrats successifs, renouvellements, signatures et checklist d'intégration (badge, compte, matériel, formation, inscription paie) ne sont pas implémentés comme module d'onboarding. |
| Vue générale — Évolution et rupture | Transferts, promotions et demandes de formation présents. La gestion complète des ruptures de contrat et des départs n'est pas implémentée. |
| Architecture applicative | Interface web JSP avec styles adaptatifs, API REST, services Java/Spring, circuits de validation et authentification par rôles. Aucune application mobile native ni moteur de workflow configurable n'est fourni. |
| Données et intégration | PostgreSQL, stockage local des documents, notifications internes et traces métier ciblées : validations, affectations et corrections de date. Une GED complète, un audit généralisé et une solution de reporting/BI ne sont pas fournis. |

### Fonctionnalités supplémentaires du projet

Ces ajouts étendent les blocs détaillés du support et font volontairement partie du périmètre :

- **Mobilité interne** : postes ouverts à la mobilité sans annonce publique obligatoire, transferts et promotions. Les transferts sont décidés directement par les RH ; l'affectation change à la réalisation confirmée, avec une date effective.
- **Parcours employé** : affectations datées, conservation des anciens libellés de poste/service, consultation des formations réalisées et correction tracée de la date d'entrée.
- **Demandes internes** : recrutement, promotion, transfert, formation et matériel, avec statuts, validations, commentaires et exécution. Les demandes de formation et de matériel ne constituent pas des modules complets de formation ou de gestion de stock.
- **Ordres directs du DG aux RH** : destinataire, délai, suivi de l'exécution et notification du DG à la réalisation.
- **Candidature depuis une offre** : inscription et dépôt dans un même parcours, expériences et diplômes, CV PDF, pièces complémentaires définies par offre et suivi de l'avancement par le candidat.
- **Préparation des offres** : catalogue de métiers et diplômes, création de postes personnalisés, formulaire en trois étapes et modification des brouillons avant publication.
- **Aide à la sélection** : score de correspondance candidat/offre, tests QCM et évaluations avec réponses conservées et contrôles de progression.
- **Espaces par rôle et notifications internes** : candidat, employé, chef de service, Direction, RH et DG, avec les actions autorisées pour chaque rôle.

### Éléments restant à développer pour couvrir tout le support

- Champs structurés du besoin : budget, date souhaitée et création/remplacement.
- Proposition d'embauche et acceptation explicite par le candidat, matricule métier.
- Contrats successifs, renouvellements et documents contractuels signés.
- Checklist d'onboarding avec tâches, responsables et suivi de l'intégration.
- Départs/ruptures, intégration paie, GED complète et reporting/BI.

Références dans le code :
[demandes](src/main/java/com/sirh/demande/service/DemandeService.java),
[offres](src/main/java/com/sirh/recrutement/service/OffreEmploiService.java),
[candidatures](src/main/java/com/sirh/recrutement/service/CandidatureService.java), 
[embauche](src/main/java/com/sirh/embauche/service/EmbaucheService.java), 
[affectations](src/main/java/com/sirh/embauche/service/ParcoursEmployeService.java) et 
[ordres du DG](src/main/java/com/sirh/ordre/service/OrdreService.java).

NB : corrigeo amin vscode anareo raha sanatria tsy mandeha le config de demarrage svp!
