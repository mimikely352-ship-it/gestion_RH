<%@ tag description="Squelette des pages publiques / candidat (en-tête + pied de page), repris de candidat.html (public.css + candidat.css)." pageEncoding="UTF-8" trimDirectiveWhitespaces="true" %>
<%@ attribute name="title" required="true" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="sec" uri="http://www.springframework.org/security/tags" %>
<!DOCTYPE html>
<html lang="fr">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title><c:out value="${title}"/> — SIRH</title>
  <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/public.css">
  <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/candidat.css">
  <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/extensions.css">
</head>
<body>

<header class="site-entete">
  <div class="site-conteneur site-entete__inner">
    <a href="${pageContext.request.contextPath}/" class="site-marque">
      <span class="site-marque__logo">S</span>
      <strong>SIRH</strong>
    </a>
    <nav class="site-nav" aria-label="Navigation principale">
      <a href="${pageContext.request.contextPath}/offres">Offres d'emploi</a>
      <sec:authorize access="hasRole('CANDIDAT')">
        <a href="${pageContext.request.contextPath}/candidat/mes-candidatures">Mes candidatures</a>
      </sec:authorize>
    </nav>
    <div class="site-actions">
      <sec:authorize access="hasRole('CANDIDAT')">
        <a class="cloche" title="Notifications" href="${pageContext.request.contextPath}/notifications">
          🔔
          <c:if test="${notificationsNonLues > 0}"><span class="cloche-compteur">${notificationsNonLues}</span></c:if>
        </a>
      </sec:authorize>
      <sec:authorize access="!isAnonymous()">
        <a class="btn btn-secondaire" href="${pageContext.request.contextPath}/deconnexion">Déconnexion</a>
      </sec:authorize>
      <sec:authorize access="isAnonymous()">
        <a href="${pageContext.request.contextPath}/login" class="btn btn-secondaire">Se connecter</a>
        <a href="${pageContext.request.contextPath}/candidats/inscription" class="btn btn-principal">Devenir candidat</a>
      </sec:authorize>
    </div>
  </div>
</header>

<jsp:doBody/>

<footer class="site-pied">
  <div class="site-conteneur">
    <p class="texte-petit">© 2026 SIRH — Gestion RH.</p>
  </div>
</footer>
</body>
</html>
