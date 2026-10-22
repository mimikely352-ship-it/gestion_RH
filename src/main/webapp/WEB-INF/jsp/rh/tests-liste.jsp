<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="ui" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<ui:layout title="Tests de l'offre" activeMenu="offres">
  <h1>Tests (QCM par niveau)</h1>
  <ui:list data="${tests}" columns="niveau,titre" columnLabels="Niveau,Titre" messageVide="Aucun test créé pour cette offre.">
    <ui:action label="Gérer" url="/rh/tests/{id}" row="${row}"/>
  </ui:list>

  <div class="fiche" style="margin-top:var(--e-6);max-width:480px;"><div class="fiche-corps">
    <h3>Nouveau test</h3>
    <ui:formulaire action="/rh/offres/${offreId}/tests">
      <ui:champ label="Niveau (1 à 5)" name="niveau" type="number" requis="true"/>
      <ui:champ label="Titre" name="titre" requis="true"/>
      <ui:bouton label="Créer" type="submit" style="btn-principal"/>
    </ui:formulaire>
  </div></div>
</ui:layout>
