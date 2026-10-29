<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="ui" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<ui:layout-public title="${evaluation.titre}">
  <main class="site-conteneur site-main" style="max-width:640px;">
    <h1><c:out value="${evaluation.titre}"/></h1>
    <ui:formulaire action="/candidat/evaluations/${evaluation.id}/repondre">
      <c:forEach var="q" items="${questions}" varStatus="st">
        <div class="fiche" style="margin-bottom:var(--e-3);"><div class="fiche-corps">
          <p><b>${st.index + 1}. <c:out value="${q.enonce}"/></b></p>
          <c:if test="${q.type == 'QCM'}">
            <c:forEach var="ch" items="${q.choix}">
              <label class="case" style="margin-top:var(--e-2);">
                <input type="radio" name="choix_${q.id}" value="${ch.id}"/>
                <c:out value="${ch.libelle}"/>
              </label>
            </c:forEach>
          </c:if>
          <c:if test="${q.type == 'TEXTE'}">
            <textarea class="saisie" name="texte_${q.id}" rows="4"></textarea>
          </c:if>
        </div></div>
      </c:forEach>
      <ui:bouton label="Envoyer mes réponses" type="submit" style="btn-principal"/>
    </ui:formulaire>
  </main>
</ui:layout-public>
