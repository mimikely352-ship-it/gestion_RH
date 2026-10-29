<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="ui" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<ui:layout title="Organisation" activeMenu="organisation">
  <h1>Organisation</h1>
  <c:if test="${not empty succes}"><p role="status"><c:out value="${succes}"/></p></c:if>
  <c:if test="${not empty erreur}"><ui:alerte type="erreur"><c:out value="${erreur}"/></ui:alerte></c:if>
  <p>Créez la hiérarchie (direction → département → service → section → poste) et désignez, si besoin, un responsable pour chaque direction, département ou service. Un responsable peut être laissé vide.</p>

  <div class="fiche" style="margin-top:var(--e-5);"><div class="fiche-corps">
    <h2>Directions</h2>
    <table class="tableau">
      <thead><tr><th scope="col">Nom</th><th scope="col">Directeur</th></tr></thead>
      <tbody><c:forEach var="d" items="${directions}"><tr>
        <td><c:out value="${d.nom}"/></td>
        <td>
          <ui:formulaire action="/rh/organisation/directions/${d.id}/directeur" cssClass="rangee">
            <ui:champ-liste label="" name="directeurId" items="${utilisateurs}" valueProperty="id" labelProperty="email"
                             selected="${d.directeur != null ? d.directeur.id : null}"/>
            <ui:bouton label="Enregistrer" type="submit" style="btn-secondaire btn-petit"/>
          </ui:formulaire>
        </td>
      </tr></c:forEach></tbody>
    </table>
    <h3 style="margin-top:var(--e-5);">Créer une direction</h3>
    <ui:formulaire action="/rh/organisation/directions">
      <ui:champ label="Nom" name="nom" requis="true"/>
      <ui:champ-liste label="Directeur" name="directeurId" items="${utilisateurs}" valueProperty="id" labelProperty="email"/>
      <ui:bouton label="Créer" type="submit" style="btn-principal"/>
    </ui:formulaire>
  </div></div>

  <div class="fiche" style="margin-top:var(--e-5);"><div class="fiche-corps">
    <h2>Départements</h2>
    <table class="tableau">
      <thead><tr><th scope="col">Nom</th><th scope="col">Direction</th><th scope="col">Chef de département</th></tr></thead>
      <tbody><c:forEach var="dep" items="${departements}"><tr>
        <td><c:out value="${dep.nom}"/></td>
        <td><c:out value="${dep.direction.nom}"/></td>
        <td>
          <ui:formulaire action="/rh/organisation/departements/${dep.id}/chef" cssClass="rangee">
            <ui:champ-liste label="" name="chefDepartementId" items="${utilisateurs}" valueProperty="id" labelProperty="email"
                             selected="${dep.chefDepartement != null ? dep.chefDepartement.id : null}"/>
            <ui:bouton label="Enregistrer" type="submit" style="btn-secondaire btn-petit"/>
          </ui:formulaire>
        </td>
      </tr></c:forEach></tbody>
    </table>
    <h3 style="margin-top:var(--e-5);">Créer un département</h3>
    <ui:formulaire action="/rh/organisation/departements">
      <ui:champ label="Nom" name="nom" requis="true"/>
      <ui:champ-liste label="Direction" name="directionId" items="${directions}" valueProperty="id" labelProperty="nom" requis="true"/>
      <ui:champ-liste label="Chef de département" name="chefDepartementId" items="${utilisateurs}" valueProperty="id" labelProperty="email"/>
      <ui:bouton label="Créer" type="submit" style="btn-principal"/>
    </ui:formulaire>
  </div></div>

  <div class="fiche" style="margin-top:var(--e-5);"><div class="fiche-corps">
    <h2>Services</h2>
    <table class="tableau">
      <thead><tr><th scope="col">Nom</th><th scope="col">Département</th><th scope="col">Chef de service</th></tr></thead>
      <tbody><c:forEach var="s" items="${servicesListe}"><tr>
        <td><c:out value="${s.nom}"/></td>
        <td><c:out value="${s.departement.nom}"/></td>
        <td>
          <ui:formulaire action="/rh/organisation/services/${s.id}/chef" cssClass="rangee">
            <ui:champ-liste label="" name="chefServiceId" items="${utilisateurs}" valueProperty="id" labelProperty="email"
                             selected="${s.chefService != null ? s.chefService.id : null}"/>
            <ui:bouton label="Enregistrer" type="submit" style="btn-secondaire btn-petit"/>
          </ui:formulaire>
        </td>
      </tr></c:forEach></tbody>
    </table>
    <h3 style="margin-top:var(--e-5);">Créer un service</h3>
    <ui:formulaire action="/rh/organisation/services">
      <ui:champ label="Nom" name="nom" requis="true"/>
      <ui:champ-liste label="Département" name="departementId" items="${departements}" valueProperty="id" labelProperty="nom" requis="true"/>
      <ui:champ-liste label="Chef de service" name="chefServiceId" items="${utilisateurs}" valueProperty="id" labelProperty="email"/>
      <ui:bouton label="Créer" type="submit" style="btn-principal"/>
    </ui:formulaire>
  </div></div>

  <div class="fiche" style="margin-top:var(--e-5);"><div class="fiche-corps">
    <h2>Sections</h2>
    <table class="tableau">
      <thead><tr><th scope="col">Nom</th><th scope="col">Service</th></tr></thead>
      <tbody><c:forEach var="sec" items="${sections}"><tr>
        <td><c:out value="${sec.nom}"/></td>
        <td><c:out value="${sec.service.nom}"/></td>
      </tr></c:forEach></tbody>
    </table>
    <h3 style="margin-top:var(--e-5);">Créer une section</h3>
    <ui:formulaire action="/rh/organisation/sections">
      <ui:champ label="Nom" name="nom" requis="true"/>
      <ui:champ-liste label="Service" name="serviceId" items="${servicesListe}" valueProperty="id" labelProperty="nom" requis="true"/>
      <ui:bouton label="Créer" type="submit" style="btn-principal"/>
    </ui:formulaire>
  </div></div>

  <div class="fiche" style="margin-top:var(--e-5);"><div class="fiche-corps">
    <h2>Postes</h2>
    <table class="tableau">
      <thead><tr><th scope="col">Intitulé</th><th scope="col">Section</th><th scope="col">Mobilité ouverte</th></tr></thead>
      <tbody><c:forEach var="p" items="${postes}"><tr>
        <td><c:out value="${p.intitule}"/></td>
        <td><c:out value="${p.section != null ? p.section.nom : 'Sans rattachement'}"/></td>
        <td><c:out value="${p.mobiliteOuverte ? 'Oui' : 'Non'}"/></td>
      </tr></c:forEach></tbody>
    </table>
    <h3 style="margin-top:var(--e-5);">Créer un poste</h3>
    <ui:formulaire action="/rh/organisation/postes">
      <ui:champ label="Intitulé" name="intitule" requis="true"/>
      <ui:champ label="Catégorie" name="categorie"/>
      <ui:champ-liste label="Section" name="sectionId" items="${sections}" valueProperty="id" labelProperty="nom"/>
      <label class="case"><input type="checkbox" name="mobiliteOuverte" value="true" checked/> Ouvert à la mobilité interne</label>
      <ui:bouton label="Créer" type="submit" style="btn-principal"/>
    </ui:formulaire>
  </div></div>
</ui:layout>
