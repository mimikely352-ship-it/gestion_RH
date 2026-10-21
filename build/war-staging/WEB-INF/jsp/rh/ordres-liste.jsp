<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="ui" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<ui:layout title="Ordres du DG" activeMenu="ordres">
  <h1>Ordres du DG</h1>
  <c:choose>
    <c:when test="${empty ordres}"><div class="vide"><p>Aucun ordre reçu.</p></div></c:when>
    <c:otherwise>
      <div class="pile" style="margin-top:var(--e-5);">
        <c:forEach var="o" items="${ordres}">
          <div class="fiche"><div class="fiche-corps">
            <div class="rangee" style="justify-content:space-between;">
              <div>
                <h3><c:out value="${o.description}"/></h3>
                <p class="texte-petit texte-doux">Émis par <c:out value="${o.dgEmail}"/> — échéance <c:out value="${o.dateLimite}"/></p>
              </div>
              <ui:statut valeur="${o.statut}"/>
            </div>
            <div class="rangee" style="margin-top:var(--e-3);">
              <c:if test="${o.statut == 'EMIS'}">
                <ui:formulaire action="/rh/ordres/${o.id}/demarrer"><ui:bouton label="Démarrer l'exécution" type="submit" style="btn-principal btn-petit"/></ui:formulaire>
              </c:if>
              <c:if test="${o.statut == 'EN_EXECUTION'}">
                <ui:formulaire action="/rh/ordres/${o.id}/terminer"><ui:bouton label="Marquer exécuté" type="submit" style="btn-valider btn-petit"/></ui:formulaire>
              </c:if>
            </div>
          </div></div>
        </c:forEach>
      </div>
    </c:otherwise>
  </c:choose>
</ui:layout>
