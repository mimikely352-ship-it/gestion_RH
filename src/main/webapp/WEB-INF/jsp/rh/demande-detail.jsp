<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="ui" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<ui:layout title="Demande" activeMenu="demandes">
  <ui:bouton label="Retour aux demandes" href="/rh/demandes" style="btn-secondaire"/>
  <c:if test="${not empty erreur}"><ui:alerte type="erreur"><c:out value="${erreur}"/></ui:alerte></c:if>
  <c:if test="${not empty succes}"><p role="status"><c:out value="${succes}"/></p></c:if>
  <h1><c:out value="${demande.type}"/> — <c:out value="${demande.auteurEmail}"/></h1>
  <p class="texte-doux"><c:out value="${demande.description}"/></p>
  <ui:statut valeur="${demande.statut}"/>
  <c:if test="${demande.affectationOrigine != null}"><p>Poste avant le mouvement : <strong><c:out value="${demande.affectationOrigine.posteLibelle}"/></strong></p></c:if>

  <c:if test="${demande.posteCible != null}"><p>Poste demandé : <strong><c:out value="${demande.posteCible.intitule}"/></strong></p></c:if>
  <c:if test="${demande.statut == 'EN_EXECUTION' and demande.type != 'RECRUTEMENT'}">
    <div class="fiche" style="margin-top:var(--e-5);"><div class="fiche-corps">
      <h3>Exécution</h3>
      <p class="texte-doux">
        <c:choose>
          <c:when test="${demande.type == 'TRANSFERT' or demande.type == 'PROMOTION'}">Confirmez lorsque le changement de poste est effectif. Cette action met à jour le poste affiché dans l’effectif, le service et le parcours de l’employé.</c:when>
          <c:otherwise>Confirmez lorsque la formation ou la prestation demandée a réellement été réalisée. Le poste de l’employé reste inchangé.</c:otherwise>
        </c:choose>
      </p>
      <ui:formulaire action="/rh/demandes/${demande.id}/cloturer-execution">
        <label for="date-realisation">Date effective de réalisation</label>
        <input class="saisie" id="date-realisation" type="date" name="dateEffet" required>
        <ui:bouton label="Confirmer la réalisation" type="submit" style="btn-valider"/>
      </ui:formulaire>
    </div></div>
  </c:if>
  <c:if test="${demande.statut == 'EN_EXECUTION' and demande.type == 'RECRUTEMENT'}">
    <p>Le recrutement se poursuit depuis l’offre associée.</p>
    <ui:bouton label="Voir les offres d’emploi" href="/rh/offres" style="btn-principal"/>
  </c:if>

  <h3 style="margin-top:var(--e-6);">Historique de validation</h3>
  <ui:list data="${historique}" columns="etape,valideurEmail,decision,dateValidation" columnLabels="Étape,Valideur,Décision,Date"/>
</ui:layout>
