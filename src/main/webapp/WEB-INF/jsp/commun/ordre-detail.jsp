<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="ui" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<ui:layout title="Détail de l’ordre" activeMenu="ordres">
  <a class="btn btn-secondaire" href="${pageContext.request.contextPath}/${espaceRh ? 'rh' : 'dg'}/ordres">Retour aux ordres</a>
  <h1>Ordre n° <c:out value="${ordre.id}"/></h1>
  <div class="fiche"><div class="fiche-corps">
    <ui:statut valeur="${ordre.statut}"/>
    <h2>Instruction du directeur général</h2>
    <p style="white-space:pre-wrap;overflow-wrap:anywhere"><c:out value="${ordre.description}"/></p>
    <dl>
      <dt>Émis par</dt><dd><c:out value="${ordre.dgEmail}"/></dd>
      <dt>RH destinataire</dt><dd><c:out value="${ordre.rhEmail}"/></dd>
      <dt>Date d’émission</dt><dd><c:out value="${ordre.dateEmissionAffichee}"/></dd>
      <dt>Échéance</dt><dd><c:out value="${ordre.dateLimiteAffichee}"/></dd>
      <dt>Délai d’exécution</dt><dd><c:out value="${ordre.delaiJours}"/> jours</dd>
    </dl>
    <c:if test="${espaceRh}">
      <c:if test="${ordre.statut == 'EMIS'}">
        <ui:formulaire action="/rh/ordres/${ordre.id}/demarrer"><ui:bouton label="Démarrer l’exécution" type="submit" style="btn-principal"/></ui:formulaire>
      </c:if>
      <c:if test="${ordre.statut == 'EN_EXECUTION'}">
        <ui:formulaire action="/rh/ordres/${ordre.id}/terminer"><ui:bouton label="Marquer exécuté" type="submit" style="btn-valider"/></ui:formulaire>
      </c:if>
    </c:if>
  </div></div>
</ui:layout>
