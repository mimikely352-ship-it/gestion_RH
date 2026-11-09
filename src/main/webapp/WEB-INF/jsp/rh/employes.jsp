<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="ui" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<ui:layout title="Employés et historique" activeMenu="employes">
  <div class="rangee" style="justify-content:space-between;">
    <h1>Employés et historique</h1>
    <ui:bouton label="Créer un employé" href="/rh/employes/nouveau" style="btn-principal"/>
  </div>
  <c:if test="${not empty message}"><p role="status"><c:out value="${message}"/></p></c:if>
  <c:if test="${not empty erreur}"><ui:alerte type="erreur"><c:out value="${erreur}"/></ui:alerte></c:if>
  <p>Consultez les employés actifs, leur poste et les mouvements enregistrés. Une demande de transfert en cours ne change pas le poste actuel.</p>
  <div class="fiche"><div class="fiche-corps">
    <h2>Effectif actuel (<c:out value="${nombreEmployes}"/>)</h2>
    <c:if test="${empty employes}"><p>Aucun employé actif.</p></c:if>
    <div style="overflow-x:auto">
      <table class="tableau">
        <caption class="champ-aide">Le poste provient de l’embauche ou d’un mouvement exécuté. Les dates absentes des anciens dossiers ne sont pas reconstituées.</caption>
        <thead><tr><th scope="col">Employé</th><th scope="col">Poste actuel</th><th scope="col">Date d’entrée</th><th scope="col">Service</th><th scope="col">Intégration</th><th scope="col">Parcours dans l’entreprise</th></tr></thead>
        <tbody><c:forEach var="e" items="${employes}"><tr>
          <td>
            <c:choose>
              <c:when test="${e.dossierId != null}"><a href="${pageContext.request.contextPath}/rh/employes/${e.dossierId}"><strong><c:out value="${e.nom}"/></strong></a></c:when>
              <c:otherwise><strong><c:out value="${e.nom}"/></strong></c:otherwise>
            </c:choose>
            <br><c:out value="${e.email}"/></td>
          <td><c:out value="${e.poste}"/></td><td><c:out value="${e.dateEntree}"/></td><td><c:out value="${e.service}"/></td>
          <td><c:if test="${e.dossierId != null}"><a href="${pageContext.request.contextPath}/rh/employes/${e.dossierId}"><c:out value="${avancementIntegration[e.dossierId]}"/></a></c:if></td>
          <td><details><summary>Voir le parcours</summary>
            <h3>Affectations enregistrées</h3>
            <c:if test="${empty e.historique}"><p>Aucun historique disponible pour ce compte.</p></c:if>
            <ul><c:forEach var="h" items="${e.historique}"><li><c:out value="${h}"/></li></c:forEach></ul>
            <c:if test="${e.dossierId != null}">
              <details><summary>Renseigner ou corriger la date d’entrée</summary>
                <ui:formulaire action="/rh/employes/date-entree">
                  <input type="hidden" name="dossierId" value="${e.dossierId}">
                  <label for="entree-${e.dossierId}">Date réelle d’entrée de <c:out value="${e.nom}"/></label>
                  <input class="saisie" id="entree-${e.dossierId}" type="date" name="dateEntree" required>
                  <ui:bouton label="Enregistrer la date" type="submit" style="btn-secondaire"/>
                </ui:formulaire>
              </details>
            </c:if>
            <c:if test="${not empty e.transferts}"><h3>Demandes en cours</h3><ul><c:forEach var="t" items="${e.transferts}"><li><c:out value="${t}"/></li></c:forEach></ul></c:if>
            <c:if test="${e.dossierId != null}">
              <details><summary>Promouvoir / muter directement (sans demande de l’employé)</summary>
                <ui:formulaire action="/rh/employes/${e.dossierId}/promouvoir">
                  <ui:champ-liste label="Nouveau poste" name="posteCibleId" items="${postes}" valueProperty="id" labelProperty="intitule" requis="true"/>
                  <ui:champ label="Date d’effet" name="dateEffet" type="date"/>
                  <ui:bouton label="Appliquer la promotion" type="submit" style="btn-principal"/>
                </ui:formulaire>
              </details>
            </c:if>
          </details></td>
        </tr></c:forEach></tbody>
      </table>
    </div>
  </div></div>
  <c:if test="${not empty documentsARenouveler}">
    <div class="fiche" style="margin-top:var(--e-6);"><div class="fiche-corps">
      <h2>Documents administratifs à renouveler</h2>
      <p class="texte-doux">Pièces expirées ou expirant dans les 30 jours (dernière version déposée).</p>
      <div style="overflow-x:auto"><table class="tableau">
        <thead><tr><th scope="col">Salarié</th><th scope="col">Document</th><th scope="col">Expire le</th><th scope="col">État</th></tr></thead>
        <tbody><c:forEach var="r" items="${documentsARenouveler}"><tr>
          <td><a href="${pageContext.request.contextPath}/rh/employes/${r.dossierId}#documents"><c:out value="${r.nom}"/></a></td>
          <td><c:out value="${r.document}"/></td><td><c:out value="${r.expiration}"/></td>
          <td><span class="statut ${r.validite == 'Expiré' ? 'statut--danger' : 'statut--attente'}"><c:out value="${r.validite}"/></span></td>
        </tr></c:forEach></tbody>
      </table></div>
    </div></div>
  </c:if>
  <div class="fiche" style="margin-top:var(--e-6);"><div class="fiche-corps">
    <h2>Départs : démissions et renvois</h2>
    <p class="texte-doux">Les employés partis n’apparaissent plus dans l’effectif ; leur dossier complet reste consultable ici.</p>
    <c:if test="${empty departs}"><p>Aucun départ enregistré.</p></c:if>
    <c:if test="${not empty departs}">
      <div style="overflow-x:auto"><table class="tableau">
        <thead><tr><th scope="col">Employé</th><th scope="col">Type</th><th scope="col">Dernier jour</th><th scope="col">État du dossier</th></tr></thead>
        <tbody><c:forEach var="d" items="${departs}"><tr>
          <td><a href="${pageContext.request.contextPath}/rh/employes/${d.dossier.id}#depart"><strong><c:out value="${d.dossier.prenom}"/> <c:out value="${d.dossier.nom}"/></strong></a></td>
          <td><c:out value="${d.type.libelle}"/></td>
          <td><c:out value="${d.dateEffet}"/></td>
          <td><c:choose>
            <c:when test="${d.statut == 'EN_COURS'}"><span class="statut statut--attente">En cours</span></c:when>
            <c:otherwise><span class="statut statut--neutre">Clôturé — a quitté l’entreprise</span></c:otherwise>
          </c:choose></td>
        </tr></c:forEach></tbody>
      </table></div>
    </c:if>
  </div></div>
  <nav aria-label="Pagination">
    <c:if test="${pageEffectif > 0}"><a class="btn btn-secondaire" href="?page=${pageEffectif - 1}">Page precedente</a></c:if>
    <c:if test="${(pageEffectif + 1) * 25 < nombreEmployes}"><a class="btn btn-secondaire" href="?page=${pageEffectif + 1}">Page suivante</a></c:if>
  </nav>
</ui:layout>
