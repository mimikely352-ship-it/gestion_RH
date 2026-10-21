<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="ui" tagdir="/WEB-INF/tags" %>
<ui:layout title="Offres d'emploi" activeMenu="offres">
  <div class="rangee" style="justify-content:space-between;margin-bottom:var(--e-5);">
    <h1>Offres d'emploi</h1>
    <ui:bouton label="Nouvelle offre" href="/rh/offres/nouvelle" style="btn-principal"/>
  </div>
  <ui:list data="${offres}" columns="titre,posteIntitule,statut,nbPostes,dateLimite"
           columnLabels="Titre,Poste,Statut,Postes,Date limite" messageVide="Aucune offre créée.">
    <ui:action label="Voir" url="/rh/offres/{id}"/>
  </ui:list>
</ui:layout>
