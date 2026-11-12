<%@ tag pageEncoding="UTF-8" %>
<%@ attribute name="validations" required="true" type="java.util.Collection" %>
<%@ attribute name="rh" required="false" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="ui" tagdir="/WEB-INF/tags" %>
<section class="fiche fiche-corps"><h2>À valider</h2>
  <c:if test="${empty validations}"><p>Aucune demande en attente.</p></c:if>
  <c:forEach var="v" items="${validations}"><details class="presence-demande">
    <summary><c:out value="${v.salarie}"/> · <c:out value="${v.demande.type.nom}"/> · ${v.demande.jours} jour(s) <ui:statut valeur="${v.demande.statut}"/></summary>
    <p><ui:date valeur="${v.demande.debut}"/> → <ui:date valeur="${v.demande.fin}"/>
      <small class="texte-doux"> · déposée le <ui:date valeur="${v.demande.soumiseLe}" heure="true"/></small></p>
    <c:if test="${v.demande.decisionManagerLe != null}"><p class="texte-doux">Validée par le responsable (<c:out value="${v.demande.decisionManagerPar}"/>) le <ui:date valeur="${v.demande.decisionManagerLe}" heure="true"/></p></c:if>
    <c:if test="${not empty v.demande.commentaireManager}"><p>Commentaire du responsable : <c:out value="${v.demande.commentaireManager}"/></p></c:if>
    <p><c:out value="${v.demande.motif}"/></p>
    <c:if test="${rh == 'true' and v.demande.justificatifChemin != null}"><a href="${pageContext.request.contextPath}/conges/${v.demande.id}/justificatif">Justificatif PDF</a></c:if>
    <c:if test="${v.decisionPossible}">
      <ui:formulaire action="/conges/${v.demande.id}/decision">
        <ui:champ label="Commentaire (obligatoire pour un refus)" name="commentaire" maxlength="500"/>
        <div class="rangee"><button class="btn btn-valider" type="submit" name="accepter" value="true">Valider</button><button class="btn btn-refuser" type="submit" name="accepter" value="false">Refuser</button></div>
      </ui:formulaire>
    </c:if>
  </details></c:forEach>
</section>
