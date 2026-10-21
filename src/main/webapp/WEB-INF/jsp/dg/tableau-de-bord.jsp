<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="ui" tagdir="/WEB-INF/tags" %>
<ui:layout title="Tableau de bord" activeMenu="tableau-de-bord">
  <h1>Tableau de bord — Direction Générale</h1>
  <div class="grille-3" style="margin-top:var(--e-6);">
    <div class="fiche"><div class="fiche-corps">
      <h3>Demandes à valider</h3>
      <p class="texte-doux">Décision finale sur les demandes internes.</p>
      <a class="btn btn-secondaire btn-petit" href="${pageContext.request.contextPath}/dg/demandes">Voir les demandes</a>
    </div></div>
    <div class="fiche"><div class="fiche-corps">
      <h3>Ordres</h3>
      <p class="texte-doux">Émettre un ordre direct au RH.</p>
      <a class="btn btn-secondaire btn-petit" href="${pageContext.request.contextPath}/dg/ordres">Voir mes ordres</a>
    </div></div>
  </div>
</ui:layout>
