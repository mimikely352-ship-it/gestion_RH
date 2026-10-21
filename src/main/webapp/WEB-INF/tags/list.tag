<%@ tag description="Tableau générique : <ui:list data='${items}' columns='nom,poste,statut'><ui:action .../></ui:list>. Colonne 'actions' alimentée par le corps de la balise (une ou plusieurs <ui:action/>), exécuté une fois par ligne avec 'row' en scope." pageEncoding="UTF-8" trimDirectiveWhitespaces="true" %>
<%@ attribute name="title" required="false" %>
<%@ attribute name="data" required="true" type="java.util.Collection" %>
<%@ attribute name="columns" required="true" %>
<%@ attribute name="columnLabels" required="false" %>
<%@ attribute name="messageVide" required="false" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<div class="tableau-cadre">
  <c:if test="${not empty title}">
    <div class="tableau-outils"><h3><c:out value="${title}"/></h3></div>
  </c:if>
  <c:choose>
    <c:when test="${empty data}">
      <div class="vide"><p><c:out value="${not empty messageVide ? messageVide : 'Aucune donnée.'}"/></p></div>
    </c:when>
    <c:otherwise>
      <table class="tableau">
        <thead>
          <tr>
            <c:forEach var="col" items="${fn:split(columns, ',')}" varStatus="st">
              <th><c:out value="${not empty columnLabels ? fn:split(columnLabels, ',')[st.index] : col}"/></th>
            </c:forEach>
            <th></th>
          </tr>
        </thead>
        <tbody>
          <c:forEach var="row" items="${data}">
            <tr>
              <c:forEach var="col" items="${fn:split(columns, ',')}">
                <td><c:out value="${row[col]}"/></td>
              </c:forEach>
              <td class="rangee"><jsp:doBody/></td>
            </tr>
          </c:forEach>
        </tbody>
      </table>
    </c:otherwise>
  </c:choose>
</div>
