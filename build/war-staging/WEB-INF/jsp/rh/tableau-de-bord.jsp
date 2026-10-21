<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="ui" tagdir="/WEB-INF/tags" %>
<ui:layout title="Tableau de bord" activeMenu="tableau-de-bord">
  <h1>Tableau de bord RH</h1>
  <div class="grille-3" style="margin-top:var(--e-6);">
    <div class="fiche"><div class="fiche-corps">
      <h3>Offres d'emploi</h3>
      <p class="texte-doux">Créer, publier et clôturer les offres.</p>
      <a class="btn btn-secondaire btn-petit" href="${pageContext.request.contextPath}/rh/offres">Voir les offres</a>
    </div></div>
    <div class="fiche"><div class="fiche-corps">
      <h3>Candidatures</h3>
      <p class="texte-doux">Entretiens, tests et évaluations.</p>
      <a class="btn btn-secondaire btn-petit" href="${pageContext.request.contextPath}/rh/candidatures">Voir les candidatures</a>
    </div></div>
    <div class="fiche"><div class="fiche-corps">
      <h3>Demandes &amp; ordres</h3>
      <p class="texte-doux">Exécuter les demandes validées par le DG et les ordres directs.</p>
      <a class="btn btn-secondaire btn-petit" href="${pageContext.request.contextPath}/rh/demandes">Voir les demandes</a>
      <a class="btn btn-secondaire btn-petit" href="${pageContext.request.contextPath}/rh/ordres">Voir les ordres</a>
    </div></div>
  </div>
</ui:layout>
