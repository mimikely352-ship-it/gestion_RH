<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="ui" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<ui:layout-public title="Offres d'emploi">
  <section class="site-hero">
    <div class="site-conteneur">
      <h1>Le poste qui vous <span>ressemble</span></h1>
      <p>Toutes les offres actuellement ouvertes au recrutement.</p>
    </div>
  </section>

  <main class="site-conteneur site-main">
    <div class="site-section-titre">
      <h2>Offres d'emploi</h2>
      <span class="texte-petit texte-doux">${offres.size()} offre(s) disponible(s)</span>
    </div>

    <c:choose>
      <c:when test="${empty offres}">
        <div class="site-vide">
          <h3>Aucune offre n'est publiée pour le moment</h3>
          <p>Revenez un peu plus tard.</p>
        </div>
      </c:when>
      <c:otherwise>
        <div class="offres-grille">
          <c:forEach var="offre" items="${offres}">
            <article class="offre-carte">
              <div class="offre-carte__top">
                <div class="offre-logo offre-logo--purple">${offre.poste.intitule.substring(0,1)}</div>
                <div>
                  <h3><c:out value="${offre.titre}"/></h3>
                  <p class="texte-petit texte-doux"><c:out value="${offre.poste.intitule}"/></p>
                </div>
              </div>
              <p class="offre-carte__desc"><c:out value="${offre.description}"/></p>
              <div class="offre-carte__tags">
                <span class="tag">${offre.nbPostes} poste(s)</span>
                <c:if test="${not empty offre.dateLimite}">
                  <span class="tag tag--kraft">Avant le <c:out value="${offre.dateLimite}"/></span>
                </c:if>
              </div>
              <div class="offre-carte__pied">
                <a href="${pageContext.request.contextPath}/offres/${offre.id}" class="btn btn-principal btn-petit">Voir l'offre</a>
              </div>
            </article>
          </c:forEach>
        </div>
      </c:otherwise>
    </c:choose>
  </main>
</ui:layout-public>
