<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="ui" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<ui:layout title="Nouvelle demande" activeMenu="mes-demandes">
  <h1>Nouvelle demande</h1>
  <p>Pour un transfert : envoyez votre demande aux RH. Ils décident directement et confirment ensuite le changement de poste effectif.</p>
  <c:if test="${not empty avertissementRattachement}"><p><c:out value="${avertissementRattachement}"/></p></c:if>
  <c:if test="${not empty erreur}"><ui:alerte type="erreur"><c:out value="${erreur}"/></ui:alerte></c:if>
  <div class="fiche" style="margin-top:var(--e-5);max-width:640px;"><div class="fiche-corps">
    <ui:formulaire action="/employe/demandes">
      <div class="champ">
        <label class="champ-libelle" for="champ-type">Type de demande</label>
        <select class="saisie" id="champ-type" name="type" required>
          <c:forEach var="t" items="${types}"><option value="${t}" ${typeChoisi == t ? 'selected' : ''}><c:out value="${t}"/></option></c:forEach>
        </select>
      </div>
      <ui:champ-liste label="Poste ciblé (recrutement / transfert / promotion)" name="posteCibleId"
                       items="${postes}" valueProperty="id" labelProperty="intitule" selected="${posteChoisi}"/>
      <ui:champ-texte label="Description / motif" name="description" value="${description}" requis="true"/>
      <div class="formulaire-actions">
        <ui:bouton label="Soumettre" type="submit" style="btn-principal"/>
      </div>
    </ui:formulaire>
  </div></div>
</ui:layout>
