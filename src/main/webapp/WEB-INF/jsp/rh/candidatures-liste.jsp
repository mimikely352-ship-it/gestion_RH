<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="ui" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<ui:layout title="Candidatures" activeMenu="candidatures">
  <div class="offre-espace">
    <header class="offre-entete"><div><h1>Candidatures</h1><p class="offre-metier">Consultez les dossiers reçus et préparez votre présélection.</p></div><span class="offre-compteur">${candidatures.size()} dossier(s)</span></header>
    <section class="offre-carte">
      <c:choose><c:when test="${empty candidatures}"><div class="offre-etat-vide"><h2>Aucune candidature reçue</h2><p>Les dossiers apparaîtront ici après le dépôt des candidats sur vos offres publiées.</p><ui:bouton label="Consulter les offres" href="/rh/offres" style="btn-secondaire"/></div></c:when><c:otherwise>
        <div class="champ rh-recherche"><label class="champ-libelle" for="recherche-dossiers">Rechercher un candidat ou une offre</label><input class="saisie" type="search" id="recherche-dossiers" data-recherche-liste="dossiers"/></div>
        <p class="offre-texte-secondaire" data-resultats="dossiers" role="status">${candidatures.size()} dossier(s)</p>
        <div class="offre-table-scroll" role="region" aria-label="Dossiers des candidats" tabindex="0"><table class="offre-table">
          <caption class="interface-visuel-cache">Toutes les candidatures reçues</caption>
          <thead><tr><th scope="col">Candidat</th><th scope="col">Offre</th><th scope="col">Avancement</th><th scope="col">Date de dépôt</th><th scope="col">Score indicatif</th><th scope="col">Dossier</th></tr></thead>
          <tbody><c:forEach var="d" items="${candidatures}"><tr data-liste="dossiers">
            <th scope="row"><c:out value="${d.candidatNomComplet}"/></th><td><c:out value="${d.offreTitre}"/></td><td><ui:statut valeur="${d.statut}"/></td>
            <td><ui:date valeur="${d.dateCandidature}" heure="true"/></td>
            <td><c:choose><c:when test="${d.scoreMatching != null}"><fmt:formatNumber value="${d.scoreMatching}" maxFractionDigits="0"/> / 100</c:when><c:otherwise>Non calculé</c:otherwise></c:choose></td>
            <td><a class="btn btn-secondaire" href="${pageContext.request.contextPath}/rh/candidatures/${d.id}">Ouvrir<span class="interface-visuel-cache"> le dossier de <c:out value="${d.candidatNomComplet}"/></span></a></td>
          </tr></c:forEach></tbody>
        </table></div>
        <p class="offre-precision">Le score aide à lire les dossiers. Il ne remplace pas la décision RH.</p>
      </c:otherwise></c:choose>
    </section>
  </div>
</ui:layout>
