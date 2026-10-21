<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="ui" tagdir="/WEB-INF/tags" %>
<ui:layout title="Tableau de bord" activeMenu="tableau-de-bord">
  <h1>Tableau de bord — Chef de service</h1>
  <div class="fiche" style="margin-top:var(--e-6);"><div class="fiche-corps">
    <h3>Demandes à valider</h3>
    <p class="texte-doux">Demandes soumises par les employés de votre service.</p>
    <a class="btn btn-secondaire btn-petit" href="${pageContext.request.contextPath}/chef-service/demandes">Voir les demandes</a>
  </div></div>
</ui:layout>
