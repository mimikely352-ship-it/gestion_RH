<%@ tag description="Squelette commun des pages internes (RH, DG, Direction, Chef de service, Employé) : barre latérale + en-tête + contenu. Reprend la structure .app/.lateral/.principal de la charte graphique (admin.css)." pageEncoding="UTF-8" trimDirectiveWhitespaces="true" %>
<%@ attribute name="title" required="true" %>
<%@ attribute name="activeMenu" required="false" %>
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
  <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/admin.css">
  <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/extensions.css">
  <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/recrutement.css">
  <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/interface.css">
  <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/offre-assistant.css">
  <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/theme.css">
</head>
<body>
<a class="interface-evitement" href="#contenu-principal">Aller au contenu principal</a>
<div class="app">

  <aside class="lateral">
    <a href="${pageContext.request.contextPath}/" class="lateral-marque">
      <img class="marque-logo" src="${pageContext.request.contextPath}/assets/brand/logo" alt="" width="46" height="46">
      <strong><c:out value="${nomApplication}" default="Oria RH"/></strong>
    </a>

    <button class="interface-menu" type="button" aria-expanded="false" aria-controls="navigation-principale">Menu</button>
    <div class="interface-navigation" id="navigation-principale">
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
      <nav class="lateral-groupe" aria-label="Organisation">
        <span class="lateral-titre">Organisation</span>
        <a class="lateral-lien ${activeMenu == 'organisation' ? 'actif' : ''}" href="${pageContext.request.contextPath}/rh/organisation">Direction / départements / services</a>
        <a class="lateral-lien ${activeMenu == 'employe-nouveau' ? 'actif' : ''}" href="${pageContext.request.contextPath}/rh/employes/nouveau">Créer un employé</a>
      </nav>
    </sec:authorize>

    <sec:authorize access="hasAnyRole('EMPLOYE','CHEF_SERVICE','DIRECTION','RH','DG')">
      <nav class="lateral-groupe" aria-label="Demandes">
        <span class="lateral-titre">Demandes internes</span>
        <sec:authorize access="hasRole('EMPLOYE')">
          <a class="lateral-lien ${activeMenu == 'mes-demandes' ? 'actif' : ''}" href="${pageContext.request.contextPath}/employe/demandes">Mes demandes</a>
          <a class="lateral-lien" href="${pageContext.request.contextPath}/candidat/mes-candidatures">Mon dossier de recrutement</a>
        </sec:authorize>
        <sec:authorize access="hasRole('CHEF_SERVICE')">
          <a class="lateral-lien ${activeMenu == 'demandes' ? 'actif' : ''}" href="${pageContext.request.contextPath}/chef-service/demandes">Demandes à valider</a>
        </sec:authorize>
        <sec:authorize access="hasRole('DIRECTION')">
          <a class="lateral-lien ${activeMenu == 'demandes' ? 'actif' : ''}" href="${pageContext.request.contextPath}/direction/demandes">Demandes à valider</a>
        </sec:authorize>
        <sec:authorize access="hasRole('RH')">
          <a class="lateral-lien ${activeMenu == 'employes' ? 'actif' : ''}" href="${pageContext.request.contextPath}/rh/employes">Employés et historique</a>
          <a class="lateral-lien" href="${pageContext.request.contextPath}/rh/mobilite">Mobilité interne</a>
          <a class="lateral-lien ${activeMenu == 'demandes' ? 'actif' : ''}" href="${pageContext.request.contextPath}/rh/demandes">Demandes à valider / exécuter</a>
          <a class="lateral-lien ${activeMenu == 'ordres' ? 'actif' : ''}" href="${pageContext.request.contextPath}/rh/ordres">Ordres du DG</a>
        </sec:authorize>
        <sec:authorize access="hasRole('DG')">
          <a class="lateral-lien ${activeMenu == 'demandes' ? 'actif' : ''}" href="${pageContext.request.contextPath}/dg/demandes">Demandes à valider</a>
          <a class="lateral-lien ${activeMenu == 'ordres' ? 'actif' : ''}" href="${pageContext.request.contextPath}/dg/ordres">Mes ordres</a>
        </sec:authorize>
      </nav>
    </sec:authorize>

    </div>
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
        <a href="${pageContext.request.contextPath}/"><c:out value="${nomApplication}" default="Oria RH"/></a><span>/</span><c:out value="${title}"/>
      </nav>
      <div class="rangee">
        <a class="cloche" title="Notifications" aria-label="Notifications${notificationsNonLues > 0 ? ' : notifications non lues' : ''}" href="${pageContext.request.contextPath}/notifications">
          <ui:icone nom="notification"/>
          <c:if test="${notificationsNonLues > 0}">
            <span class="cloche-compteur">${notificationsNonLues}</span>
          </c:if>
        </a>
        <ui:formulaire action="/deconnexion" cssClass="rangee"><ui:bouton label="Déconnexion" type="submit" style="btn-secondaire btn-petit"/></ui:formulaire>
      </div>
    </header>

    <main class="page" id="contenu-principal" tabindex="-1">
      <jsp:doBody/>
    </main>
  </div>
</div>
<script src="${pageContext.request.contextPath}/assets/js/interface.js" defer></script>
</body>
</html>
