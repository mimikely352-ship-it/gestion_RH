<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="ui" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<ui:layout title="Entretiens" activeMenu="entretiens">
  <div class="offre-espace">
    <header class="offre-entete"><div><h1>Entretiens</h1><p class="offre-metier">Retrouvez les rendez-vous et les décisions liées aux candidatures.</p></div><ui:bouton label="Consulter les candidatures" href="/rh/candidatures" style="btn-secondaire"/></header>
    <section class="offre-carte">
      <c:choose><c:when test="${empty entretiens}"><div class="offre-etat-vide"><h2>Aucun entretien planifié</h2><p>Ouvrez un dossier, retenez le candidat pour entretien, puis renseignez la date et le lieu depuis sa candidature.</p><ui:bouton label="Choisir une candidature" href="/rh/candidatures" style="btn-principal"/></div></c:when><c:otherwise>
        <div class="champ rh-recherche"><label class="champ-libelle" for="recherche-entretiens">Rechercher un candidat, une offre ou un lieu</label><input class="saisie" type="search" id="recherche-entretiens" data-recherche-liste="entretiens"/></div>
        <p class="offre-texte-secondaire" data-resultats="entretiens" role="status">${entretiens.size()} entretien(s)</p>
        <div class="offre-table-scroll" role="region" aria-label="Liste des entretiens" tabindex="0"><table class="offre-table">
          <caption class="interface-visuel-cache">Entretiens planifiés et réalisés</caption>
          <thead><tr><th scope="col">Date et heure</th><th scope="col">Candidat</th><th scope="col">Offre</th><th scope="col">Lieu</th><th scope="col">État</th><th scope="col">Décision RH</th><th scope="col">Dossier</th></tr></thead>
          <tbody><c:forEach var="e" items="${entretiens}"><tr data-liste="entretiens">
            <td><ui:date valeur="${e.dateHeure}" heure="true"/></td><th scope="row"><c:out value="${e.candidatNomComplet}"/></th><td><c:out value="${e.offreTitre}"/></td><td><c:out value="${empty e.lieu ? 'À préciser' : e.lieu}"/></td><td><ui:statut valeur="${e.statut}"/></td><td><ui:statut valeur="${e.decisionRh}"/></td><td><a class="btn btn-secondaire" href="${pageContext.request.contextPath}/rh/candidatures/${e.candidatureId}">Ouvrir<span class="interface-visuel-cache"> le dossier de <c:out value="${e.candidatNomComplet}"/></span></a></td>
          </tr></c:forEach></tbody>
        </table></div>
      </c:otherwise></c:choose>
    </section>
  </div>
</ui:layout>
