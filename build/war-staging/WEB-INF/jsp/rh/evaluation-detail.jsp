<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="ui" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<ui:layout title="${evaluation.titre}" activeMenu="candidatures">
  <div class="rangee" style="justify-content:space-between;">
    <h1><c:out value="${evaluation.titre}"/> (<c:out value="${evaluation.format}"/>)</h1>
    <ui:statut valeur="${evaluation.decisionRh}"/>
  </div>

  <c:if test="${not empty reponses}">
    <div class="fiche" style="margin-top:var(--e-5);max-width:640px;"><div class="fiche-corps">
      <h3>Réponses du candidat</h3>
      <c:forEach var="r" items="${reponses}" varStatus="st">
        <div style="margin-bottom:var(--e-3);">
          <p><b>${st.index + 1}. <c:out value="${r.question.enonce}"/></b></p>
          <c:choose>
            <c:when test="${evaluation.format == 'QCM'}">
              <p><c:out value="${not empty r.choix ? r.choix.libelle : '(sans réponse)'}"/>
                <c:if test="${not empty r.choix}"> — <ui:statut valeur="${r.choix.estCorrect ? 'CORRECT' : 'INCORRECT'}"/></c:if>
              </p>
            </c:when>
            <c:otherwise>
              <p class="texte-doux"><c:out value="${not empty r.texteLibre ? r.texteLibre : '(sans réponse)'}"/></p>
            </c:otherwise>
          </c:choose>
        </div>
      </c:forEach>
    </div></div>

    <c:if test="${evaluation.decisionRh == 'EN_ATTENTE'}">
      <div class="fiche" style="margin-top:var(--e-5);max-width:640px;"><div class="fiche-corps">
        <h3>Décision</h3>
        <div class="rangee">
          <ui:formulaire action="/rh/evaluations/${evaluation.id}/decider" cssClass="rangee">
            <input type="hidden" name="decision" value="ACCEPTE"/>
            <ui:bouton label="Accepter" type="submit" style="btn-valider"/>
          </ui:formulaire>
          <ui:formulaire action="/rh/evaluations/${evaluation.id}/decider" cssClass="rangee">
            <input type="hidden" name="decision" value="REFUSE"/>
            <ui:bouton label="Refuser" type="submit" style="btn-refuser"/>
          </ui:formulaire>
        </div>
      </div></div>
    </c:if>
  </c:if>

  <div class="fiche" style="margin-top:var(--e-5);max-width:640px;"><div class="fiche-corps">
    <h3>Questions</h3>
    <c:forEach var="q" items="${questions}" varStatus="st">
      <p><b>${st.index + 1}. <c:out value="${q.enonce}"/></b></p>
    </c:forEach>
    <h3 style="margin-top:var(--e-5);">Ajouter une question</h3>
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
