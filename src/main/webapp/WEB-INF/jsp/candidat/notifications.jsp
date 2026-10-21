<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="ui" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<ui:layout-public title="Notifications">
  <main class="site-conteneur site-main">
    <h1>Notifications</h1>
    <c:choose>
      <c:when test="${empty notifications}"><div class="site-vide"><p>Aucune notification.</p></div></c:when>
      <c:otherwise>
        <ul class="notif-liste" style="margin-top:var(--e-5);">
          <c:forEach var="n" items="${notifications}">
            <li class="notif-item ${n.lu ? '' : 'non-lue'}">
              <div class="rangee" style="justify-content:space-between;">
                <div>
                  <a href="${pageContext.request.contextPath}${n.lien}"><c:out value="${n.message}"/></a>
                  <div class="texte-mini"><c:out value="${n.dateCreation}"/></div>
                </div>
                <c:if test="${!n.lu}">
                  <ui:formulaire action="/notifications/${n.id}/lu"><ui:bouton label="Marquer lue" type="submit" style="btn-discret btn-petit"/></ui:formulaire>
                </c:if>
              </div>
            </li>
          </c:forEach>
        </ul>
      </c:otherwise>
    </c:choose>
  </main>
</ui:layout-public>
