<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="ui" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<ui:layout title="Nouvelle demande" activeMenu="mes-demandes">
  <h1>Nouvelle demande</h1>
  <c:if test="${not empty erreur}"><ui:alerte type="erreur">${erreur}</ui:alerte></c:if>
  <div class="fiche" style="margin-top:var(--e-5);max-width:640px;"><div class="fiche-corps">
    <ui:formulaire action="/employe/demandes">
      <div class="champ">
        <label class="champ-libelle" for="champ-type">Type de demande</label>
        <select class="saisie" id="champ-type" name="type" required>
          <c:forEach var="t" items="${types}"><option value="${t}"><c:out value="${t}"/></option></c:forEach>
        </select>
      </div>
      <ui:champ-liste label="Poste ciblé (recrutement / transfert / promotion)" name="posteCibleId"
                       items="${postes}" valueProperty="id" labelProperty="intitule"/>
      <ui:champ-texte label="Description / motif" name="description" requis="true"/>
      <div class="formulaire-actions">
        <ui:bouton label="Soumettre" type="submit" style="btn-principal"/>
      </div>
    </ui:formulaire>
  </div></div>
</ui:layout>
