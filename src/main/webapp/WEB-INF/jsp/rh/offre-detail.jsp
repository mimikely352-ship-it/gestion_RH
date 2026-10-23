<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="ui" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<ui:layout title="${offre.titre}" activeMenu="offres">
  <div class="offre-espace">
    <a class="offre-retour" href="${pageContext.request.contextPath}/rh/offres"><ui:icone nom="retour"/> Toutes les offres</a>
    <c:if test="${not empty erreur}"><p class="offre-message offre-message-erreur" role="alert"><c:out value="${erreur}"/></p></c:if>
    <c:if test="${not empty succes}"><p class="offre-message" role="status"><c:out value="${succes}"/></p></c:if>
    <header class="offre-entete">
      <div><p class="offre-surtitre">FICHE DE RECRUTEMENT</p>
        <h1><c:out value="${offre.titre}"/></h1>
        <p class="offre-metier"><c:out value="${offre.posteIntitule}"/><c:if test="${not empty offre.poste.categorie}"><span aria-hidden="true"> · </span><c:out value="${offre.poste.categorie}"/></c:if></p>
      </div>
      <ui:statut valeur="${offre.statut}"/>
    </header>
    <dl class="offre-indicateurs">
      <div><dt>Entreprise</dt><dd><c:out value="${offre.entrepriseNom}"/></dd></div>
      <div><dt>Personnes à recruter</dt><dd><c:out value="${offre.nbPostes}"/></dd></div>
      <div><dt>Expérience minimale</dt><dd><c:choose><c:when test="${offre.experienceMinMois > 0}">${offre.experienceMinMois} <span>mois</span></c:when><c:otherwise>Débutants acceptés</c:otherwise></c:choose></dd></div>
      <div><dt>Date limite</dt><dd><c:choose><c:when test="${not empty offre.dateLimite}"><fmt:parseDate value="${offre.dateLimite}" pattern="yyyy-MM-dd" var="dateLimite"/><time datetime="${offre.dateLimite}"><fmt:formatDate value="${dateLimite}" pattern="dd/MM/yyyy"/></time></c:when><c:otherwise>Sans date limite</c:otherwise></c:choose></dd></div>
      <div><dt>Candidatures reçues</dt><dd><a href="#candidatures">${candidatures.size()} <span>voir les dossiers <ui:icone nom="bas"/></span></a></dd></div>
    </dl>
    <div class="offre-disposition">
      <div class="offre-contenu">
        <section class="offre-carte" aria-labelledby="missions-titre">
          <div class="offre-carte-titre"><span class="offre-repere" aria-hidden="true">01</span><h2 id="missions-titre">Missions et profil recherché</h2></div>
          <div class="offre-description"><c:out value="${offre.description}" default="Aucune description renseignée."/></div>
        </section>
        <section class="offre-carte" aria-labelledby="diplomes-titre">
          <div class="offre-carte-titre"><span class="offre-repere" aria-hidden="true">02</span><h2 id="diplomes-titre">Formation et diplômes</h2></div>
          <c:choose><c:when test="${empty offre.diplomesRequis}"><p class="offre-texte-secondaire">Aucun diplôme exigé pour cette offre.</p></c:when><c:otherwise>
            <p class="offre-texte-secondaire">Les diplômes suivants sont acceptés comme alternatives.</p>
            <ul class="offre-liste-criteres"><c:forEach var="d" items="${offre.diplomesRequis}"><li><span class="offre-puce"><ui:icone nom="valide"/></span><c:out value="${d.libelle}"/></li></c:forEach></ul>
          </c:otherwise></c:choose>
          <p class="offre-precision">Le score de correspondance est indicatif. La présélection est décidée par les RH.</p>
        </section>
        <section class="offre-carte" aria-labelledby="pieces-titre">
          <div class="offre-carte-titre"><span class="offre-repere" aria-hidden="true">03</span><h2 id="pieces-titre">Pièces du dossier complémentaire</h2></div>
          <c:choose><c:when test="${empty offre.piecesRequises}"><p class="offre-texte-secondaire">Aucune pièce complémentaire demandée.</p></c:when><c:otherwise>
            <p class="offre-texte-secondaire">À joindre par le candidat au moment du dépôt.</p>
            <ul class="offre-liste-criteres"><c:forEach var="piece" items="${offre.listePiecesRequises}"><li><span class="offre-format" aria-hidden="true">PDF</span><c:out value="${piece}"/></li></c:forEach></ul>
            <p class="offre-precision">Un fichier PDF par pièce · 5 Mo maximum par fichier.</p>
          </c:otherwise></c:choose>
        </section>
      </div>
      <aside class="offre-actions offre-carte" aria-labelledby="actions-titre">
        <p class="offre-surtitre">GESTION DE L’OFFRE</p>
        <h2 id="actions-titre"><c:choose><c:when test="${offre.statut == 'BROUILLON'}">Préparer la publication</c:when><c:when test="${offre.statut == 'PUBLIEE'}">Suivre le recrutement</c:when><c:otherwise>Recrutement clôturé</c:otherwise></c:choose></h2>
        <c:choose>
          <c:when test="${offre.statut == 'BROUILLON'}">
            <p>Cette offre n’est pas visible par les candidats. Vérifiez les missions et les critères avant de la publier.</p>
            <ui:formulaire action="/rh/offres/${offre.id}/publier"><ui:bouton label="Publier l’offre" type="submit" style="btn-principal"/></ui:formulaire>
            <ui:bouton label="Modifier le brouillon" href="/rh/offres/${offre.id}/modifier" style="btn-secondaire"/>
          </c:when>
          <c:when test="${offre.statut == 'PUBLIEE'}">
            <p>L’offre est publiée. Consultez les dossiers reçus pour préparer votre présélection.</p>
            <a class="btn btn-principal" href="#candidatures">Examiner les candidatures</a>
            <ui:bouton label="Voir l’annonce publique" href="/offres/${offre.id}" style="btn-secondaire"/>
          </c:when>
          <c:otherwise><p>Les nouveaux dépôts sont fermés. Les candidatures reçues restent consultables.</p><a class="btn btn-principal" href="#candidatures">Consulter les dossiers</a></c:otherwise>
        </c:choose>
        <div class="offre-actions-secondaires"><h3>Évaluations</h3><p>Préparez les tests utilisés pendant le recrutement.</p><ui:bouton label="Configurer les tests" href="/rh/offres/${offre.id}/tests" style="btn-secondaire"/></div>
        <c:if test="${offre.statut == 'PUBLIEE'}">
          <div class="offre-actions-secondaires"><h3>Terminer la réception</h3><p>Clôturer l’offre bloque les nouvelles candidatures et conserve les dossiers existants.</p>
            <ui:formulaire action="/rh/offres/${offre.id}/cloturer"><ui:bouton label="Clôturer l’offre" type="submit" style="btn-secondaire"/></ui:formulaire>
          </div>
        </c:if>
      </aside>
    </div>
    <section id="candidatures" class="offre-carte offre-candidatures" aria-labelledby="candidatures-titre" tabindex="-1">
      <div class="offre-section-entete"><div><h2 id="candidatures-titre">Candidatures <span class="offre-compteur">${candidatures.size()}</span></h2><p class="offre-texte-secondaire">Ouvrez un dossier pour consulter les pièces et prendre une décision.</p></div></div>
      <c:choose><c:when test="${empty candidatures}">
        <div class="offre-etat-vide"><span class="offre-etat-icone"><ui:icone nom="dossier"/></span><h3>Aucune candidature pour le moment</h3>
          <p><c:choose><c:when test="${offre.statut == 'BROUILLON'}">Publiez l’offre pour permettre aux candidats de déposer leur dossier.</c:when><c:when test="${offre.statut == 'PUBLIEE'}">Les nouveaux dossiers apparaîtront ici dès leur dépôt.</c:when><c:otherwise>Aucun dossier n’a été reçu pour cette offre.</c:otherwise></c:choose></p>
        </div>
      </c:when><c:otherwise>
        <div class="offre-table-scroll" role="region" aria-label="Liste des candidatures" tabindex="0">
          <table class="offre-table"><caption class="interface-visuel-cache">Candidatures pour <c:out value="${offre.titre}"/></caption>
            <thead><tr><th scope="col">Candidat</th><th scope="col">Date de dépôt</th><th scope="col">Score indicatif</th><th scope="col">Avancement</th><th scope="col"><span class="interface-visuel-cache">Dossier</span></th></tr></thead>
            <tbody><c:forEach var="dossier" items="${candidatures}"><tr>
              <th scope="row"><c:out value="${dossier.candidatNomComplet}"/></th>
              <td><fmt:parseDate value="${fn:substring(dossier.dateCandidature,0,10)}" pattern="yyyy-MM-dd" var="dateDepot"/><time datetime="${dossier.dateCandidature}"><fmt:formatDate value="${dateDepot}" pattern="dd/MM/yyyy"/></time></td>
              <td><c:choose><c:when test="${dossier.scoreMatching != null}"><fmt:formatNumber value="${dossier.scoreMatching}" maxFractionDigits="0"/> / 100</c:when><c:otherwise>Non calculé</c:otherwise></c:choose></td>
              <td><ui:statut valeur="${dossier.statut}"/></td>
              <td><a class="btn btn-secondaire" href="${pageContext.request.contextPath}/rh/candidatures/${dossier.id}">Ouvrir le dossier<span class="interface-visuel-cache"> de <c:out value="${dossier.candidatNomComplet}"/></span></a></td>
            </tr></c:forEach></tbody>
          </table>
        </div>
      </c:otherwise></c:choose>
    </section>
  </div>
</ui:layout>
