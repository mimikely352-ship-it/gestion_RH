<%@ tag description="Badge de statut coloré par famille (succès/attente/danger/info/neutre), déduite du nom de la valeur d'énumération. Voir AFFICHAGE.md §3 pour le code couleur d'origine." pageEncoding="UTF-8" trimDirectiveWhitespaces="true" %>
<%@ attribute name="valeur" required="true" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<c:set var="v" value="${valeur}"/>
<c:set var="libelle" value="${fn:replace(v, '_', ' ')}"/>
<c:choose>
  <c:when test="${v == 'BROUILLON'}"><c:set var="libelle" value="Brouillon"/></c:when>
  <c:when test="${v == 'PLANIFIE'}"><c:set var="libelle" value="Planifié"/></c:when>
  <c:when test="${v == 'REALISE'}"><c:set var="libelle" value="Réalisé"/></c:when>
  <c:when test="${v == 'ANNULE'}"><c:set var="libelle" value="Annulé"/></c:when>
  <c:when test="${v == 'PUBLIEE'}"><c:set var="libelle" value="Publiée"/></c:when>
  <c:when test="${v == 'CLOTUREE'}"><c:set var="libelle" value="Clôturée"/></c:when>
  <c:when test="${v == 'RECUE'}"><c:set var="libelle" value="À examiner"/></c:when>
  <c:when test="${v == 'ACCEPTEE_ENTRETIEN'}"><c:set var="libelle" value="Retenue pour entretien"/></c:when>
  <c:when test="${v == 'NON_RETENUE'}"><c:set var="libelle" value="Non retenue"/></c:when>
  <c:when test="${v == 'EN_ENTRETIEN'}"><c:set var="libelle" value="Entretien en cours"/></c:when>
  <c:when test="${v == 'REFUSEE_ENTRETIEN'}"><c:set var="libelle" value="Non retenue après entretien"/></c:when>
  <c:when test="${v == 'EN_TEST'}"><c:set var="libelle" value="Tests en cours"/></c:when>
  <c:when test="${v == 'EN_EVALUATION'}"><c:set var="libelle" value="Évaluation en cours"/></c:when>
  <c:when test="${v == 'RETENUE'}"><c:set var="libelle" value="Retenue"/></c:when>
  <c:when test="${v == 'REFUSEE'}"><c:set var="libelle" value="Non retenue après évaluation"/></c:when>
  <c:when test="${v == 'EMBAUCHE'}"><c:set var="libelle" value="Embauche finalisée"/></c:when>
  <c:when test="${v == 'EN_ATTENTE'}"><c:set var="libelle" value="En attente de décision"/></c:when>
  <c:when test="${v == 'ACCEPTE'}"><c:set var="libelle" value="Accepté"/></c:when>
  <c:when test="${v == 'REFUSE'}"><c:set var="libelle" value="Refusé"/></c:when>
  <c:when test="${v == 'REUSSI'}"><c:set var="libelle" value="Réussi"/></c:when>
  <c:when test="${v == 'ECHEC'}"><c:set var="libelle" value="Échec"/></c:when>
</c:choose>
<c:choose>
  <c:when test="${fn:contains(v,'REFUS') or fn:contains(v,'REJET') or fn:contains(v,'ECHEC') or v == 'NON_RETENUE'}">
    <span class="statut statut--danger"><c:out value="${libelle}"/></span>
  </c:when>
  <c:when test="${v == 'EN_ATTENTE' or fn:contains(v,'EN_ENTRETIEN') or fn:contains(v,'EN_TEST') or fn:contains(v,'EN_EVALUATION') or fn:contains(v,'EN_EXECUTION') or v == 'EMIS' or v == 'SOUMISE' or v == 'BROUILLON' or v == 'RECUE'}">
    <span class="statut statut--attente"><c:out value="${libelle}"/></span>
  </c:when>
  <c:when test="${fn:contains(v,'VALIDE') or fn:contains(v,'ACCEPT') or v == 'REUSSI' or v == 'RETENUE' or v == 'EMBAUCHE' or v == 'EXECUTEE' or v == 'EXECUTE' or v == 'PUBLIEE'}">
    <span class="statut statut--succes"><c:out value="${libelle}"/></span>
  </c:when>
  <c:otherwise>
    <span class="statut statut--neutre"><c:out value="${libelle}"/></span>
  </c:otherwise>
</c:choose>
