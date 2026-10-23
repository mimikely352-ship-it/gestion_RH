# Audit de préparation à la production — Oria RH

Date : 23 octobre 2026. Revue du code présent dans le workspace.

Ce document décrit l'état **avant corrections**. Les changements réalisés ensuite,
leur validation et les limites restantes figurent dans [CORRECTIONS_METIER.md](CORRECTIONS_METIER.md).

## Périmètre et limites

Lecture des contrôleurs, services, entités, SQL, configuration et vues principales.
Trois vérifications exécutées en environnement de test avec dépôts simulés, sans modifier
la base réelle. Ce document ne constitue pas un test d'intrusion du serveur ni une
analyse des vulnérabilités publiées des dépendances. Les constats ci-dessous ne sont pas
des corrections : le code applicatif n'a pas été modifié pendant cet audit.

## P0 — Configuration à verrouiller avant exposition

### 1. Paramètres de démonstration dangereux en production

- `src/main/resources/application.properties` : démonstration activée, cookie Secure désactivé,
  connexion PostgreSQL sous `postgres`, mot de passe dans la configuration, secret JWT avec valeur de repli.
- `src/main/java/com/sirh/config/DataInitializer.java` : comptes privilégiés prédéfinis avec un
  mot de passe commun, également écrit dans les logs.
- Impact : si ces réglages sont déployés tels quels, accès privilégiés prévisibles et protection
  insuffisante des secrets. La valeur de repli JWT connue permettrait de fabriquer des jetons si elle est utilisée.
- Correction : profil production sans données de démonstration, secrets obligatoires hors dépôt,
  compte SQL limité, HTTPS et cookie Secure. Désactiver le générateur ne supprime pas les comptes déjà créés.

## P1 — Sécurité et intégrité métier

### 2. API acceptant les cookies sans protection CSRF

- `config/SecurityConfig.java:61` ignore le CSRF pour `/api/**`, mais `config/JwtAuthFilter.java:57`
  accepte aussi le cookie JWT sur ces routes.
- Vérification : POST sur une route API de test derrière la chaîne de sécurité réelle,
  avec cookie seul et sans jeton CSRF : HTTP 200.
- Impact : frontière API/navigateur incohérente. SameSite=Lax réduit certains scénarios
  intersites mais ne remplace pas une séparation explicite des modes d'authentification.
- Correction : API authentifiée exclusivement par en-tête Bearer, ou CSRF exigé dès qu'un cookie authentifie la requête.

### 3. Un RH peut exécuter par API un ordre destiné à un autre RH

- `ordre/controller/OrdreApiController.java:39` passe seulement l'identifiant de l'ordre.
  `ordre/service/OrdreService.java:68` ne vérifie ni l'exécutant ni le destinataire.
- Vérification : la méthode API termine un ordre sans recevoir l'identité de celui qui l'exécute.
- Les protections ajoutées dans les pages JSP ne couvrent donc pas l'API.
- Correction : contrôler les droits sur l'ordre dans le service commun, pour toutes les entrées.

### 4. Une notification appartenant à un autre compte peut être marquée lue

- `notification/service/NotificationService.java:53` charge par identifiant sans vérifier le propriétaire.
  Les contrôleurs JSP et API appellent cette méthode sans transmettre le compte connecté.
- Impact : altération des notifications d'un autre utilisateur ; cela ne démontre pas un accès à leur contenu.
- Correction : mise à jour par identifiant ET destinataire authentifié.

### 5. Les brouillons d'offres sont consultables publiquement par leur identifiant

- `recrutement/controller/OffrePubliqueController.java:40` et `OffreApiController.java:25`
  chargent une offre sans vérifier son statut. Les routes sont publiques.
- Impact : diffusion d'une annonce encore confidentielle. Le bouton Postuler masqué ne protège pas son contenu.
- Correction : recherche publique limitée aux offres effectivement publiées ; accès séparé pour l'aperçu RH.

### 6. Désactivation d'un compte et déconnexion ne révoquent pas son JWT

- `config/JwtAuthFilter.java:68` fait confiance au rôle contenu dans le jeton sans relecture
  du compte actif. `utilisateur/service/AuthService.java:54` supprime seulement le cookie.
- Impact : un jeton conservé reste utilisable jusqu'à expiration, même après désactivation
  du compte ou changement de rôle. L'expiration configurée est de huit heures.
- Correction : sessions révocables ou contrôle serveur de version de jeton/compte actif,
  avec durée et renouvellement définis. Effacer un cookie ne révoque pas une copie du jeton.

### 7. Les résultats des QCM peuvent être faussés

- `evaluation/service/PassageTestService.java:59` charge un choix sans vérifier son appartenance
  à la question ; il incrémente la note sur le seul attribut `estCorrect`.
- Vérification : une réponse d'une autre question marquée correcte obtient 20/20.
- `CandidatTestController` vérifie le propriétaire de la candidature, mais pas le lien test/offre
  ni l'éligibilité de la candidature à passer ce test à cet instant.
- `EvaluationService.java:122` présente aussi le défaut d'appartenance du choix à la question.
- Correction : valider candidature → offre → test → question → choix, statut autorisé et règles de soumission.
  Unicité en base ne remplace pas un message explicite de seconde soumission. Les évaluations doivent aussi
  empêcher l'ajout de réponses après une décision finale.

### 8. Les demandes et ordres ne sont pas protégés contre les décisions concurrentes

- Pas de champ `@Version` ni de verrou applicatif constaté sur `Demande` et `Ordre`.
- Impact : deux RH peuvent accepter/refuser ou exécuter en parallèle, produire plusieurs
  notifications et un historique contradictoire. Plusieurs transferts d'une même personne peuvent se concurrencer.
- `OrdreService` ne vérifie pas non plus les transitions avant de remplacer le statut.
- Correction : contrôle de version ou verrou adapté, transitions explicites, contraintes métier,
  réponses compréhensibles au second utilisateur et opérations idempotentes.

### 9. Une offre personnalisée peut aboutir à une embauche impossible

- `recrutement/service/OffreEmploiService.java:102` autorise un poste sans section.
- `embauche/service/EmbaucheService.java:47` refuse ensuite une embauche sans service.
  Le contrôleur d'embauche fournit `null` comme choix manuel de service.
- Impact : le blocage n'apparaît qu'après le recrutement. Le parcours actuel ne permet pas de
  compléter ce rattachement depuis la modification du brouillon.
- Correction : rendre le service explicite au moment approprié, permettre sa correction et
  signaler les informations nécessaires avant publication ou finalisation.

### 10. Le parcours de l'employé n'est pas un historique durable des affectations

- `embauche/service/EffectifService.java:49` recompose les faits depuis les candidatures,
  les demandes et les libellés actuels des postes.
- Impact : renommer un poste modifie l'affichage du passé ; plusieurs mouvements anciens sans date
  rendent le poste actuel indéterminé. La date d'entrée est modifiable sans historique dédié de correction.
- Correction : dossier employé distinct du rôle de connexion, affectations avec dates de début/fin,
  source du mouvement et trace de l'auteur. Conserver la date d'entrée initiale lors d'un transfert.
  Les dates historiques absentes doivent rester signalées, ou être corrigées avec traçabilité.

## P2 — Fiabilité, exploitation et qualité d'usage

### 11. Le CV d'inscription est moins contrôlé que les pièces complémentaires

- `candidat/service/CandidatInscriptionService.java:94` appelle `enregistrer`, qui contrôle
  principalement l'extension. La vérification de l'en-tête PDF est une méthode séparée, utilisée pour les compléments.
- L'inscription ne prévoit pas le nettoyage des fichiers déjà copiés si la transaction SQL échoue,
  contrairement au dépôt de pièces complémentaires.
- Correction : contrôles homogènes, nettoyage après rollback, quotas, analyse des fichiers adaptée
  au déploiement et stratégie de sauvegarde coordonnée avec la base. Un en-tête PDF ne garantit pas l'innocuité d'un fichier.

### 12. Validation serveur et gestion des erreurs incomplètes

- `candidat/dto/InscriptionCandidatForm.java` n'a pas de contraintes de validation ; le contrôleur
  d'inscription ne déclenche pas de validation structurée.
- Exemples : dates d'expérience inversées, données obligatoires vides, email mal formé,
  délais d'ordre négatifs. Les contrôles HTML peuvent être contournés.
- `commun/exception/GlobalExceptionHandler.java` est un RestControllerAdvice couvrant tout le package :
  certaines erreurs des pages peuvent retourner du JSON. Les erreurs non traitées peuvent retomber sur la page Whitelabel.
- Correction : DTO validés, règles métier serveur, erreurs de formulaire conservant la saisie,
  pages 403/404/500 adaptées, journal technique avec identifiant de suivi.

### 13. Une notification trop longue peut annuler une opération métier

- `sql/01_schema.sql` limite `notification.message` à 255 caractères.
- `OrdreService.emettre` insère la description complète dans la notification ; `NotificationService`
  ne borne pas systématiquement sa longueur. La notification participe à la même transaction.
- Impact : une instruction longue peut faire échouer l'enregistrement de l'ordre.
- Correction : messages courts avec lien vers le dossier ; politique transactionnelle claire,
  et éventuellement envoi différé fiable pour les notifications externes.

### 14. Performance non préparée pour un effectif important

- `EffectifService.lister` charge les utilisateurs puis relance des requêtes de profil,
  candidatures et demandes pour chaque employé.
- Les listes de demandes RH chargent toutes les demandes avant filtrage en Java.
- Impact : volume de données et nombre de requêtes croissants, temps de réponse dégradé.
- Correction : requêtes filtrées en base, projections, pagination, index et mesures sur un jeu de données représentatif.

### 15. Migrations et exploitation à fiabiliser

- Les migrations SQL sont appliquées manuellement. Aucun mécanisme de suivi automatique des versions
  de schéma n'a été trouvé dans l'application. Le blocage `date_entree` observé est un exemple concret.
- Correction : migrations versionnées exécutées une seule fois, validation avant déploiement,
  procédure de reprise et environnement de préproduction.
- À vérifier hors du code : HTTPS effectif, compte SQL réellement utilisé, sauvegardes et restauration
  testées, stockage des fichiers, supervision, rotation des logs et secrets, limitation des tentatives de connexion.
  L'absence de ces mécanismes dans le dépôt ne prouve pas leur absence dans l'infrastructure.

### 16. Confidentialité et périmètre fonctionnel à expliciter

- Les documents sont privés par propriétaire/candidat ou rôle RH, ce qui est un bon point.
  En revanche, tous les RH peuvent actuellement télécharger tous les documents candidats.
- Les pièces demandées sont des libellés libres : aucune restriction métier spécifique pour les
  documents particulièrement sensibles ni politique de conservation n'a été identifiée.
- Correction : politique d'accès, catégories de documents, accès journalisés, conservation et suppression définies.
- Si plusieurs entreprises doivent utiliser la même installation, l'isolation par entreprise n'est
  pas présente dans le modèle courant : ce serait un chantier préalable, pas un filtre visuel.

## Ordre conseillé

1. Fermer les défauts de configuration et d'accès, les brouillons publics et les incohérences API/cookies.
2. Fiabiliser QCM, décisions concurrentes et rattachement organisationnel à l'embauche.
3. Stabiliser le dossier employé, l'historique et les règles de documents.
4. Automatiser les migrations et tester le parcours complet avec PostgreSQL et navigateur.
5. Harmoniser l'interface : libellés métier, dates lisibles, actions et confirmations cohérentes,
   navigation clavier, affichage mobile, pagination et messages d'erreur utiles.

Les tests existants sont utiles mais ne constituent pas une garantie de sécurité ni une validation
du parcours complet en production. Ajouter les cas négatifs des constats ci-dessus est prioritaire.
