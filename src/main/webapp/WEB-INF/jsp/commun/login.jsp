<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="ui" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<ui:layout-public title="Connexion" pageCss="connexion.css">
  <main class="connexion" aria-labelledby="connexion-titre">
    <section class="connexion-identite" aria-labelledby="connexion-promesse">
      <div class="connexion-signature"><span class="connexion-point" aria-hidden="true"></span> L’ESPACE DES TALENTS</div>
      <div class="connexion-message">
        <span class="connexion-edition">Votre parcours, vos perspectives.</span>
        <h2 id="connexion-promesse">Les talents font<br>grandir les équipes.</h2>
        <p>Un même espace pour trouver votre prochaine opportunité et accompagner votre vie professionnelle.</p>
      </div>
      <div class="connexion-parcours"><span>01 <strong>Rencontrer</strong></span><span>02 <strong>Rejoindre</strong></span><span>03 <strong>Évoluer</strong></span></div>
      <div class="connexion-orbite" aria-hidden="true"></div>
    </section>
    <section class="connexion-espace">
      <div class="connexion-formulaire">
        <span class="connexion-surtitre">BIENVENUE CHEZ <c:out value="${nomApplication}" default="Oria RH"/></span>
        <h1 id="connexion-titre">Heureux de vous revoir.</h1>
        <p class="connexion-introduction">Connectez-vous pour retrouver votre espace.</p>
        <c:if test="${not empty erreur}"><div class="connexion-erreur" role="alert"><c:out value="${erreur}"/></div></c:if>
        <ui:formulaire action="/login" cssClass="connexion-champs">
          <c:if test="${offreId != null}"><input type="hidden" name="offreId" value="${offreId}"><p class="connexion-contexte">Connectez-vous pour poursuivre votre candidature à l’offre choisie.</p></c:if>
          <div class="connexion-champ">
            <label for="connexion-email">Adresse e-mail</label>
            <input class="saisie" id="connexion-email" name="email" type="email" autocomplete="username" placeholder="vous@exemple.com" value="<c:out value='${email}'/>" required>
          </div>
          <div class="connexion-champ">
            <label for="connexion-mot-de-passe">Mot de passe</label>
            <div class="connexion-secret">
              <input class="saisie" id="connexion-mot-de-passe" name="motDePasse" type="password" autocomplete="current-password" placeholder="Votre mot de passe" required>
              <button type="button" class="connexion-voir" aria-controls="connexion-mot-de-passe" aria-pressed="false" hidden>Afficher</button>
            </div>
          </div>
          <button class="btn btn-principal connexion-envoyer" type="submit">Se connecter <ui:icone nom="suivant"/></button>
        </ui:formulaire>
        <div class="connexion-decouverte">
          <c:choose><c:when test="${offreId != null}"><p>Pas encore de compte ?</p><a href="${pageContext.request.contextPath}/offres/${offreId}/postuler">Candidater à cette offre <ui:icone nom="suivant"/></a></c:when>
          <c:otherwise><p>Votre prochaine aventure commence ici.</p><a href="${pageContext.request.contextPath}/offres">Découvrir les offres d’emploi <ui:icone nom="suivant"/></a></c:otherwise></c:choose>
        </div>
      </div>
    </section>
  </main>
  <script src="${pageContext.request.contextPath}/assets/js/connexion.js" defer></script>
</ui:layout-public>
