# SIRH — Gestion RH

Application de gestion RH : recrutement (offres, candidatures, tests QCM, évaluations), demandes
internes avec circuit hiérarchique (Employé → Chef de service → Direction → RH → DG), ordres du DG,
notifications. Vues JSP + API REST.

**Stack :** Java 17 · Spring Boot 3.5 · JSP/JSTL (pas de Thymeleaf) · PostgreSQL · build **Ant** ·
déploiement WAR sur **Tomcat 10.1+ externe**.

## Démarrage rapide (TL;DR)

```bash
# 1. Base de données (une fois)
createdb sirh
psql -U <utilisateur> -d sirh -f sql/01_schema.sql
psql -U <utilisateur> -d sirh -f sql/02_seed.sql

# 2. Config : adapter src/main/resources/application.properties
#    (spring.datasource.username / spring.datasource.password au minimum)

# 3. Build
./build.sh war          # Linux/macOS/Git Bash
build.bat war            # Windows (cmd/PowerShell)

# 4. Déploiement manuel sur Tomcat
# Copier build/dist/sirh.war dans $CATALINA_HOME/webapps/ (le renommer ROOT.war pour servir à la racine)
# puis (re)démarrer Tomcat
```

Application accessible sur `http://<serveur>:8080/` (ou `/sirh/` si le WAR garde son nom).

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
