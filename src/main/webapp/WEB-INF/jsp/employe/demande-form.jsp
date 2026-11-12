<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="ui" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<ui:layout title="${empty actionDemande ? 'Nouvelle demande' : 'Besoin de personnel'}" activeMenu="${empty actionDemande ? 'mes-demandes' : 'besoins'}">
  <h1>${empty actionDemande ? 'Nouvelle demande' : 'Besoin de personnel'}</h1>
  <c:if test="${empty actionDemande}"><p>Transfert : validation et affectation par les RH.</p></c:if>
  <c:if test="${not empty avertissementRattachement}"><p><c:out value="${avertissementRattachement}"/></p></c:if>
  <c:if test="${not empty erreur}"><ui:alerte type="erreur"><c:out value="${erreur}"/></ui:alerte></c:if>
  <div class="fiche" style="margin-top:var(--e-5);max-width:640px;"><div class="fiche-corps">
    <ui:formulaire action="${empty actionDemande ? '/employe/demandes' : actionDemande}">
      <div class="champ">
        <label class="champ-libelle" for="champ-type">Type de demande</label>
        <select class="saisie" id="champ-type" name="type" required>
          <c:forEach var="t" items="${types}"><option value="${t}" ${typeChoisi == t ? 'selected' : ''}><c:out value="${t}"/></option></c:forEach>
        </select>
      </div>
      <ui:champ-liste label="Poste ciblé (recrutement / transfert / promotion)" name="posteCibleId"
                      items="${postes}" valueProperty="id" labelProperty="intitule" selected="${posteChoisi}"/>
      <ui:champ-texte label="Description / motif" name="description" value="${description}" requis="true"/>
      <fieldset id="besoin-recrutement" class="besoin-champs">
        <legend>Besoin de personnel</legend>
        <div class="grille-2">
          <div class="champ"><label for="besoin-motif">Motif</label><select class="saisie" name="motifBesoin" id="besoin-motif" data-besoin-requis><option value="">Choisir</option><option value="CREATION" ${motifBesoin == 'CREATION' ? 'selected' : ''}>Création de poste</option><option value="REMPLACEMENT" ${motifBesoin == 'REMPLACEMENT' ? 'selected' : ''}>Remplacement</option></select></div>
          <div class="champ"><label for="besoin-effectif">Effectif</label><input class="saisie" type="number" min="1" step="1" name="effectifDemande" id="besoin-effectif" value="<c:out value='${effectifDemande}'/>" data-besoin-requis></div>
          <div class="champ"><label for="besoin-salaire">Salaire brut mensuel par poste (Ar)</label><input class="saisie" type="number" min="0.01" step="0.01" name="salaireMensuelParPoste" id="besoin-salaire" value="<c:out value='${salaireMensuelParPoste}'/>" data-besoin-requis></div>
          <div class="champ"><label for="besoin-budget">Budget global de recrutement (Ar / an)</label><input class="saisie" type="number" min="0.01" step="0.01" name="budgetGlobalRecrutement" id="besoin-budget" value="<c:out value='${budgetGlobalRecrutement}'/>" aria-describedby="besoin-budget-aide" data-besoin-requis><small id="besoin-budget-aide">Au moins effectif × salaire mensuel × 12. Les propositions d’embauche ne pourront pas le dépasser.</small></div>
          <div class="champ"><label for="besoin-date">Date souhaitée</label><input class="saisie" type="date" name="dateSouhaitee" id="besoin-date" value="<c:out value='${dateSouhaitee}'/>" data-besoin-requis></div>
        </div>
      </fieldset>
      <div class="formulaire-actions">
        <ui:bouton label="Soumettre" type="submit" style="btn-principal"/>
      </div>
    </ui:formulaire>
  </div></div>
</ui:layout>
