<%@ tag pageEncoding="UTF-8" %>
<%@ attribute name="demandes" required="true" type="java.util.Collection" %>
<%@ attribute name="dossierId" required="true" %>
<%@ attribute name="rh" required="false" %>
<%@ attribute name="aujourdhui" required="false" type="java.time.LocalDate" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="ui" tagdir="/WEB-INF/tags" %>
<section class="fiche fiche-corps"><h2>Demandes de congé</h2>
  <c:if test="${empty demandes}"><p>Aucune demande.</p></c:if>
  <c:forEach var="d" items="${demandes}"><details class="presence-demande">
    <summary><c:out value="${d.type.nom}"/> · <ui:date valeur="${d.debut}"/> → <ui:date valeur="${d.fin}"/> · ${d.jours} jour(s) <ui:statut valeur="${d.statut}"/></summary>
    <p><c:out value="${d.motif}"/></p>
    <p class="texte-doux">Déposée le <ui:date valeur="${d.soumiseLe}" heure="true"/></p>
    <c:if test="${d.decisionManagerLe != null}"><p>Responsable : <c:out value="${d.decisionManagerPar}"/> · <ui:date valeur="${d.decisionManagerLe}" heure="true"/>
      <c:if test="${not empty d.commentaireManager}"> — <c:out value="${d.commentaireManager}"/></c:if></p></c:if>
    <c:if test="${d.decisionLe != null}"><p>Décision : <c:out value="${d.decisionPar}"/> · <ui:date valeur="${d.decisionLe}" heure="true"/>
      <c:if test="${not empty d.commentaire}"> — <c:out value="${d.commentaire}"/></c:if></p></c:if>
    <c:if test="${d.annuleeLe != null}"><p>Annulée par <c:out value="${d.annuleePar}"/> · <ui:date valeur="${d.annuleeLe}" heure="true"/>
      <c:if test="${not empty d.motifAnnulation}"> — <c:out value="${d.motifAnnulation}"/></c:if></p></c:if>
    <c:if test="${d.justificatifChemin != null}"><a href="${pageContext.request.contextPath}/conges/${d.id}/justificatif">Justificatif PDF</a></c:if>
    <%-- Annulation : demande en attente (salarié ou RH), ou congé accepté non commencé (RH uniquement). --%>
    <c:set var="enAttente" value="${d.statut == 'ATTENTE_MANAGER' or d.statut == 'ATTENTE_RH'}"/>
    <c:set var="accepteFutur" value="${d.statut == 'ACCEPTE' and aujourdhui != null and d.debut.isAfter(aujourdhui)}"/>
    <c:choose>
      <c:when test="${enAttente or (rh == 'true' and accepteFutur)}">
        <c:set var="annulation" value="/conges/${d.id}/annuler"/><c:if test="${rh == 'true'}"><c:set var="annulation" value="/rh/conges/${d.id}/annuler"/></c:if>
        <ui:formulaire action="${annulation}" cssClass="presence-filtres"><input type="hidden" name="dossierId" value="${dossierId}"><ui:champ label="Motif d’annulation" name="motif" maxlength="500" requis="true"/><ui:bouton label="${enAttente ? 'Annuler la demande' : 'Annuler le congé'}" type="submit" style="btn-secondaire"/></ui:formulaire>
      </c:when>
      <c:when test="${d.statut == 'ACCEPTE' and rh == 'true'}"><p class="texte-doux">Congé commencé : il ne peut plus être annulé.</p></c:when>
      <c:when test="${accepteFutur}"><p class="texte-doux">Pour annuler un congé accepté, adressez-vous aux RH.</p></c:when>
    </c:choose>
  </details></c:forEach>
</section>
