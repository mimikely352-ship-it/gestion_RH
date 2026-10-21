<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="ui" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<ui:layout-public title="Mes tests">
  <main class="site-conteneur site-main">
    <h1>Tests pour <c:out value="${candidature.offreTitre}"/></h1>
    <div class="candidature-liste" style="margin-top:var(--e-5);">
      <c:forEach var="t" items="${tests}">
        <c:set var="passageExistant" value="${null}"/>
        <c:forEach var="p" items="${passages}"><c:if test="${p.test.id == t.id}"><c:set var="passageExistant" value="${p}"/></c:if></c:forEach>
        <div class="candidature">
          <div class="candidature__entete">
            <div class="candidature__infos">
              <h3>Niveau ${t.niveau} — <c:out value="${t.titre}"/></h3>
            </div>
            <div class="candidature__actions">
              <c:choose>
                <c:when test="${passageExistant != null}"><ui:statut valeur="${passageExistant.decisionRh}"/></c:when>
                <c:otherwise>
                  <a class="btn btn-principal btn-petit" href="${pageContext.request.contextPath}/candidat/tests/${t.id}/passer?candidatureId=${candidature.id}">Passer le test</a>
                </c:otherwise>
              </c:choose>
            </div>
          </div>
        </div>
      </c:forEach>
    </div>
  </main>
</ui:layout-public>
