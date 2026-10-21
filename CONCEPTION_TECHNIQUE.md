# 04 - Conception technique (Spring Boot + PostgreSQL + REST API)

## 1. Stack
- Java (Ze tintsika na 8 na 21 mety daholo), **Spring Boot**
- Spring Web (API REST), Spring Data JPA, Spring Validation
- Spring Security + **JWT** (authentification par rôle)
- **PostgreSQL**
- Lombok, springdoc-openapi (Swagger UI pour tester l'API)

## 2. Architecture en couches
```
Controller (REST, DTO)  →  Service (règles métier, @Transactional)  →  Repository (JPA)  →  PostgreSQL
```
- Les **entités** ne sortent jamais du back : on expose des **DTO**.
- Les **règles de gestion (RG)** sont dans les services, pas dans les controllers.
- Un `@RestControllerAdvice` centralise les erreurs (400, 403, 404, 409).

## 3. Structure des packages
```
com.projet.sirh
├── config/          # SecurityConfig, JwtFilter, CorsConfig
├── organisation/    # Direction, Departement, Service, Section, Poste
├── auth/            # Utilisateur, AuthController (login, register)
├── besoin/          # DemandeRecrutement, Validation
├── recrutement/     # OffreEmploi, Candidat, Candidature, Entretien, Evaluation
├── embauche/        # PropositionEmbauche, Employe
├── contrat/         # Contrat, Affectation
├── onboarding/      # Onboarding, OnboardingTache
├── commun/          # Document, Notification, exceptions, enums
```
Chaque module contient : `entity/`, `dto/`, `repository/`, `service/`, `controller/`.

## 4. Endpoints REST principaux
| Méthode | URL | Rôle | Description |
|---|---|---|---|
| POST | `/api/auth/register` | public | Inscription candidat |
| POST | `/api/auth/login` | public | Retourne un JWT |
| GET | `/api/organisation/directions` (…/departements, /services, /sections, /postes) | public | Filtres en cascade |
| POST/PUT/DELETE | `/api/organisation/**` | ADMIN | Gestion organisation |
| POST | `/api/demandes` | MANAGER | Créer une demande |
| PUT | `/api/demandes/{id}/soumettre` | MANAGER | Soumettre |
| PUT | `/api/demandes/{id}/valider` | RH, DIRECTION | Valider / rejeter (body : décision, commentaire) |
| GET | `/api/offres?departementId=&posteId=` | public | Offres publiées filtrées |
| GET | `/api/offres/{id}` | public | Détail offre |
| POST | `/api/offres` | RH | Créer offre depuis demande |
| PUT | `/api/offres/{id}/publier` | RH | Publier |
| POST | `/api/offres/{id}/candidatures` | CANDIDAT | Postuler (multipart avec CV) |
| GET | `/api/candidats/moi/candidatures` | CANDIDAT | Mes candidatures |
| GET | `/api/offres/{id}/candidatures` | RH | Candidatures d'une offre |
| PUT | `/api/candidatures/{id}/statut` | RH | Changer statut |
| POST | `/api/candidatures/{id}/entretiens` | RH | Planifier entretien/test |
| POST | `/api/entretiens/{id}/evaluations` | RH, MANAGER | Évaluer |
| POST | `/api/candidatures/{id}/proposition` | RH | Proposer l'embauche |
| PUT | `/api/propositions/{id}/repondre` | CANDIDAT | Accepter / refuser |
| GET | `/api/employes` · `/api/employes/{id}` | RH | Liste / fiche |
| POST | `/api/employes/{id}/contrats` | RH | Nouveau contrat / renouvellement |
| GET | `/api/onboardings/{id}` | RH, EMPLOYE | Checklist |
| PUT | `/api/onboarding-taches/{id}/faite` | RH, MANAGER, ADMIN | Cocher une tâche |
| GET | `/api/notifications` · PUT `/api/notifications/{id}/lu` | connecté | Notifications |

## 5. Traitement clé : acceptation d'une proposition (RG12)
```java
@Transactional
public EmployeDto accepterProposition(Long propositionId) {
    PropositionEmbauche p = propositionRepo.findById(propositionId).orElseThrow();
    if (p.getStatut() != StatutProposition.ENVOYEE) throw new BusinessException("Déjà traitée");
    p.setStatut(StatutProposition.ACCEPTEE);

    Candidat c = p.getCandidature().getCandidat();
    Employe e = employeRepo.save(new Employe(genererMatricule(), c, p.getDateDebut()));
    contratRepo.save(Contrat.depuis(p, e));
    affectationRepo.save(new Affectation(e, posteDe(p), p.getDateDebut()));
    onboardingService.creerAvecTachesParDefaut(e);   // 7 tâches (badge, paie, compte bancaire…)
    notificationService.notifierRH("Nouvel employé : " + e.getMatricule());
    return mapper.toDto(e);
}
```
Si une étape échoue, tout est annulé (rollback).

**Matricule :** `EMP-` + année + compteur sur 4 chiffres (ex. via une séquence PostgreSQL `CREATE SEQUENCE matricule_seq;`).

## 6. Sécurité
- Mots de passe hashés avec **BCrypt**.
- JWT contenant l'id et le rôle ; contrôle par `@PreAuthorize("hasRole('RH')")`.
- Un candidat ne voit que **ses** candidatures ; un manager que les demandes de **son** département.

## 7. Configuration
```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/sirh
spring.datasource.username=postgres
spring.datasource.password=postgres
spring.jpa.hibernate.ddl-auto=validate
spring.jpa.show-sql=true
spring.servlet.multipart.max-file-size=5MB
app.upload.dir=uploads/
```
> Exécuter le script de `02_BASE_DE_DONNEES.md` (ou utiliser Flyway), puis `ddl-auto=validate`.

## 8. Planning MVP (ordre de réalisation)
1. **Sprint 1 :** organisation + authentification JWT + inscription candidat
2. **Sprint 2 :** demandes de recrutement + workflow de validation
3. **Sprint 3 :** offres + candidatures + filtres page d'accueil
4. **Sprint 4 :** entretiens, évaluations, proposition d'embauche
5. **Sprint 5 :** création automatique employé/contrat/affectation + onboarding + notifications
