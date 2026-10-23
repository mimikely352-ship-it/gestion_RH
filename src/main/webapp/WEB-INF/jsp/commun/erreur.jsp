<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="ui" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<ui:layout-public title="Action impossible">
  <main class="site-conteneur site-main"><div class="fiche"><div class="fiche-corps">
    <h1>Cette action ne peut pas être terminée</h1>
    <p role="alert"><c:out value="${messageErreur}"/></p>
    <ui:bouton label="Revenir à mon espace" href="/compte/accueil" style="btn-principal"/>
  </div></div></main>
</ui:layout-public>
