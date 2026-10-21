# Installation et déploiement — SIRH Gestion RH

## 1. Prérequis

| Outil | Version | Remarque |
|---|---|---|
| JDK | 17 | Requis pour compiler et exécuter l'application |
| Apache Ant | 1.10+ | Build (`build.xml`) |
| PostgreSQL | 13+ | Base de données |
| Tomcat | **10.1+** | Le packaging utilise la spécification Jakarta EE 10 (namespace `jakarta.*`) via Spring Boot 3 : un Tomcat 9 (namespace `javax.*`) n'est **pas compatible**. |
| Accès réseau | — | Nécessaire au premier build pour que Ivy télécharge les dépendances depuis Maven Central |

Le build ne dépend pas d'une installation préalable d'Ant+Ivy pré-configurée : `tools/ivy-2.5.2.jar` est fourni dans le dépôt, et `build.xml` sait bootstrapper Ivy lui-même (`ant resolve`).

### JAVA_HOME

Si la variable d'environnement `JAVA_HOME` de la machine pointe vers un JDK autre que 17 (fréquent si plusieurs JDK sont installés), utilisez les wrappers fournis au lieu d'appeler `ant` directement :

```bash
# Windows
build.bat war

# Linux/macOS/Git Bash
./build.sh war
```

Ces wrappers forcent `JAVA_HOME` sur un JDK 17 pour la durée de la commande (sans toucher au `JAVA_HOME` global de la machine). Le chemin par défaut peut être surchargé via la variable d'environnement `SIRH_JDK17_HOME`.

## 2. Base de données

Créer la base et l'utilisateur (adapter les identifiants) :

```sql
CREATE DATABASE sirh;
```

Exécuter les scripts dans l'ordre :

```bash
psql -U <utilisateur> -d sirh -f sql/01_schema.sql
psql -U <utilisateur> -d sirh -f sql/02_seed.sql
```

- `01_schema.sql` : création de toutes les tables.
- `02_seed.sql` : référentiel diplômes + organisation de démonstration (direction, départements, services, sections, postes).

Les **comptes de démonstration** (un par rôle) sont créés automatiquement au premier démarrage de l'application par `com.sirh.config.DataInitializer` (pas dans le SQL, car le mot de passe doit être haché par l'encodeur de l'application) :

| Rôle | Email | Mot de passe |
|---|---|---|
| DG | dg@sirh.local | password123 |
| RH | rh@sirh.local | password123 |
| Chef de service | chef-service@sirh.local | password123 |
| Direction | direction@sirh.local | password123 |
| Employé | employe@sirh.local | password123 |

⚠️ Mettre `app.demo-data.enabled=false` dans `application.properties` avant tout déploiement en production.

## 3. Configuration (`src/main/resources/application.properties`)

Variables à adapter selon l'environnement (via le fichier ou des variables d'environnement) :

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/sirh
spring.datasource.username=postgres
spring.datasource.password=postgres

app.jwt.secret=${SIRH_JWT_SECRET:...}   # à définir en prod, 32+ caractères
app.jwt.cookie-secure=true               # à activer en prod (HTTPS)
app.demo-data.enabled=false              # à désactiver en prod
app.upload.dir=/chemin/absolu/vers/uploads/
```

## 4. Build

```bash
./build.sh clean war
```

Produit `build/dist/sirh.war`.

Cibles disponibles (`ant -p` ou voir `build.xml`) :
- `resolve` : résout les dépendances (Ivy → Maven Central).
- `compile` : compile les sources Java.
- `test` : exécute les tests unitaires (JUnit 5 via `<junitlauncher>`).
- `war` : assemble le WAR (cible par défaut).
- `run` : lance l'application en Tomcat embarqué (pratique en développement, **pas destiné à la production**).
- `clean` / `clean-all` : nettoyage (ce dernier supprime aussi les dépendances résolues).

## 5. Déploiement sur Tomcat externe

1. Copier `build/dist/sirh.war` dans `$CATALINA_HOME/webapps/` (le renommer `ROOT.war` pour le servir à la racine, sinon il sera accessible sous `/sirh`).
2. Démarrer/redémarrer Tomcat.
3. Vérifier les logs (`$CATALINA_HOME/logs/catalina.out`) : au premier démarrage, Hibernate valide le schéma (`ddl-auto=validate`) contre les tables créées à l'étape 2, et `DataInitializer` crée les comptes de démonstration si activé.
4. Accéder à `http://<serveur>:8080/` (ou `/sirh/` selon le nom du WAR).

### Pourquoi Tomcat 10.1+ ?

Spring Boot 3 (requis par le cahier des charges pour Java 17) utilise Jakarta EE 10, dont le `jakarta.servlet.*` (ex `javax.servlet.*`) n'est **binairement compatible qu'avec Tomcat 10.1 et plus**. Un déploiement sur Tomcat 9 échouera au chargement (`ClassNotFoundException` sur les classes servlet).

## 6. Upload des CV

Le dossier défini par `app.upload.dir` doit exister et être accessible en écriture par le compte qui exécute Tomcat. Il est créé automatiquement au démarrage s'il n'existe pas (à condition que le processus ait les droits sur le répertoire parent).

## 7. Vérification rapide après déploiement

1. `http://<serveur>/offres` doit afficher la liste des offres publiées (vide au départ, sauf si des offres de démonstration sont créées manuellement).
2. `http://<serveur>/login` avec `rh@sirh.local` / `password123` doit rediriger vers le tableau de bord RH.
3. Créer une offre, la publier, puis s'inscrire comme candidat (`/candidats/inscription`) et postuler pour vérifier le calcul automatique du score de matching.
