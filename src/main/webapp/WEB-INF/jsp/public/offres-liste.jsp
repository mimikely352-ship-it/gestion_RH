<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="ui" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<ui:layout-public title="Offres d'emploi">
  <section class="marque-hero" aria-labelledby="accueil-titre">
    <div><span class="marque-surtitre">DES TALENTS, DES ÉQUIPES, UN AVENIR</span>
      <h1 id="accueil-titre">Donnez un nouvel élan à votre avenir.</h1>
      <p>Découvrez les opportunités, présentez votre parcours et suivez chaque étape de votre candidature.</p>
      <a class="btn btn-principal" href="#offres-disponibles">Découvrir les offres</a>
    </div>
    <div class="marque-hero-identite" aria-hidden="true"><img src="${pageContext.request.contextPath}/assets/brand/logo" alt="" width="150" height="150"><strong><c:out value="${nomApplication}" default="Oria RH"/></strong><p>Le lien entre les talents et les équipes.</p></div>
  </section>
  <form class="marque-recherche" action="${pageContext.request.contextPath}/offres" method="get" role="search">
    <div><label for="recherche-publique">Quel poste recherchez-vous ?</label><input type="search" id="recherche-publique" name="q" value="${fn:escapeXml(rechercheOffres)}" placeholder="Métier ou intitulé du poste"></div>
    <button type="submit" class="btn btn-principal">Rechercher</button>
  </form>

  <main class="site-conteneur site-main" id="offres-disponibles" tabindex="-1">
    <div class="site-section-titre">
      <h2>Offres d'emploi</h2>
      <span class="texte-petit texte-doux">${offres.size()} offre(s) disponible(s)</span>
    </div>

    <c:choose>
      <c:when test="${empty offres}">
        <div class="site-vide">
          <h3>${empty rechercheOffres ? 'Aucune offre publiée pour le moment' : 'Aucune offre ne correspond à votre recherche'}</h3>
          <p><c:choose><c:when test="${not empty rechercheOffres}"><a href="${pageContext.request.contextPath}/offres">Afficher toutes les offres</a></c:when><c:otherwise>Revenez un peu plus tard pour découvrir les nouvelles opportunités.</c:otherwise></c:choose></p>
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
                  <p class="texte-petit"><strong><c:out value="${offre.entrepriseNom}"/></strong></p>
                  <p class="texte-petit texte-doux"><c:out value="${offre.poste.intitule}"/></p>
                </div>
              </div>
              <p class="offre-carte__desc"><c:out value="${offre.description}"/></p>
              <div class="offre-carte__tags">
                <span class="tag">${offre.nbPostes} poste(s)</span>
                <c:if test="${not empty offre.dateLimite}">
                  <span class="tag tag--kraft">Jusqu’au <ui:date valeur="${offre.dateLimite}"/></span>
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
