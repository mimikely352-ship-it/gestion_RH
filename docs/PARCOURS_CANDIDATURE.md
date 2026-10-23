# Consulter une offre et postuler

## Parcours utilisateur

1. Ouvrir `/offres` sans se connecter : seules les offres publiées sont accessibles.
2. Ouvrir une offre et cliquer sur **Postuler**.
3. Pour une première candidature, remplir les informations personnelles, choisir un mot de passe, joindre le CV et les pièces demandées.
4. Cliquer sur **Créer mon compte et envoyer ma candidature**. Le compte et la candidature sont enregistrés ensemble ; le candidat est connecté et retrouve son dossier dans **Mes candidatures**.
5. Avec un compte candidat existant, utiliser **Se connecter** : la connexion ramène sur l'offre choisie, où le dossier peut être déposé.
6. Un employé connecté utilise la mobilité interne lorsque le poste l'autorise. Les comptes RH et administratifs ne déposent pas de candidature externe.

## Où écrire les règles métier ?

Les règles doivent rester dans les services Java, même si des contrôles existent dans le formulaire.

| Fichier | Responsabilité |
| --- | --- |
| `src/main/java/com/sirh/candidat/service/InscriptionCandidatureService.java` | Nouvelle transaction : vérifier l'offre, le CV et les pièces, créer le compte puis déposer la candidature. Un échec annule les écritures et déclenche le nettoyage des fichiers via les services existants. |
| `src/main/java/com/sirh/candidat/service/CandidatInscriptionService.java` | Règles existantes du profil : email unique, mot de passe, expériences et diplômes. |
| `src/main/java/com/sirh/candidat/service/DossierCandidatureService.java` | Constitution du dossier et conservation des pièces pour cette candidature. |
| `src/main/java/com/sirh/recrutement/service/CandidatureService.java` | Admissibilité du dépôt et statut initial de la candidature. |

Ne pas placer les règles de statut ou d'autorisation uniquement dans JavaScript ou dans une JSP.

## Fichiers d'interface et navigation modifiés

- `candidat/controller/CandidatureInscriptionController.java` : nouvelle route GET/POST `/offres/{offreId}/postuler`, erreurs du formulaire et orientation selon le compte.
- `utilisateur/controller/LoginController.java` : conserve l'offre choisie pendant la connexion.
- `utilisateur/service/AuthService.java` : connexion du candidat après le dépôt réussi.
- `demande/controller/EmployeDemandeController.java` : présélection du poste pour une demande de transfert.

Ces chemins Java sont relatifs à `src/main/java/com/sirh/`.

Dans `src/main/webapp/WEB-INF/` :

- `jsp/public/inscription.jsp` : formulaire commun, adapté à l'offre et à ses pièces.
- `jsp/public/offre-detail.jsp` : action Postuler et mobilité interne.
- `jsp/commun/login.jsp` : connexion avec retour à l'offre.
- `jsp/candidat/mes-candidatures.jsp` : confirmation du dépôt.
- `tags/layout-public.tag` : accès public aux offres.
- `tags/champ.tag` : échappement des valeurs saisies.
- `tags/champ-liste.tag` : conservation de la sélection multiple après erreur.

## Installation et vérification

Aucune migration SQL n'est nécessaire pour ce changement. La base reste `sirh`.

Dans CMD, arrêter l'application puis compiler et relancer selon le mode habituel :

```bat
cd /d C:\S5\GESTION\Gestion\Projet\gestion_RH
build.bat war
```

Le WAR est `build/dist/sirh.war`. En mode Tomcat externe, remplacer le WAR déployé puis redémarrer Tomcat. En mode embarqué, utiliser `build.bat run` après avoir arrêté l'ancien processus.

Vérifier dans une fenêtre privée : consultation sans compte, premier dépôt avec les pièces, affichage dans Mes candidatures, connexion d'un candidat existant depuis une offre. Vérifier aussi qu'un CV absent, une pièce absente ou une offre expirée empêche le dépôt, sans créer de compte.

Tests ajoutés : `src/test/java/com/sirh/candidat/service/InscriptionCandidatureServiceTest.java` couvre l'offre expirée, la pièce manquante et l'enchaînement inscription/dépôt. La vérification navigateur et une simulation d'échec transactionnel sur une vraie base restent à effectuer pour ce nouveau parcours.
