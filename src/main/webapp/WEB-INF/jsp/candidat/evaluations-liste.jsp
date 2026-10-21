<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="ui" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<ui:layout-public title="Mes évaluations">
  <main class="site-conteneur site-main">
    <h1>Évaluations pour <c:out value="${candidature.offreTitre}"/></h1>
    <c:choose>
      <c:when test="${empty evaluations}">
        <div class="site-vide"><p>Aucune évaluation pour l'instant.</p></div>
      </c:when>
      <c:otherwise>
        <div class="candidature-liste" style="margin-top:var(--e-5);">
          <c:forEach var="ev" items="${evaluations}">
            <div class="candidature">
              <div class="candidature__entete">
                <div class="candidature__infos">
                  <h3><c:out value="${ev.titre}"/> (<c:out value="${ev.format}"/>)</h3>
                </div>
                <div class="candidature__actions">
                  <c:choose>
                    <c:when test="${ev.dateEvaluation == null}">
                      <a class="btn btn-principal btn-petit" href="${pageContext.request.contextPath}/candidat/evaluations/${ev.id}/repondre">Répondre</a>
                    </c:when>
                    <c:otherwise><ui:statut valeur="${ev.decisionRh}"/></c:otherwise>
                  </c:choose>
                </div>
              </div>
            </div>
          </c:forEach>
        </div>
      </c:otherwise>
    </c:choose>
  </main>
</ui:layout-public>
