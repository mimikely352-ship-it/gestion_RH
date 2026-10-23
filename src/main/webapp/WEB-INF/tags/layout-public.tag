<%@ tag description="Squelette des pages publiques / candidat (en-tête + pied de page), repris de candidat.html (public.css + candidat.css)." pageEncoding="UTF-8" trimDirectiveWhitespaces="true" %>
<%@ attribute name="title" required="true" %>
<%@ attribute name="pageCss" required="false" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="sec" uri="http://www.springframework.org/security/tags" %>
<%@ taglib prefix="ui" tagdir="/WEB-INF/tags" %>
<!DOCTYPE html>
<html lang="fr">
<head>
  <meta charset="UTF-8">
  <meta name="compte-connecte" content="<c:out value='${utilisateurConnecte.email}'/>" data-racine="${pageContext.request.contextPath}">
  <script src="${pageContext.request.contextPath}/assets/js/compte.js" defer></script>
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title><c:out value="${title}"/> — <c:out value="${nomApplication}" default="Oria RH"/></title>
  <link rel="icon" href="${pageContext.request.contextPath}/assets/brand/logo" type="image/jpeg">
  <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/public.css">
  <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/candidat.css">
  <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/extensions.css">
  <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/interface.css">
  <c:if test="${not empty pageCss}"><link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/${pageCss}"></c:if>
  <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/theme.css">
</head>
<body>
<a class="interface-evitement" href="#contenu-candidat">Aller au contenu principal</a>

<header class="site-entete">
  <div class="site-conteneur site-entete__inner">
    <a href="${pageContext.request.contextPath}/" class="site-marque">
      <img class="marque-logo" src="${pageContext.request.contextPath}/assets/brand/logo" alt="" width="46" height="46">
      <strong><c:out value="${nomApplication}" default="Oria RH"/></strong>
    </a>
    <nav class="site-nav" aria-label="Navigation principale">
      <a href="${pageContext.request.contextPath}/offres">Offres d'emploi</a>
      <sec:authorize access="hasAnyRole('EMPLOYE','RH','CHEF_SERVICE','DIRECTION','DG')">
        <a href="${pageContext.request.contextPath}/compte/accueil">Mon espace</a>
      </sec:authorize>
      <sec:authorize access="hasRole('CANDIDAT')">
        <a href="${pageContext.request.contextPath}/candidat/mes-candidatures">Mes candidatures</a>
      </sec:authorize>
    </nav>
    <div class="site-actions">
      <sec:authorize access="hasRole('CANDIDAT')">
        <a class="cloche" title="Notifications" aria-label="Notifications${notificationsNonLues > 0 ? ' : notifications non lues' : ''}" href="${pageContext.request.contextPath}/notifications">
          <ui:icone nom="notification"/>
          <c:if test="${notificationsNonLues > 0}"><span class="cloche-compteur">${notificationsNonLues}</span></c:if>
        </a>
      </sec:authorize>
      <sec:authorize access="!isAnonymous()">
        <ui:formulaire action="/deconnexion" cssClass="rangee"><ui:bouton label="Déconnexion" type="submit" style="btn-secondaire btn-petit"/></ui:formulaire>
      </sec:authorize>
      <sec:authorize access="isAnonymous()">
        <a href="${pageContext.request.contextPath}/login" class="btn btn-secondaire">Se connecter</a>
        <a href="${pageContext.request.contextPath}/offres" class="btn btn-principal">Trouver une offre</a>
      </sec:authorize>
    </div>
  </div>
</header>

<div id="contenu-candidat" tabindex="-1"><jsp:doBody/></div>

<footer class="site-pied">
  <div class="site-conteneur">
    <p class="texte-petit">© 2026 <c:out value="${nomApplication}" default="Oria RH"/> — Les talents font grandir les équipes.</p>
  </div>
</footer>
</body>
</html>
