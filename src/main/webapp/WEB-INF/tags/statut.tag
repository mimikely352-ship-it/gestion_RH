<%@ tag description="Badge de statut coloré par famille (succès/attente/danger/info/neutre), déduite du nom de la valeur d'énumération. Voir AFFICHAGE.md §3 pour le code couleur d'origine." pageEncoding="UTF-8" trimDirectiveWhitespaces="true" %>
<%@ attribute name="valeur" required="true" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<c:set var="v" value="${valeur}"/>
<c:choose>
  <c:when test="${fn:contains(v,'REFUS') or fn:contains(v,'REJET') or fn:contains(v,'ECHEC') or v == 'NON_RETENUE'}">
    <span class="statut statut--danger"><c:out value="${v}"/></span>
  </c:when>
  <c:when test="${v == 'EN_ATTENTE' or fn:contains(v,'EN_ENTRETIEN') or fn:contains(v,'EN_TEST') or fn:contains(v,'EN_EVALUATION') or fn:contains(v,'EN_EXECUTION') or v == 'EMIS' or v == 'SOUMISE' or v == 'BROUILLON' or v == 'RECUE'}">
    <span class="statut statut--attente"><c:out value="${v}"/></span>
  </c:when>
  <c:when test="${fn:contains(v,'VALIDE') or fn:contains(v,'ACCEPT') or v == 'REUSSI' or v == 'RETENUE' or v == 'EMBAUCHE' or v == 'EXECUTEE' or v == 'EXECUTE' or v == 'PUBLIEE'}">
    <span class="statut statut--succes"><c:out value="${v}"/></span>
  </c:when>
  <c:otherwise>
    <span class="statut statut--neutre"><c:out value="${v}"/></span>
  </c:otherwise>
</c:choose>
