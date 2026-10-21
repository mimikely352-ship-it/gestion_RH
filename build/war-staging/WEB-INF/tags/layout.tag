<%@ tag description="Squelette commun des pages internes (RH, DG, Direction, Chef de service, Employé) : barre latérale + en-tête + contenu. Reprend la structure .app/.lateral/.principal de la charte graphique (admin.css)." pageEncoding="UTF-8" trimDirectiveWhitespaces="true" %>
<%@ attribute name="title" required="true" %>
<%@ attribute name="activeMenu" required="false" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="sec" uri="http://www.springframework.org/security/tags" %>
<!DOCTYPE html>
<html lang="fr">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title><c:out value="${title}"/> — SIRH Gestion RH</title>
  <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/admin.css">
  <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/extensions.css">
</head>
<body>
<div class="app">

  <aside class="lateral">
    <a href="${pageContext.request.contextPath}/" class="lateral-marque">
      <span class="lateral-logo">RH</span>
      <strong>SIRH</strong>
    </a>

    <nav class="lateral-groupe" aria-label="Pilotage">
      <span class="lateral-titre">Pilotage</span>
      <sec:authorize access="hasRole('RH')">
        <a class="lateral-lien ${activeMenu == 'tableau-de-bord' ? 'actif' : ''}" href="${pageContext.request.contextPath}/rh/tableau-de-bord">Tableau de bord</a>
      </sec:authorize>
      <sec:authorize access="hasRole('DG')">
        <a class="lateral-lien ${activeMenu == 'tableau-de-bord' ? 'actif' : ''}" href="${pageContext.request.contextPath}/dg/tableau-de-bord">Tableau de bord</a>
      </sec:authorize>
      <sec:authorize access="hasRole('DIRECTION')">
        <a class="lateral-lien ${activeMenu == 'tableau-de-bord' ? 'actif' : ''}" href="${pageContext.request.contextPath}/direction/tableau-de-bord">Tableau de bord</a>
      </sec:authorize>
      <sec:authorize access="hasRole('CHEF_SERVICE')">
        <a class="lateral-lien ${activeMenu == 'tableau-de-bord' ? 'actif' : ''}" href="${pageContext.request.contextPath}/chef-service/tableau-de-bord">Tableau de bord</a>
      </sec:authorize>
      <sec:authorize access="hasRole('EMPLOYE')">
        <a class="lateral-lien ${activeMenu == 'tableau-de-bord' ? 'actif' : ''}" href="${pageContext.request.contextPath}/employe/tableau-de-bord">Tableau de bord</a>
      </sec:authorize>
    </nav>

    <sec:authorize access="hasRole('RH')">
      <nav class="lateral-groupe" aria-label="Recrutement">
        <span class="lateral-titre">Recrutement</span>
        <a class="lateral-lien ${activeMenu == 'offres' ? 'actif' : ''}" href="${pageContext.request.contextPath}/rh/offres">Offres d'emploi</a>
        <a class="lateral-lien ${activeMenu == 'candidatures' ? 'actif' : ''}" href="${pageContext.request.contextPath}/rh/candidatures">Candidatures</a>
        <a class="lateral-lien ${activeMenu == 'entretiens' ? 'actif' : ''}" href="${pageContext.request.contextPath}/rh/entretiens">Entretiens</a>
      </nav>
    </sec:authorize>

    <sec:authorize access="hasAnyRole('EMPLOYE','CHEF_SERVICE','DIRECTION','RH','DG')">
      <nav class="lateral-groupe" aria-label="Demandes">
        <span class="lateral-titre">Demandes internes</span>
        <sec:authorize access="hasRole('EMPLOYE')">
          <a class="lateral-lien ${activeMenu == 'mes-demandes' ? 'actif' : ''}" href="${pageContext.request.contextPath}/employe/demandes">Mes demandes</a>
        </sec:authorize>
        <sec:authorize access="hasRole('CHEF_SERVICE')">
          <a class="lateral-lien ${activeMenu == 'demandes' ? 'actif' : ''}" href="${pageContext.request.contextPath}/chef-service/demandes">Demandes à valider</a>
        </sec:authorize>
        <sec:authorize access="hasRole('DIRECTION')">
          <a class="lateral-lien ${activeMenu == 'demandes' ? 'actif' : ''}" href="${pageContext.request.contextPath}/direction/demandes">Demandes à valider</a>
        </sec:authorize>
        <sec:authorize access="hasRole('RH')">
          <a class="lateral-lien ${activeMenu == 'demandes' ? 'actif' : ''}" href="${pageContext.request.contextPath}/rh/demandes">Demandes à valider / exécuter</a>
          <a class="lateral-lien ${activeMenu == 'ordres' ? 'actif' : ''}" href="${pageContext.request.contextPath}/rh/ordres">Ordres du DG</a>
        </sec:authorize>
        <sec:authorize access="hasRole('DG')">
          <a class="lateral-lien ${activeMenu == 'demandes' ? 'actif' : ''}" href="${pageContext.request.contextPath}/dg/demandes">Demandes à valider</a>
          <a class="lateral-lien ${activeMenu == 'ordres' ? 'actif' : ''}" href="${pageContext.request.contextPath}/dg/ordres">Mes ordres</a>
        </sec:authorize>
      </nav>
    </sec:authorize>

    <div class="lateral-utilisateur">
      <span class="avatar avatar--petit">${utilisateurConnecte != null ? utilisateurConnecte.email.substring(0,1).toUpperCase() : '?'}</span>
      <div>
        <c:out value="${utilisateurConnecte.email}"/>
        <small><c:out value="${utilisateurConnecte.role}"/></small>
      </div>
    </div>
  </aside>

  <div class="principal">
    <header class="entete">
      <nav class="fil" aria-label="Fil d'Ariane">
        <a href="${pageContext.request.contextPath}/">SIRH</a><span>/</span><c:out value="${title}"/>
      </nav>
      <div class="rangee">
        <a class="cloche" title="Notifications" href="${pageContext.request.contextPath}/notifications">
          🔔
          <c:if test="${notificationsNonLues > 0}">
            <span class="cloche-compteur">${notificationsNonLues}</span>
          </c:if>
        </a>
        <a class="btn btn-discret btn-petit" href="${pageContext.request.contextPath}/deconnexion">Déconnexion</a>
      </div>
    </header>

    <main class="page">
      <jsp:doBody/>
    </main>
  </div>
</div>
</body>
</html>
