<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="ui" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<ui:layout title="Mobilité interne" activeMenu="mobilite">
  <h1>Postes et mobilité interne</h1>
  <p>Ouvrez ou fermez un poste aux demandes de transfert, indépendamment des offres publiques. La section définit le service de destination.</p>
  <c:if test="${not empty succes}"><p role="status"><c:out value="${succes}"/></p></c:if>
  <c:forEach var="p" items="${postes}">
    <div class="fiche"><div class="fiche-corps"><h2><c:out value="${p.intitule}"/></h2>
      <ui:formulaire action="/rh/mobilite">
        <input type="hidden" name="posteId" value="${p.id}">
        <ui:champ-liste label="Section et service de destination" name="sectionId" items="${sections}" valueProperty="id" labelProperty="nom" selected="${p.section.id}" requis="true"/>
        <label><input type="checkbox" name="ouverte" value="true" ${p.mobiliteOuverte ? 'checked' : ''}> Ouvert à la mobilité interne</label>
        <ui:bouton label="Enregistrer" type="submit" style="btn-principal"/>
      </ui:formulaire>
    </div></div>
  </c:forEach>
</ui:layout>
