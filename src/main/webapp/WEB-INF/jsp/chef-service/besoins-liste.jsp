<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="ui" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<ui:layout title="Besoins de personnel" activeMenu="besoins">
  <header class="rangee" style="justify-content:space-between;"><h1>Besoins de personnel</h1><ui:bouton label="Nouveau besoin" href="/besoins/nouveau" style="btn-principal"/></header>
  <c:if test="${empty demandes}"><p class="vide">Aucun besoin enregistré.</p></c:if>
  <div class="pile">
    <c:forEach var="d" items="${demandes}">
      <section class="fiche fiche-corps">
        <ui:statut valeur="${d.statut}"/>
        <ui:besoin demande="${d}"/>
        <p><c:out value="${d.description}"/></p>
      </section>
    </c:forEach>
  </div>
</ui:layout>
