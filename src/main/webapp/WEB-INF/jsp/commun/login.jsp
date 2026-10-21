<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="ui" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<ui:layout-public title="Connexion">
  <main class="site-conteneur site-main" style="max-width:420px;margin:var(--e-12) auto;">
    <div class="fiche">
      <div class="fiche-entete"><h2>Connexion</h2></div>
      <div class="fiche-corps">
        <c:if test="${not empty erreur}">
          <ui:alerte type="erreur">${erreur}</ui:alerte>
        </c:if>
        <ui:formulaire action="/login" cssClass="pile">
          <ui:champ label="Email" name="email" type="email" requis="true"/>
          <ui:champ label="Mot de passe" name="motDePasse" type="password" requis="true"/>
          <div class="formulaire-actions">
            <ui:bouton label="Se connecter" type="submit" style="btn-principal"/>
          </div>
        </ui:formulaire>
        <p class="texte-petit texte-doux" style="margin-top:var(--e-5);">
          Pas encore candidat ? <a href="${pageContext.request.contextPath}/candidats/inscription">Créer un compte</a>
        </p>
      </div>
    </div>
  </main>
</ui:layout-public>
