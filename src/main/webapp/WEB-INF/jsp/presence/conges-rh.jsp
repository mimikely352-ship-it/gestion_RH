<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="ui" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<ui:layout title="Congés et absences" activeMenu="conges-rh">
  <h1>Congés et absences</h1>
  <c:if test="${not empty succes}"><p role="status"><c:out value="${succes}"/></p></c:if>
  <c:if test="${not empty erreur}"><ui:alerte type="erreur"><c:out value="${erreur}"/></ui:alerte></c:if>
  <ui:conge-validations validations="${validations}" rh="true"/>
  <form method="get" class="presence-filtres">
    <div class="champ"><label for="conges-salarie">Salarié</label><select class="saisie" id="conges-salarie" name="dossierId" required><option value="">Choisir un salarié</option><c:forEach var="d" items="${dossiers}"><option value="${d.id}" ${d.id == dossier.id ? 'selected' : ''}><c:out value="${d.prenom} ${d.nom}"/></option></c:forEach></select></div>
    <ui:champ label="Année" name="annee" type="number" min="2000" value="${annee}"/>
    <ui:bouton label="Afficher le dossier" type="submit" style="btn-secondaire"/>
  </form>
  <c:if test="${dossier != null}">
    <h2><c:out value="${dossier.prenom} ${dossier.nom}"/></h2>
    <ui:conge-soldes soldes="${soldes}"/>
    <details class="fiche lecture-section"><summary>Définir les droits annuels</summary><div class="fiche-corps">
      <ui:formulaire action="/rh/conges/droits"><input type="hidden" name="dossierId" value="${dossier.id}">
        <div class="champ"><label for="droit-type">Type décompté</label><select class="saisie" id="droit-type" name="typeId" required><c:forEach var="t" items="${types}"><c:if test="${t.deduitSolde}"><option value="${t.id}"><c:out value="${t.nom}"/></option></c:if></c:forEach></select></div>
        <div class="grille-2"><ui:champ label="Année" name="annee" type="number" min="2000" value="${annee}" requis="true"/><ui:champ label="Total des droits (jours)" name="jours" type="number" min="0" requis="true"/></div>
        <ui:champ label="Motif" name="motif" maxlength="255" requis="true"/>
        <ui:bouton label="Enregistrer les droits" type="submit" style="btn-principal"/>
      </ui:formulaire>
    </div></details>
    <ui:conge-historique demandes="${demandes}" dossierId="${dossier.id}" rh="true" aujourdhui="${aujourdhui}"/>
    <details class="fiche lecture-section"><summary>Enregistrer une absence</summary><div class="fiche-corps">
      <ui:formulaire action="/rh/conges/absences" enctype="multipart/form-data"><input type="hidden" name="dossierId" value="${dossier.id}">
        <div class="grille-2"><ui:champ label="Du" name="debut" type="date" requis="true"/><ui:champ label="Au" name="fin" type="date" requis="true"/></div>
        <ui:champ label="Motif" name="motif" maxlength="120" requis="true"/>
        <label><input type="checkbox" name="remuneree" value="true"> Absence rémunérée</label>
        <label for="absence-piece">Justificatif PDF (facultatif, 5 Mo maximum)</label><input class="saisie" id="absence-piece" type="file" name="justificatif" accept="application/pdf">
        <ui:bouton label="Enregistrer l’absence" type="submit" style="btn-principal"/>
      </ui:formulaire>
    </div></details>
    <c:if test="${not empty absences}"><section class="fiche fiche-corps"><h2>Absences enregistrées</h2>
      <c:forEach var="a" items="${absences}"><details class="presence-demande"><summary><ui:date valeur="${a.debut}"/> → <ui:date valeur="${a.fin}"/> · <c:out value="${a.libelle}"/></summary>
        <p>${a.remuneree ? 'Rémunérée' : 'Non rémunérée'}</p><c:if test="${a.justificatifChemin != null}"><a href="${pageContext.request.contextPath}/absences/${a.id}/justificatif">Justificatif PDF</a></c:if>
        <ui:formulaire action="/rh/conges/absences/${a.id}/annuler"><input type="hidden" name="dossierId" value="${dossier.id}"><ui:champ label="Motif d’annulation" name="motif" maxlength="255" requis="true"/><ui:bouton label="Annuler l’absence" type="submit" style="btn-secondaire"/></ui:formulaire>
      </details></c:forEach>
    </section></c:if>
  </c:if>
  <details class="fiche lecture-section"><summary>Ajouter un type de congé</summary><div class="fiche-corps">
    <ui:formulaire action="/rh/conges/types"><ui:champ label="Nom" name="nom" maxlength="80" requis="true"/>
      <div class="rangee"><label><input type="checkbox" name="deduitSolde" value="true"> Décompter un solde</label><label><input type="checkbox" name="remunere" value="true"> Rémunéré</label><label><input type="checkbox" name="justificatifRequis" value="true"> PDF obligatoire</label></div>
      <ui:bouton label="Créer le type" type="submit" style="btn-principal"/>
    </ui:formulaire>
  </div></details>
</ui:layout>
