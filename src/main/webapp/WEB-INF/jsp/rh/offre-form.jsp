<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="ui" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<ui:layout title="Nouvelle offre" activeMenu="offres">
  <h1>Nouvelle offre d'emploi</h1>
  <div class="fiche" style="margin-top:var(--e-5);max-width:720px;">
    <div class="fiche-corps">
      <ui:formulaire action="/rh/offres">
        <c:if test="${not empty demandeId}"><input type="hidden" name="demandeId" value="${demandeId}"/></c:if>
        <ui:champ-liste label="Poste" name="posteId" items="${postes}" valueProperty="id" labelProperty="intitule" requis="true"/>
        <ui:champ label="Titre de l'offre" name="titre" requis="true"/>
        <ui:champ-texte label="Description" name="description" requis="true"/>
        <div class="formulaire-grille">
          <ui:champ label="Nombre de postes" name="nbPostes" type="number" value="1" requis="true"/>
          <ui:champ label="Expérience minimale (mois)" name="experienceMinMois" type="number" aide="Laisser vide si aucune exigence."/>
          <ui:champ label="Seuil d'acceptation automatique (%)" name="seuilAcceptation" type="number" value="60" requis="true"/>
          <ui:champ label="Date limite" name="dateLimite" type="date"/>
        </div>
        <ui:champ-liste label="Diplômes requis" name="diplomeIds" items="${diplomes}" valueProperty="id" labelProperty="libelle" multiple="true"
                         aide="Aucune sélection = pas d'exigence de diplôme."/>
        <div class="formulaire-actions">
          <ui:bouton label="Créer l'offre (brouillon)" type="submit" style="btn-principal"/>
        </div>
      </ui:formulaire>
    </div>
  </div>
</ui:layout>
