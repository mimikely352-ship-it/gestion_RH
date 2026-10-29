<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="ui" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<ui:layout title="${test.titre}" activeMenu="offres">
  <h1>Niveau ${test.niveau} — <c:out value="${test.titre}"/></h1>

  <h3 style="margin-top:var(--e-6);">Questions</h3>
  <c:forEach var="q" items="${questions}" varStatus="st">
    <div class="fiche" style="margin-bottom:var(--e-3);"><div class="fiche-corps">
      <b>${st.index + 1}. <c:out value="${q.enonce}"/></b>
    </div></div>
  </c:forEach>

  <h3 style="margin-top:var(--e-6);">Résultats des candidats</h3>
  <c:if test="${empty passages}"><p>Aucun candidat n’a encore passé ce test.</p></c:if>
  <c:forEach var="p" items="${passages}">
    <div class="fiche" style="margin-bottom:var(--e-3);"><div class="fiche-corps">
      <div class="rangee" style="justify-content:space-between;">
        <span><c:out value="${p.candidature.candidat.utilisateur.email}"/> — note ${p.note}/20</span>
        <ui:statut valeur="${p.decisionRh}"/>
      </div>
      <details>
        <summary>Voir les réponses</summary>
        <c:forEach var="r" items="${reponsesParPassage[p.id]}">
          <p><b><c:out value="${r.question.enonce}"/></b></p>
          <p class="texte-doux"><c:out value="${not empty r.choix ? r.choix.libelle : '(sans réponse)'}"/>
            <c:if test="${not empty r.choix}"> — <ui:statut valeur="${r.choix.estCorrect ? 'CORRECT' : 'INCORRECT'}"/></c:if>
          </p>
        </c:forEach>
      </details>
      <c:if test="${p.decisionRh == 'EN_ATTENTE' and p.datePassage != null}">
        <div class="rangee" style="margin-top:var(--e-2);">
          <ui:formulaire action="/rh/passages/${p.id}/decider" cssClass="rangee">
            <input type="hidden" name="decision" value="REUSSI"/>
            <ui:bouton label="Valider" type="submit" style="btn-valider"/>
          </ui:formulaire>
          <ui:formulaire action="/rh/passages/${p.id}/decider" cssClass="rangee">
            <input type="hidden" name="decision" value="ECHEC"/>
            <ui:bouton label="Échec" type="submit" style="btn-refuser"/>
          </ui:formulaire>
        </div>
      </c:if>
    </div></div>
  </c:forEach>

  <div class="fiche" style="margin-top:var(--e-5);max-width:640px;"><div class="fiche-corps">
    <h3>Ajouter une question (QCM, 4 choix max)</h3>
    <ui:formulaire action="/rh/tests/${test.id}/questions">
      <ui:champ-texte label="Énoncé" name="enonce" requis="true"/>
      <c:forEach var="i" begin="1" end="4">
        <div class="rangee">
          <input type="radio" name="correct" value="${i}" required/>
          <div style="flex:1;"><ui:champ label="Choix ${i}" name="choix${i}"/></div>
        </div>
      </c:forEach>
      <p class="texte-petit texte-doux">Cochez le bouton radio du choix correct.</p>
      <ui:bouton label="Ajouter la question" type="submit" style="btn-principal"/>
    </ui:formulaire>
  </div></div>
</ui:layout>
