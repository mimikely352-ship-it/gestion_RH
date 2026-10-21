<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="ui" tagdir="/WEB-INF/tags" %>
<ui:layout title="Entretiens" activeMenu="entretiens">
  <h1>Entretiens</h1>
  <ui:list data="${entretiens}" columns="candidatNomComplet,offreTitre,dateHeure,lieu,statut,decisionRh"
           columnLabels="Candidat,Offre,Date,Lieu,Statut,Décision" messageVide="Aucun entretien planifié.">
    <ui:action label="Voir la candidature" url="/rh/candidatures/{id}" idProperty="candidatureId"/>
  </ui:list>
</ui:layout>
