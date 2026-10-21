<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="ui" tagdir="/WEB-INF/tags" %>
<ui:layout title="Mes ordres" activeMenu="ordres">
  <h1>Mes ordres</h1>
  <ui:list data="${ordres}" columns="description,rhEmail,delaiJours,statut,dateLimite"
           columnLabels="Description,RH destinataire,Délai (j),Statut,Échéance" messageVide="Aucun ordre émis."/>

  <div class="fiche" style="margin-top:var(--e-6);max-width:640px;"><div class="fiche-corps">
    <h3>Émettre un ordre</h3>
    <ui:formulaire action="/dg/ordres">
      <ui:champ-liste label="RH destinataire" name="rhId" items="${rhs}" valueProperty="id" labelProperty="email" requis="true"/>
      <ui:champ-texte label="Description" name="description" requis="true"/>
      <ui:champ label="Délai d'exécution (jours)" name="delaiJours" type="number" value="7" requis="true"/>
      <ui:bouton label="Émettre l'ordre" type="submit" style="btn-principal"/>
    </ui:formulaire>
  </div></div>
</ui:layout>
