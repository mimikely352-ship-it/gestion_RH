<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="ui" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="sec" uri="http://www.springframework.org/security/tags" %>
<ui:layout-public title="${offre.titre}">
  <main class="site-conteneur site-main" style="max-width:760px;">
    <div class="fiche">
      <div class="fiche-entete">
        <div>
          <h2><c:out value="${offre.titre}"/></h2>
          <p class="texte-doux"><c:out value="${offre.poste.intitule}"/></p>
        </div>
      </div>
      <div class="fiche-corps pile">
        <p><c:out value="${offre.description}"/></p>
        <div class="offre-infos">
          <span><b>${offre.nbPostes}</b> poste(s)</span>
          <c:if test="${not empty offre.dateLimite}"><span>Date limite : <b><c:out value="${offre.dateLimite}"/></b></span></c:if>
          <c:if test="${not empty offre.experienceMinMois}"><span>Expérience souhaitée : <b>${offre.experienceMinMois} mois</b></span></c:if>
        </div>
        <c:if test="${not empty offre.diplomesRequis}">
          <div>
            <h4>Diplômes requis</h4>
            <ul>
              <c:forEach var="d" items="${offre.diplomesRequis}"><li><c:out value="${d.libelle}"/></li></c:forEach>
            </ul>
          </div>
        </c:if>

        <sec:authorize access="hasRole('CANDIDAT')">
          <ui:formulaire action="/candidat/candidatures">
            <input type="hidden" name="offreId" value="${offre.id}"/>
            <ui:bouton label="Postuler à cette offre" type="submit" style="btn-principal"/>
          </ui:formulaire>
        </sec:authorize>
        <sec:authorize access="isAnonymous()">
          <ui:alerte type="info">
            <a href="${pageContext.request.contextPath}/login">Connectez-vous</a> ou
            <a href="${pageContext.request.contextPath}/candidats/inscription">créez un compte candidat</a> pour postuler.
          </ui:alerte>
        </sec:authorize>
        <sec:authorize access="isFullyAuthenticated() and !hasRole('CANDIDAT')">
          <ui:alerte type="alerte">Seuls les comptes candidats peuvent postuler à une offre.</ui:alerte>
        </sec:authorize>
      </div>
    </div>
  </main>
</ui:layout-public>
