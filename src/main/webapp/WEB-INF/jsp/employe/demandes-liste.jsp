<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="ui" tagdir="/WEB-INF/tags" %>
<ui:layout title="Mes demandes" activeMenu="mes-demandes">
  <div class="rangee" style="justify-content:space-between;">
    <h1>Mes demandes</h1>
    <ui:bouton label="Nouvelle demande" href="/employe/demandes/nouvelle" style="btn-principal"/>
  </div>
  <ui:list data="${demandes}" columns="type,description,statut,dateCreation"
           columnLabels="Type,Description,Statut,Date" messageVide="Vous n'avez soumis aucune demande."/>
</ui:layout>
