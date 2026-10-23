<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="ui" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="sec" uri="http://www.springframework.org/security/tags" %>
<ui:layout-public title="${offre.titre}" pageCss="offre-candidat.css">
  <main class="annonce-conteneur">
    <a class="annonce-retour" href="${pageContext.request.contextPath}/offres">Retour aux offres d’emploi</a>
    <header class="annonce-entete">
      <div><p class="annonce-surtitre">OFFRE D’EMPLOI</p><h1><c:out value="${offre.titre}"/></h1><p class="annonce-metier"><c:out value="${offre.posteIntitule}"/><c:if test="${not empty offre.poste.categorie}"> · <c:out value="${offre.poste.categorie}"/></c:if></p></div>
      <span class="annonce-etat ${candidaturesOuvertes ? 'annonce-etat-ouvert' : ''}">${candidaturesOuvertes ? 'Candidatures ouvertes' : 'Candidatures fermées'}</span>
    </header>
    <dl class="annonce-reperes">
      <div><dt>Entreprise qui recrute</dt><dd><c:out value="${offre.entrepriseNom}"/></dd></div>
      <div><dt>Postes à pourvoir</dt><dd>${offre.nbPostes}</dd></div>
      <div><dt>Expérience souhaitée</dt><dd><c:choose><c:when test="${offre.experienceMinMois > 0}">${offre.experienceMinMois} mois minimum</c:when><c:otherwise>Débutants acceptés</c:otherwise></c:choose></dd></div>
      <div><dt>Date limite</dt><dd><c:choose><c:when test="${not empty offre.dateLimite}"><ui:date valeur="${offre.dateLimite}"/></c:when><c:otherwise>Sans date limite</c:otherwise></c:choose></dd></div>
    </dl>
    <c:if test="${not empty erreur}"><div class="annonce-erreur" role="alert" tabindex="-1" id="erreur-candidature"><c:out value="${erreur}"/></div></c:if>
    <div class="annonce-grille">
      <div class="annonce-contenu">
        <section class="annonce-section" aria-labelledby="missions-titre">
          <h2 id="missions-titre">${empty texteAnnonce.profil ? 'Missions et profil recherché' : 'Missions principales'}</h2>
          <div class="annonce-texte"><c:out value="${texteAnnonce.missions}" default="La description de cette offre n’a pas été précisée."/></div>
        </section>
        <c:if test="${not empty texteAnnonce.profil}"><section class="annonce-section" aria-labelledby="profil-titre"><h2 id="profil-titre">Profil recherché</h2><div class="annonce-texte"><c:out value="${texteAnnonce.profil}"/></div></section></c:if>
        <section class="annonce-section" aria-labelledby="diplomes-titre">
          <h2 id="diplomes-titre">Formation et diplômes</h2>
          <c:choose><c:when test="${empty offre.diplomesRequis}"><p>Aucun diplôme exigé pour cette offre.</p></c:when><c:otherwise>
            <p>Les diplômes suivants sont acceptés comme alternatives :</p>
            <ul class="annonce-diplomes"><c:forEach var="d" items="${offre.diplomesRequis}"><li><c:out value="${d.libelle}"/></li></c:forEach></ul>
          </c:otherwise></c:choose>
          <p class="annonce-note">Chaque dossier est examiné par les RH. Le score de correspondance n’entraîne pas de refus automatique.</p>
        </section>
        <c:if test="${not empty offre.piecesRequises}"><section class="annonce-section" aria-labelledby="pieces-titre"><h2 id="pieces-titre">Documents à préparer</h2><p>Prévoyez un fichier PDF de 5 Mo maximum pour chaque pièce demandée.</p><ul class="annonce-pieces"><c:forEach var="piece" items="${offre.listePiecesRequises}"><li><c:out value="${piece}"/></li></c:forEach></ul></section></c:if>
      </div>
      <aside class="annonce-depot" aria-labelledby="depot-titre">
        <h2 id="depot-titre">Votre candidature</h2>
        <p class="annonce-aide">Un candidat peut choisir un seul poste par entreprise. Une candidature déjà déposée auprès de cette entreprise empêche un nouveau dépôt, même après un refus.</p>
        <c:choose><c:when test="${not candidaturesOuvertes}"><p>Cette offre ne reçoit plus de candidatures ou n’est pas encore publiée.</p><a class="btn btn-secondaire" href="${pageContext.request.contextPath}/offres">Voir les offres ouvertes</a></c:when><c:otherwise>
          <sec:authorize access="hasRole('CANDIDAT')">
            <p>Votre profil sera associé au dossier, ainsi que le CV enregistré sur votre compte, s’il est disponible.</p>
            <ui:formulaire action="/candidat/candidatures" enctype="multipart/form-data" cssClass="annonce-formulaire">
              <input type="hidden" name="offreId" value="${offre.id}"/>
              <c:choose><c:when test="${not empty offre.piecesRequises}">
                <h3>Pièces complémentaires</h3><p class="annonce-aide">Tous les documents ci-dessous sont obligatoires. PDF uniquement, 5 Mo maximum par fichier.</p>
                <c:forEach var="piece" items="${offre.listePiecesRequises}" varStatus="ligne">
                  <div class="annonce-fichier"><label for="piece-${ligne.index}"><c:out value="${piece}"/> <span>(obligatoire)</span></label>
                    <input id="piece-${ligne.index}" name="piece_${ligne.index}" type="file" accept=".pdf,application/pdf" required aria-describedby="erreur-piece-${ligne.index}"/>
                    <span class="annonce-erreur-champ" id="erreur-piece-${ligne.index}"></span>
                  </div>
                </c:forEach>
                <p class="annonce-preparation" id="pieces-preparees" role="status" aria-live="polite">0 / ${offre.piecesRequises.size()} pièce(s) sélectionnée(s)</p>
              </c:when><c:otherwise><p class="annonce-aide">Aucune pièce complémentaire n’est demandée.</p></c:otherwise></c:choose>
              <button class="btn btn-principal" type="submit">Envoyer ma candidature</button>
              <p class="annonce-aide" id="depot-progression" role="status" aria-live="polite">Vous recevrez une notification après le dépôt de votre dossier.</p>
            </ui:formulaire>
          </sec:authorize>
          <sec:authorize access="isAnonymous()"><p>Envoyez votre dossier pour cette offre. Votre compte de suivi sera créé au moment de l’envoi.</p><a class="btn btn-principal" href="${pageContext.request.contextPath}/offres/${offre.id}/postuler">Postuler</a><a class="annonce-lien" href="${pageContext.request.contextPath}/login?offreId=${offre.id}">J’ai déjà un compte</a></sec:authorize>
          <sec:authorize access="hasRole('EMPLOYE')">
            <c:choose><c:when test="${offre.poste.mobiliteOuverte and not empty offre.poste.section}">
              <p>Vous êtes déjà employé. Demandez une mobilité interne vers ce poste ; les RH examineront votre demande.</p>
              <a class="btn btn-principal" href="${pageContext.request.contextPath}/offres/${offre.id}/postuler">Demander une mobilité vers ce poste</a>
            </c:when><c:otherwise><p>Ce poste n’est pas ouvert à la mobilité interne.</p><a class="btn btn-secondaire" href="${pageContext.request.contextPath}/compte/accueil">Mon espace employé</a></c:otherwise></c:choose>
          </sec:authorize>
          <sec:authorize access="isFullyAuthenticated() and !hasAnyRole('CANDIDAT','EMPLOYE')"><p>Vous consultez cette offre avec un compte interne. Pour déposer un dossier externe, utilisez un compte candidat.</p><a class="btn btn-secondaire" href="${pageContext.request.contextPath}/compte/accueil">Mon espace</a></sec:authorize>
        </c:otherwise></c:choose>
        <div class="annonce-suite"><h3>Après le dépôt</h3><p>Retrouvez votre dossier dans « Mes candidatures ». Les RH vous informeront de la suite du recrutement.</p></div>
      </aside>
    </div>
  </main>
  <script src="${pageContext.request.contextPath}/assets/js/offre-candidat.js" defer></script>
</ui:layout-public>
