<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="ui" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<ui:layout title="${evaluation.titre}" activeMenu="candidatures">
  <h1><c:out value="${evaluation.titre}"/> (<c:out value="${evaluation.format}"/>)</h1>

  <div class="fiche" style="margin-top:var(--e-5);max-width:640px;"><div class="fiche-corps">
    <h3>Ajouter une question</h3>
    <ui:formulaire action="/rh/evaluations/${evaluation.id}/questions">
      <ui:champ-texte label="Énoncé" name="enonce" requis="true"/>
      <c:if test="${evaluation.format == 'QCM'}">
        <c:forEach var="i" begin="1" end="4">
          <div class="rangee">
            <input type="radio" name="correct" value="${i}"/>
            <div style="flex:1;"><ui:champ label="Choix ${i}" name="choix${i}"/></div>
          </div>
        </c:forEach>
        <p class="texte-petit texte-doux">Cochez le bouton radio du choix correct.</p>
      </c:if>
      <ui:bouton label="Ajouter la question" type="submit" style="btn-principal"/>
    </ui:formulaire>
  </div></div>

  <a class="btn btn-secondaire btn-petit" style="margin-top:var(--e-5);"
     href="${pageContext.request.contextPath}/rh/candidatures/${evaluation.candidature.id}">Retour à la candidature</a>
</ui:layout>
