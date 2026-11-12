<%@ tag pageEncoding="UTF-8" trimDirectiveWhitespaces="true" %>
<%@ attribute name="demande" required="true" type="com.sirh.demande.entity.Demande" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="ui" tagdir="/WEB-INF/tags" %>
<c:if test="${demande.type == 'RECRUTEMENT'}">
  <dl class="infos" style="margin-block:var(--e-4);">
    <div><dt>Poste</dt><dd><c:out value="${demande.posteCible.intitule}" default="Non renseigné"/></dd></div>
    <div><dt>Département</dt><dd><c:out value="${demande.posteCible.section.service.departement.nom}" default="Non renseigné"/></dd></div>
    <div><dt>Motif</dt><dd><c:out value="${demande.motifBesoin}" default="Non renseigné"/></dd></div>
    <div><dt>Effectif</dt><dd><c:out value="${demande.effectifDemande}" default="Non renseigné"/></dd></div>
    <div><dt>Salaire brut mensuel / poste</dt><dd><c:out value="${demande.salaireMensuelParPoste}" default="Non renseigné"/><c:if test="${demande.salaireMensuelParPoste != null}"> Ar</c:if></dd></div>
    <div><dt>Coût annuel prévu</dt><dd><c:out value="${demande.coutAnnuelPrevu}" default="Non calculable"/><c:if test="${demande.coutAnnuelPrevu != null}"> Ar</c:if></dd></div>
    <div><dt>Budget global (annuel)</dt><dd><c:out value="${demande.budgetGlobalRecrutement}" default="Non défini"/><c:if test="${demande.budgetGlobalRecrutement != null}"> Ar</c:if></dd></div>
    <div><dt>Date souhaitée</dt><dd><c:choose><c:when test="${demande.dateSouhaitee != null}"><ui:date valeur="${demande.dateSouhaitee}"/></c:when><c:otherwise>Non renseignée</c:otherwise></c:choose></dd></div>
  </dl>
</c:if>
