<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="ui" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<ui:layout title="Congés à valider" activeMenu="conges-validation">
  <h1>Congés à valider</h1>
  <c:if test="${not empty succes}"><p role="status"><c:out value="${succes}"/></p></c:if>
  <c:if test="${not empty erreur}"><ui:alerte type="erreur"><c:out value="${erreur}"/></ui:alerte></c:if>
  <ui:conge-validations validations="${validations}"/>
</ui:layout>
