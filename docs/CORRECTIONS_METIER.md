# Corrections des parcours et de la sécurité

## Métier

| Constat | Correction | Sources principales |
|---|---|---|
| Embauche sans service impossible après une offre personnalisée | Choix du service d'accueil à l'embauche si le poste n'en possède pas | `EmbaucheService`, `RhCandidatureController`, `rh/candidature-detail.jsp` |
| Transfert subordonné à une offre publique | Disponibilité de mobilité gérée séparément par les RH | `MobiliteController`, `Poste`, `rh/mobilite.jsp` |
| Demandes de mouvement concurrentes | Verrou de compte à la création, vérification d'une demande ouverte, version de demande, référence à l'affectation d'origine | `DemandeService`, `UtilisateurRepository`, `Demande` |
| Poste actuel et historique recalculés | Dossier employé séparé, périodes d'affectation et libellés conservés | `DossierEmploye`, `Affectation`, `ParcoursEmployeService`, `EffectifService` |
| Changement prématuré du poste | Application atomique du mouvement uniquement à la confirmation RH, avec date réelle passée ou du jour | `ParcoursEmployeService`, `RhDemandeController`, `rh/demande-detail.jsp` |
| Perte d'accès au dossier candidat après embauche | Lecture du dossier conservée pour le rôle EMPLOYE ; dépôts externes toujours réservés aux candidats | `CandidatCandidatureController`, `DocumentDownloadController`, `layout.tag` |
| Tests incohérents | Vérification étape, offre, niveau, question/choix, seconde soumission ; versions sur les décisions | `PassageTestService`, `EvaluationService` |
| Formations absentes du parcours | Ajout des formations réalisées et de leur date effective | `EffectifService` |
| Recrutement exécuté à la création d'un brouillon | Exécution de la demande à la publication de l'offre associée | `OffreEmploiService` |
| Dates incohérentes | Validation des expériences, naissance, entretien, date d'entrée et date d'effet ; correction d'entrée historisée | `CandidatInscriptionService`, `EntretienService`, `EffectifService`, `CorrectionDateEntree` |

Le profil employé ne dépend plus du rôle de connexion pour conserver ses informations. La liste RH
reste limitée aux comptes EMPLOYE actifs, conformément au besoin exprimé. Un changement de rôle
ne supprime pas le dossier ni ses affectations.

## Sécurité complémentaire

- Les API utilisent le Bearer, pas le cookie JWT : suppression de l'incohérence CSRF.
- Vérification du compte actif, du rôle courant et de la révocation du jeton à chaque requête.
- Vérification du destinataire des ordres dans le service commun JSP/API ; transitions explicites.
- Vérification du propriétaire d'une notification avant son marquage comme lue.
- Les détails publics et API des offres excluent les brouillons.
- Vérification PDF identique pour les CV et compléments ; nettoyage des CV si la transaction échoue.
- Messages de notification bornés à la taille SQL pour ne pas annuler une opération avec une description longue.
- Profil production distinct, sans secrets par défaut ni génération des comptes de démonstration.
- Gestion séparée des erreurs HTML et JSON ; affichage explicite des conflits de version.

## Migration

`09_dates_parcours_employes.sql` puis `10_fiabilisation_metier.sql`, suivies au démarrage en développement.
Le mode production laisse la migration à l'opérateur. Aucun script ne supprime les employés existants.
Les dates historiques absentes ne sont pas remplacées par une date de candidature ou par aujourd'hui.
Les dossiers ambigus sont conservés pour vérification, sans affectation courante supposée.

## Validation

Dernière validation : **86 tests réussis**, toutes les JSP compilées sans erreur et WAR généré.

- Tests unitaires et contrôleurs, y compris contrôles négatifs des autorisations et des transitions.
- Compilation de toutes les JSP via Jasper.
- PostgreSQL 18 isolé : installation neuve, reprise d'embauches anciennes avec/sans date et réapplication des scripts.
- Démarrage de l'application avec migrations automatiques avant Hibernate.
- Parcours HTTP avec formulaires CSRF réels : inscription, embauche, accès au dossier candidat,
  transfert sans annonce, doublon refusé, décision RH, confirmation datée, affectation unique,
  historique conservé après renommage, API Bearer et révocation après déconnexion.

Les données de vérification sont exclusivement dans les bases locales de test sur le port 55439.
La base `sirh` de l'utilisateur n'a pas été modifiée pendant ces vérifications.
Les vérifications HTTP ne remplacent pas une recette visuelle et d'accessibilité dans le navigateur.

## Limites de déploiement

Cette correction ne constitue pas une certification de production. HTTPS, secrets réellement
déployés, sauvegarde/restauration, antivirus, quotas globaux de stockage, surveillance et politique
de conservation des pièces doivent être validés dans l'environnement d'exploitation.
L'isolation multi-entreprises n'a pas été ajoutée : elle exige un modèle et des droits dédiés.
