<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="ui" tagdir="/WEB-INF/tags" %>
<ui:layout title="Candidatures" activeMenu="candidatures">
  <h1>Toutes les candidatures</h1>
  <ui:list data="${candidatures}" columns="candidatNomComplet,offreTitre,scoreMatching,statut,dateCandidature"
           columnLabels="Candidat,Offre,Score,Statut,Date" messageVide="Aucune candidature reçue.">
    <ui:action label="Voir" url="/rh/candidatures/{id}" row="${row}"/>
  </ui:list>
</ui:layout>
