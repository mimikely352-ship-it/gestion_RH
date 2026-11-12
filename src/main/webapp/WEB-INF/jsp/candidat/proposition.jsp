<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="ui" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<ui:layout-public title="Proposition d’embauche">
  <main class="site-conteneur site-main">
    <h1>Proposition d’embauche</h1>
    <c:if test="${not empty succes}"><p role="status"><c:out value="${succes}"/></p></c:if>
    <c:if test="${not empty erreur}"><ui:alerte type="erreur"><c:out value="${erreur}"/></ui:alerte></c:if>
    <section class="fiche fiche-corps">
      <h2><c:out value="${candidature.offreTitre}"/></h2>
      <dl class="infos">
        <div><dt>Entreprise</dt><dd><c:out value="${candidature.offre.entrepriseNom}"/></dd></div>
        <div><dt>Contrat</dt><dd><c:out value="${candidature.propositionTypeContrat.libelle}"/></dd></div>
        <div><dt>Salaire brut mensuel</dt><dd><c:out value="${candidature.propositionSalaire}"/> Ar</dd></div>
        <div><dt>Entrée</dt><dd><ui:date valeur="${candidature.propositionDebut}"/></dd></div>
        <c:if test="${candidature.propositionFin != null}"><div><dt>Fin prévue</dt><dd><ui:date valeur="${candidature.propositionFin}"/></dd></div></c:if>
        <div><dt>État</dt><dd><c:out value="${candidature.propositionStatut}"/></dd></div>
      </dl>
      <c:if test="${candidature.propositionStatut == 'EN_ATTENTE' and candidature.statut == 'RETENUE'}">
        <p class="texte-doux">Votre réponse est définitive. En acceptant, vous devenez salarié(e) : votre dossier salarié,
          votre contrat et votre programme d’intégration sont créés aussitôt, et votre compte donne accès à l’espace salarié
          avec les mêmes identifiants.</p>
        <div class="rangee">
          <ui:formulaire action="/candidat/propositions/${candidature.id}/reponse"><input type="hidden" name="accepter" value="true"><ui:bouton label="Accepter" type="submit" style="btn-valider"/></ui:formulaire>
          <ui:formulaire action="/candidat/propositions/${candidature.id}/reponse"><input type="hidden" name="accepter" value="false"><ui:bouton label="Refuser" type="submit" style="btn-refuser"/></ui:formulaire>
        </div>
      </c:if>
    </section>
    <a href="${pageContext.request.contextPath}/candidat/mes-candidatures">Mes candidatures</a>
  </main>
</ui:layout-public>
