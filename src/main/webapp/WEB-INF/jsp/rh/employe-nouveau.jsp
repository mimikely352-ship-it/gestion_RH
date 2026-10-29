<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="ui" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<ui:layout title="Créer un employé" activeMenu="employe-nouveau">
  <h1>Créer un employé</h1>
  <p>Crée directement un compte Employé et son dossier, sans passer par une candidature.</p>
  <c:if test="${not empty erreur}"><ui:alerte type="erreur"><c:out value="${erreur}"/></ui:alerte></c:if>
  <div class="fiche" style="margin-top:var(--e-5);max-width:640px;"><div class="fiche-corps">
    <ui:formulaire action="/rh/employes/nouveau">
      <ui:champ label="Prénom" name="prenom" requis="true"/>
      <ui:champ label="Nom" name="nom" requis="true"/>
      <ui:champ label="Email" name="email" type="email" requis="true"/>
      <ui:champ label="Mot de passe initial" name="motDePasse" type="password" requis="true"
                 aide="8 caractères minimum. L'employé pourra le changer après connexion."/>
      <ui:champ-liste label="Poste" name="posteId" items="${postes}" valueProperty="id" labelProperty="intitule" requis="true"/>
      <ui:champ label="Date d’entrée" name="dateEntree" type="date"/>
      <ui:bouton label="Créer le compte" type="submit" style="btn-principal"/>
    </ui:formulaire>
  </div></div>
</ui:layout>
