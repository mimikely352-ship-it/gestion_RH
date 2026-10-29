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
          <p><b>${st.index + 1}. <c:out value="${r.question.enonce}"/></b> <span class="texte-petit texte-doux">(${r.question.type})</span></p>
          <c:choose>
            <c:when test="${r.question.type == 'QCM'}">
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
      <p><b>${st.index + 1}. <c:out value="${q.enonce}"/></b> <span class="texte-petit texte-doux">(${q.type})</span></p>
    </c:forEach>
    <h3 style="margin-top:var(--e-5);">Ajouter une question</h3>
    <p class="texte-petit texte-doux">Chaque question peut être un QCM ou une zone de texte libre, indépendamment des autres.</p>
    <ui:formulaire action="/rh/evaluations/${evaluation.id}/questions" cssClass="pile question-mixte">
      <ui:champ-texte label="Énoncé" name="enonce" requis="true"/>
      <div class="rangee">
        <label class="case"><input type="radio" name="type" value="QCM" checked onchange="document.querySelector('.question-mixte .choix-qcm').hidden=false"/> QCM</label>
        <label class="case"><input type="radio" name="type" value="TEXTE" onchange="document.querySelector('.question-mixte .choix-qcm').hidden=true"/> Zone de texte libre</label>
      </div>
      <div class="choix-qcm">
        <c:forEach var="i" begin="1" end="4">
          <div class="rangee">
            <input type="radio" name="correct" value="${i}"/>
            <div style="flex:1;"><ui:champ label="Choix ${i}" name="choix${i}"/></div>
          </div>
        </c:forEach>
        <p class="texte-petit texte-doux">Cochez le bouton radio du choix correct (QCM uniquement).</p>
      </div>
      <ui:bouton label="Ajouter la question" type="submit" style="btn-principal"/>
    </ui:formulaire>
  </div></div>

  <a class="btn btn-secondaire btn-petit" style="margin-top:var(--e-5);"
     href="${pageContext.request.contextPath}/rh/candidatures/${evaluation.candidature.id}">Retour à la candidature</a>
</ui:layout>
