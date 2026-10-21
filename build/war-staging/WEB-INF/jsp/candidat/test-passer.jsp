<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="ui" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<ui:layout-public title="${test.titre}">
  <main class="site-conteneur site-main" style="max-width:640px;">
    <h1>Niveau ${test.niveau} — <c:out value="${test.titre}"/></h1>
    <ui:formulaire action="/candidat/tests/${test.id}/passer">
      <input type="hidden" name="candidatureId" value="${candidatureId}"/>
      <c:forEach var="q" items="${questions}" varStatus="st">
        <div class="fiche" style="margin-bottom:var(--e-3);"><div class="fiche-corps">
          <p><b>${st.index + 1}. <c:out value="${q.enonce}"/></b></p>
          <c:forEach var="ch" items="${q.choix}">
            <label class="case" style="margin-top:var(--e-2);">
              <input type="radio" name="reponse_${q.id}" value="${ch.id}"/>
              <c:out value="${ch.libelle}"/>
            </label>
          </c:forEach>
        </div></div>
      </c:forEach>
      <ui:bouton label="Valider mes réponses" type="submit" style="btn-principal"/>
    </ui:formulaire>
  </main>
</ui:layout-public>
