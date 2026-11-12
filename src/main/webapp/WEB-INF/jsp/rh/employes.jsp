<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="ui" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<ui:layout title="Employés" activeMenu="employes">
  <div class="rangee" style="justify-content:space-between;">
    <h1>Employés</h1>
    <ui:bouton label="Créer un employé" href="/rh/employes/nouveau" style="btn-principal"/>
  </div>
  <c:if test="${not empty message}"><p role="status"><c:out value="${message}"/></p></c:if>
  <c:if test="${not empty erreur}"><ui:alerte type="erreur"><c:out value="${erreur}"/></ui:alerte></c:if>
  <c:if test="${not empty contratsAEcheance}">
    <div class="employes-alerte"><span>${contratsAEcheance.size()} contrat(s) échu(s) ou arrivant à échéance sous 30 jours</span><a href="#echeances">Voir les contrats</a></div>
  </c:if>
  <c:if test="${not empty documentsARenouveler}">
    <div class="employes-alerte"><span>${documentsARenouveler.size()} document(s) à renouveler</span><a href="#renouvellements">Voir les documents</a></div>
  </c:if>
  <div class="employes-outils">
    <nav class="employes-onglets" aria-label="Liste des employés">
      <a href="#effectif" data-employes-target="effectif" aria-current="true">Effectif ${nombreEmployes}</a>
      <a href="#departs" data-employes-target="departs">Départs ${departs.size()}</a>
    </nav>
    <div class="employes-recherche"><label class="interface-visuel-cache" for="recherche-employes">Rechercher sur cette page</label><input class="saisie" type="search" id="recherche-employes" placeholder="Rechercher sur cette page"></div>
  </div>
  <p id="resultats-employes" class="interface-visuel-cache" role="status"></p>
  <div class="fiche employes-liste" id="effectif" data-employes-panel>
    <c:if test="${empty employes}"><p>Aucun employé actif.</p></c:if>
    <div style="overflow-x:auto">
      <table class="tableau">
        <thead><tr><th scope="col">Matricule</th><th scope="col">Employé</th><th scope="col">Poste actuel</th><th scope="col">Contrat en cours</th><th scope="col">Date d’entrée</th><th scope="col">Service</th><th scope="col">Intégration</th><th scope="col"><span class="interface-visuel-cache">Actions</span></th></tr></thead>
        <tbody><c:forEach var="e" items="${employes}"><tr data-employe>
          <td><c:out value="${empty e.matricule ? '—' : e.matricule}"/></td>
          <td>
            <c:choose>
              <c:when test="${e.dossierId != null}"><a href="${pageContext.request.contextPath}/rh/employes/${e.dossierId}"><strong><c:out value="${e.nom}"/></strong></a></c:when>
              <c:otherwise><strong><c:out value="${e.nom}"/></strong></c:otherwise>
            </c:choose>
            <c:if test="${not empty e.role}"><br><small class="texte-doux"><c:out value="${e.role}"/></small></c:if>
</td>
          <td><c:out value="${e.poste}"/></td>
          <td><c:out value="${e.contrat}"/>
            <c:choose>
              <c:when test="${e.contratAlerte == 'echu'}"> <span class="statut statut--danger">Échu</span></c:when>
              <c:when test="${e.contratAlerte == 'bientot'}"> <span class="statut statut--attente">Fin sous 30 jours</span></c:when>
              <c:when test="${e.contratAlerte == 'a-venir'}"> <span class="statut statut--attente">À venir</span></c:when>
              <c:when test="${e.contratAlerte == 'aucun'}"> <span class="statut statut--neutre">À enregistrer</span></c:when>
            </c:choose></td>
          <td><c:out value="${e.dateEntree}"/></td><td><c:out value="${e.service}"/></td>
          <td><c:if test="${e.dossierId != null}"><a href="${pageContext.request.contextPath}/rh/employes/${e.dossierId}"><c:out value="${avancementIntegration[e.dossierId]}"/></a></c:if></td>
          <td class="employes-actions">
            <c:choose>
              <c:when test="${e.dossierId != null}">
                <details class="employes-menu"><summary>⋯<span class="interface-visuel-cache"> Actions pour <c:out value="${e.nom}"/></span></summary>
                  <div class="employes-menu-liens">
                    <a href="${pageContext.request.contextPath}/rh/employes/${e.dossierId}">Fiche et parcours</a>
                    <a href="${pageContext.request.contextPath}/rh/employes/${e.dossierId}#actions-rh">Promouvoir ou muter</a>
                    <a href="${pageContext.request.contextPath}/rh/employes/${e.dossierId}#entree-fiche">Corriger la date d’entrée</a>
                  </div>
                </details>
              </c:when>
              <c:otherwise><details><summary>Parcours</summary><ul><c:forEach var="h" items="${e.historique}"><li><c:out value="${h}"/></li></c:forEach></ul></details></c:otherwise>
            </c:choose>
          </td>
        </tr></c:forEach></tbody>
      </table>
    </div>
  </div>
  <c:if test="${not empty contratsAEcheance}">
    <details class="fiche lecture-section" id="echeances" style="margin-top:var(--e-6);"><summary>Contrats à échéance</summary><div class="fiche-corps">
      <p class="texte-doux">Contrats en cours échus ou se terminant sous 30 jours, sans contrat suivant prévu : renouvelez-les,
        passez à un autre contrat ou ouvrez un dossier de départ.</p>
      <div style="overflow-x:auto"><table class="tableau">
        <thead><tr><th scope="col">Salarié</th><th scope="col">Contrat</th><th scope="col">Fin prévue</th><th scope="col">État</th></tr></thead>
        <tbody><c:forEach var="r" items="${contratsAEcheance}"><tr>
          <td><a href="${pageContext.request.contextPath}/rh/employes/${r.dossierId}#contrats"><c:out value="${r.nom}"/></a>
            <br><small class="texte-doux"><c:out value="${r.matricule}"/></small></td>
          <td><c:out value="${r.contrat}"/></td><td><c:out value="${r.fin}"/></td>
          <td><span class="statut ${r.echu ? 'statut--danger' : 'statut--attente'}"><c:out value="${r.etat}"/></span></td>
        </tr></c:forEach></tbody>
      </table></div>
    </div></details>
  </c:if>
  <c:if test="${not empty documentsARenouveler}">
    <details class="fiche lecture-section" id="renouvellements" style="margin-top:var(--e-6);"><summary>Documents à renouveler</summary><div class="fiche-corps">
      <p class="texte-doux">Échéance sous 30 jours ou dépassée.</p>
      <div style="overflow-x:auto"><table class="tableau">
        <thead><tr><th scope="col">Salarié</th><th scope="col">Document</th><th scope="col">Expire le</th><th scope="col">État</th></tr></thead>
        <tbody><c:forEach var="r" items="${documentsARenouveler}"><tr>
          <td><a href="${pageContext.request.contextPath}/rh/employes/${r.dossierId}#documents"><c:out value="${r.nom}"/></a></td>
          <td><c:out value="${r.document}"/></td><td><c:out value="${r.expiration}"/></td>
          <td><span class="statut ${r.validite == 'Expiré' ? 'statut--danger' : 'statut--attente'}"><c:out value="${r.validite}"/></span></td>
        </tr></c:forEach></tbody>
      </table></div>
    </div></details>
  </c:if>
  <div class="fiche" id="departs" data-employes-panel style="margin-top:var(--e-6);"><div class="fiche-corps">
    <h2>Départs</h2>
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
            <c:otherwise><span class="statut statut--neutre">Clôturé</span></c:otherwise>
          </c:choose></td>
        </tr></c:forEach></tbody>
      </table></div>
    </c:if>
  </div></div>
  <nav class="rangee employes-pagination" aria-label="Pagination de l’effectif">
    <c:if test="${pageEffectif > 0}"><a class="btn btn-secondaire" href="?page=${pageEffectif - 1}">Précédent</a></c:if>
    <c:if test="${(pageEffectif + 1) * 25 < nombreEmployes}"><a class="btn btn-secondaire" href="?page=${pageEffectif + 1}">Suivant</a></c:if>
  </nav>
</ui:layout>
