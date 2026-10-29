<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="ui" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<ui:layout title="Demandes à valider" activeMenu="demandes">
  <h1>Demandes à valider (décision finale)</h1>
  <c:choose>
    <c:when test="${empty demandes}">
      <div class="vide"><p>Aucune demande en attente.</p></div>
    </c:when>
    <c:otherwise>
      <div class="pile" style="margin-top:var(--e-5);">
        <c:forEach var="d" items="${demandes}">
          <div class="fiche"><div class="fiche-corps">
            <h3><c:out value="${d.type}"/> — <c:out value="${d.auteurEmail}"/></h3>
            <p class="texte-doux"><c:out value="${d.description}"/></p>
            <c:if test="${d.affectationOrigine != null}"><p>Poste actuel : <strong><c:out value="${d.affectationOrigine.posteLibelle}"/></strong></p></c:if>
            <c:if test="${d.posteCible != null}"><p>Poste final retenu : <strong><c:out value="${d.posteCible.intitule}"/></strong></p></c:if>
            <div class="rangee" style="margin-top:var(--e-3);">
              <ui:formulaire action="/dg/demandes/${d.id}/valider" cssClass="rangee">
                <input type="hidden" name="decision" value="VALIDE"/>
                <ui:bouton label="Valider" type="submit" style="btn-valider btn-petit"/>
              </ui:formulaire>
              <ui:formulaire action="/dg/demandes/${d.id}/valider" cssClass="rangee">
                <input type="hidden" name="decision" value="REJETE"/>
                <input type="text" class="saisie" name="commentaire" placeholder="Motif du rejet" style="width:220px;"/>
                <ui:bouton label="Rejeter" type="submit" style="btn-refuser btn-petit"/>
              </ui:formulaire>
            </div>
          </div></div>
        </c:forEach>
      </div>
    </c:otherwise>
  </c:choose>
</ui:layout>
