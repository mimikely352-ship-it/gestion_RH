<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="ui" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<ui:layout title="Rattachement des employés" activeMenu="rattachements">
  <h1>Rattachement des employés</h1>
  <p>Complétez le service actuel des employés dont l’affectation manque. Le responsable du service recevra leurs demandes internes.</p>
  <c:if test="${not empty erreur}"><ui:alerte type="erreur"><c:out value="${erreur}"/></ui:alerte></c:if>
  <c:if test="${not empty succes}"><p role="status"><c:out value="${succes}"/></p></c:if>
  <c:choose>
    <c:when test="${empty employes}"><p class="fiche fiche-corps">Tous les employés actifs ont un service de rattachement.</p></c:when>
    <c:otherwise>
      <div class="fiche"><div class="fiche-corps">
        <h2>Compléter une affectation</h2>
        <p>Choisissez le service où travaille actuellement l’employé. Son poste souhaité sera indiqué dans sa demande de transfert.</p>
        <ui:formulaire action="/rh/employes/rattachements">
          <ui:champ-liste label="Employé sans service" name="employeId" items="${employes}" valueProperty="id" labelProperty="email" requis="true"/>
          <div class="champ">
            <label class="champ-libelle" for="service-rattachement">Service actuel et responsable</label>
            <select class="saisie" id="service-rattachement" name="serviceId" required>
              <option value="">Choisir le service actuel</option>
              <c:forEach var="s" items="${services}">
                <c:if test="${s.chefService != null and s.chefService.actif and s.chefService.role == 'CHEF_SERVICE'}">
                  <option value="${s.id}"><c:out value="${s.nom}"/> — <c:out value="${s.chefService.email}"/></option>
                </c:if>
              </c:forEach>
            </select>
            <p class="champ-aide">Seuls les services ayant un chef de service actif sont proposés.</p>
          </div>
          <ui:bouton label="Enregistrer le service actuel" type="submit" style="btn-principal"/>
        </ui:formulaire>
      </div></div>
    </c:otherwise>
  </c:choose>
</ui:layout>
