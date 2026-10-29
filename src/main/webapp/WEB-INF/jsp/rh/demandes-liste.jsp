<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="ui" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<ui:layout title="Demandes" activeMenu="demandes">
  <h1>Demandes à valider</h1>
  <p>Les transferts vous sont adressés directement. Votre acceptation suffit pour autoriser leur réalisation, sans validation du chef, de la direction ou du DG.</p>
  <c:choose>
    <c:when test="${empty aValider}"><div class="vide"><p>Aucune demande en attente de validation RH.</p></div></c:when>
    <c:otherwise>
      <div class="pile" style="margin-top:var(--e-5);">
        <c:forEach var="d" items="${aValider}">
          <div class="fiche"><div class="fiche-corps">
            <h3><c:out value="${d.type}"/> — <c:out value="${d.auteurEmail}"/></h3>
            <p class="texte-doux"><c:out value="${d.description}"/></p>
            <c:if test="${d.affectationOrigine != null}"><p>Poste actuel : <strong><c:out value="${d.affectationOrigine.posteLibelle}"/></strong></p></c:if>
            <c:if test="${d.posteCible != null}"><p>Poste demandé : <strong><c:out value="${d.posteCible.intitule}"/></strong></p></c:if>
            <div class="rangee" style="margin-top:var(--e-3);">
              <ui:formulaire action="/rh/demandes/${d.id}/valider" cssClass="rangee">
                <input type="hidden" name="decision" value="VALIDE"/>
                <c:if test="${d.type == 'TRANSFERT' or d.type == 'PROMOTION'}">
                  <ui:champ-liste label="Poste final (modifiable)" name="nouveauPosteCibleId" items="${postes}"
                                   valueProperty="id" labelProperty="intitule" selected="${d.posteCible != null ? d.posteCible.id : null}"/>
                </c:if>
                <ui:bouton label="Valider" type="submit" style="btn-valider btn-petit"/>
              </ui:formulaire>
              <ui:formulaire action="/rh/demandes/${d.id}/valider" cssClass="rangee">
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

  <h1 style="margin-top:var(--e-8);">Demandes à exécuter</h1>
  <c:choose>
    <c:when test="${empty aExecuter}"><div class="vide"><p>Aucune demande validée en attente d'exécution.</p></div></c:when>
    <c:otherwise>
      <div class="pile" style="margin-top:var(--e-5);">
        <c:forEach var="d" items="${aExecuter}">
          <div class="fiche"><div class="fiche-corps">
            <h3><c:out value="${d.type}"/> — <c:out value="${d.auteurEmail}"/></h3>
            <p class="texte-doux"><c:out value="${d.description}"/></p>
            <p class="texte-petit texte-doux">À exécuter avant le <c:out value="${d.dateLimiteExecution}"/></p>
            <ui:formulaire action="/rh/demandes/${d.id}/executer">
              <ui:bouton label="Marquer en cours d'exécution" type="submit" style="btn-principal btn-petit"/>
            </ui:formulaire>
          </div></div>
        </c:forEach>
      </div>
    </c:otherwise>
  </c:choose>
  <h2 style="margin-top:var(--e-8);">Demandes en cours d’exécution</h2>
  <c:choose>
    <c:when test="${empty enExecution}"><div class="vide"><p>Aucune demande en cours d’exécution.</p></div></c:when>
    <c:otherwise>
      <div class="pile" style="margin-top:var(--e-5);">
        <c:forEach var="d" items="${enExecution}">
          <div class="fiche"><div class="fiche-corps">
            <h3><c:out value="${d.type}"/> — <c:out value="${d.auteurEmail}"/></h3>
            <p><c:out value="${d.description}"/></p>
            <c:if test="${d.posteCible != null}"><p>Poste demandé : <strong><c:out value="${d.posteCible.intitule}"/></strong></p></c:if>
            <ui:bouton label="Ouvrir et confirmer la réalisation" href="/rh/demandes/${d.id}" style="btn-principal"/>
          </div></div>
        </c:forEach>
      </div>
    </c:otherwise>
  </c:choose>
</ui:layout>
