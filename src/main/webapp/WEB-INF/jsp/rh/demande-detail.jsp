<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="ui" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<ui:layout title="Demande" activeMenu="demandes">
  <h1><c:out value="${demande.type}"/> — <c:out value="${demande.auteurEmail}"/></h1>
  <p class="texte-doux"><c:out value="${demande.description}"/></p>
  <ui:statut valeur="${demande.statut}"/>

  <c:if test="${demande.statut == 'EN_EXECUTION' and (demande.type == 'PROMOTION' or demande.type == 'TRANSFERT')}">
    <div class="fiche" style="margin-top:var(--e-5);"><div class="fiche-corps">
      <h3>Exécution</h3>
      <p class="texte-doux">
        <c:if test="${demande.type == 'TRANSFERT'}">Organisez les tests du candidat interne comme pour un candidat externe, via l'offre publiée pour le poste ciblé.</c:if>
        <c:if test="${demande.type == 'PROMOTION'}">Choisissez un test existant ou créez-en un nouveau pour évaluer la promotion.</c:if>
      </p>
      <ui:formulaire action="/rh/demandes/${demande.id}/cloturer-execution">
        <ui:bouton label="Marquer la demande comme exécutée" type="submit" style="btn-valider"/>
      </ui:formulaire>
    </div></div>
  </c:if>

  <h3 style="margin-top:var(--e-6);">Historique de validation</h3>
  <ui:list data="${historique}" columns="etape,valideurEmail,decision,dateValidation" columnLabels="Étape,Valideur,Décision,Date"/>
</ui:layout>
