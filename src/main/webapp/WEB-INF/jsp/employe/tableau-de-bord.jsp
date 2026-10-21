<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="ui" tagdir="/WEB-INF/tags" %>
<ui:layout title="Tableau de bord" activeMenu="tableau-de-bord">
  <h1>Tableau de bord — Employé</h1>
  <div class="fiche" style="margin-top:var(--e-6);"><div class="fiche-corps">
    <h3>Mes demandes</h3>
    <p class="texte-doux">Recrutement, promotion, transfert, formation, matériel.</p>
    <a class="btn btn-principal btn-petit" href="${pageContext.request.contextPath}/employe/demandes/nouvelle">Nouvelle demande</a>
    <a class="btn btn-secondaire btn-petit" href="${pageContext.request.contextPath}/employe/demandes">Voir mes demandes</a>
  </div></div>
</ui:layout>
