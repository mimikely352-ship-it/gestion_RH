<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="ui" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<ui:layout-public title="Devenir candidat">
  <main class="site-conteneur site-main" style="max-width:720px;">
    <div class="page-titre">
      <h1>Créer mon dossier de candidature</h1>
      <p class="texte-doux">Informations personnelles, CV, expériences et diplômes.</p>
    </div>

    <c:if test="${not empty erreur}"><ui:alerte type="erreur">${erreur}</ui:alerte></c:if>

    <ui:formulaire action="/candidats/inscription" enctype="multipart/form-data">
      <h3>Informations personnelles</h3>
      <div class="formulaire-grille">
        <ui:champ label="Email" name="email" type="email" value="${form.email}" requis="true"/>
        <ui:champ label="Mot de passe" name="motDePasse" type="password" requis="true"/>
        <ui:champ label="Nom" name="nom" value="${form.nom}" requis="true"/>
        <ui:champ label="Prénom" name="prenom" value="${form.prenom}" requis="true"/>
        <ui:champ label="Téléphone" name="telephone" value="${form.telephone}"/>
        <ui:champ label="Date de naissance" name="dateNaissance" type="date" value="${form.dateNaissance}"/>
        <ui:champ label="Lieu de naissance" name="lieuNaissance" value="${form.lieuNaissance}"/>
        <ui:champ label="CIN" name="cin" value="${form.cin}"/>
        <div class="plein"><ui:champ label="Adresse" name="adresse" value="${form.adresse}"/></div>
      </div>

      <h3 style="margin-top:var(--e-6);">CV</h3>
      <ui:champ label="Téléverser votre CV (PDF)" name="cv" type="file" aide="Format PDF uniquement, 5 Mo maximum."/>

      <h3 style="margin-top:var(--e-6);">Diplômes</h3>
      <ui:champ-liste label="Vos diplômes" name="diplomeIds" items="${diplomes}" valueProperty="id" labelProperty="libelle" multiple="true"/>

      <h3 style="margin-top:var(--e-6);">Expériences professionnelles</h3>
      <c:forEach var="i" begin="0" end="2">
        <div class="fiche" style="margin-bottom:var(--e-4);">
          <div class="fiche-corps formulaire-grille">
            <ui:champ label="Intitulé du poste" name="experiences[${i}].intitule"/>
            <ui:champ label="Entreprise" name="experiences[${i}].entreprise"/>
            <ui:champ label="Date de début" name="experiences[${i}].dateDebut" type="date"/>
            <ui:champ label="Date de fin" name="experiences[${i}].dateFin" type="date" aide="Laisser vide si poste actuel."/>
            <div class="plein"><ui:champ-texte label="Description" name="experiences[${i}].description"/></div>
          </div>
        </div>
      </c:forEach>

      <div class="formulaire-actions">
        <ui:bouton label="Créer mon compte" type="submit" style="btn-principal"/>
      </div>
    </ui:formulaire>
  </main>
</ui:layout-public>
