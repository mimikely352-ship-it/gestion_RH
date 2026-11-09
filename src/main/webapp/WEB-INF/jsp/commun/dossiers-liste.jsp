<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="ui" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<ui:layout title="Dossiers salariés" activeMenu="dossiers">
  <c:if test="${aucunDossier}">
    <h1>Mon dossier salarié</h1>
    <ui:alerte type="info">Aucun dossier salarié n’est associé à votre compte. Adressez-vous au service RH.</ui:alerte>
  </c:if>
  <c:if test="${not aucunDossier}">
  <h1>Dossiers salariés</h1>
  <p class="texte-doux">Dossiers que votre habilitation vous permet de consulter : <c:out value="${perimetre}"/>. Chaque consultation est enregistrée dans le journal d’audit.</p>
  <div class="fiche"><div class="fiche-corps">
    <c:if test="${empty dossiers}"><p>Aucun dossier accessible.</p></c:if>
    <c:if test="${not empty dossiers}">
      <div style="overflow-x:auto"><table class="tableau">
        <thead><tr><th scope="col">Matricule</th><th scope="col">Salarié</th><th scope="col">Service</th></tr></thead>
        <tbody><c:forEach var="d" items="${dossiers}"><tr>
          <td><c:out value="${d.matricule}"/></td>
          <td><a href="${pageContext.request.contextPath}/dossiers/${d.id}"><strong><c:out value="${d.nom}"/></strong></a></td>
          <td><c:out value="${d.service}"/></td>
        </tr></c:forEach></tbody>
      </table></div>
    </c:if>
  </div></div>
  </c:if>
</ui:layout>
