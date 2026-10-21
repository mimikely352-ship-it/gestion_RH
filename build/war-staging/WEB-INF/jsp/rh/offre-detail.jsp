<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="ui" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<ui:layout title="${offre.titre}" activeMenu="offres">
  <div class="rangee" style="justify-content:space-between;">
    <div>
      <h1><c:out value="${offre.titre}"/></h1>
      <p class="texte-doux"><c:out value="${offre.posteIntitule}"/> — <ui:statut valeur="${offre.statut}"/></p>
    </div>
    <div class="rangee">
      <c:if test="${offre.statut == 'BROUILLON'}">
        <ui:formulaire action="/rh/offres/${offre.id}/publier"><ui:bouton label="Publier" type="submit" style="btn-valider"/></ui:formulaire>
      </c:if>
      <c:if test="${offre.statut == 'PUBLIEE'}">
        <ui:formulaire action="/rh/offres/${offre.id}/cloturer"><ui:bouton label="Clôturer" type="submit" style="btn-refuser"/></ui:formulaire>
      </c:if>
      <ui:bouton label="Gérer les tests" href="/rh/offres/${offre.id}/tests" style="btn-secondaire"/>
    </div>
  </div>

  <p style="margin:var(--e-4) 0;"><c:out value="${offre.description}"/></p>

  <h2 style="margin-top:var(--e-8);">Candidatures (${candidatures.size()})</h2>
  <ui:list data="${candidatures}" columns="candidatNomComplet,scoreMatching,statut,dateCandidature"
           columnLabels="Candidat,Score,Statut,Date" messageVide="Aucune candidature pour cette offre.">
    <ui:action label="Voir" url="/rh/candidatures/{id}"/>
  </ui:list>
</ui:layout>
