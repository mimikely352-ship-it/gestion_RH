<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="ui" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<ui:layout title="Candidature" activeMenu="candidatures">
  <div class="rangee" style="justify-content:space-between;">
    <div>
      <h1><c:out value="${candidature.candidatNomComplet}"/></h1>
      <p class="texte-doux">Candidature pour <c:out value="${candidature.offreTitre}"/></p>
    </div>
    <ui:statut valeur="${candidature.statut}"/>
  </div>

  <div class="grille-2" style="margin-top:var(--e-6);">
    <div class="fiche"><div class="fiche-corps">
      <h3>Profil</h3>
      <dl class="infos">
        <dt>Email</dt><dd><c:out value="${candidature.candidat.utilisateur.email}"/></dd>
        <dt>Téléphone</dt><dd><c:out value="${candidature.candidat.telephone}"/></dd>
        <dt>CIN</dt><dd><c:out value="${candidature.candidat.cin}"/></dd>
        <dt>Score de matching</dt><dd class="donnee">${candidature.scoreMatching} / 100</dd>
      </dl>
      <c:if test="${not empty cv}">
        <a class="btn btn-secondaire btn-petit" style="margin-top:var(--e-3);"
           href="${pageContext.request.contextPath}/documents/${cv.id}/telecharger" target="_blank">Voir le CV</a>
      </c:if>
      <c:if test="${empty cv}">
        <p class="texte-doux">Aucun CV enregistré avec cette candidature. Le CV historique des anciens dossiers peut être indisponible.</p>
      </c:if>
      <h4>Pièces complémentaires du dossier</h4>
      <c:if test="${empty candidature.piecesComplementaires}"><p class="texte-doux">Aucune pièce complémentaire déposée.</p></c:if>
      <ul><c:forEach var="piece" items="${candidature.piecesComplementaires}">
        <li><a href="${pageContext.request.contextPath}/documents/${piece.id}/telecharger" target="_blank" rel="noopener"><c:out value="${piece.libelle}"/> — <c:out value="${piece.nomFichier}"/></a></li>
      </c:forEach></ul>
      <h4>Diplômes déclarés</h4>
      <ul>
        <c:forEach var="d" items="${candidature.diplomes}"><li><c:out value="${d.libelle}"/></li></c:forEach>
      </ul>
      <h4 style="margin-top:var(--e-5);">Expériences</h4>
      <ul>
        <c:forEach var="e" items="${experiences}">
          <li><c:out value="${e.intitule}"/> — <c:out value="${e.entreprise}"/> (<c:out value="${e.dateDebut}"/> → <c:out value="${e.dateFin}"/>)</li>
        </c:forEach>
      </ul>
    </div></div>

    <div class="fiche"><div class="fiche-corps">
      <c:if test="${candidature.statut == 'RECUE'}">
        <h3>Présélection RH</h3>
        <ui:formulaire action="/rh/candidatures/${candidature.id}/preselection">
          <input type="hidden" name="retenir" value="true"/>
          <ui:bouton label="Retenir pour entretien" type="submit" style="btn-valider"/>
        </ui:formulaire>
        <ui:formulaire action="/rh/candidatures/${candidature.id}/preselection">
          <input type="hidden" name="retenir" value="false"/>
          <ui:bouton label="Ne pas retenir" type="submit" style="btn-refuser"/>
        </ui:formulaire>
      </c:if>
      <h3>Entretien</h3>
      <c:forEach var="ent" items="${entretiens}">
        <p><c:out value="${ent.dateHeure}"/> — <c:out value="${ent.lieu}"/> — <ui:statut valeur="${ent.decisionRh}"/></p>
        <c:if test="${ent.decisionRh == 'EN_ATTENTE'}">
          <ui:formulaire action="/rh/entretiens/${ent.id}/decider" cssClass="rangee">
            <input type="hidden" name="decision" value="ACCEPTE"/>
            <ui:bouton label="Accepter" type="submit" style="btn-valider"/>
          </ui:formulaire>
          <ui:formulaire action="/rh/entretiens/${ent.id}/decider" cssClass="rangee">
            <input type="hidden" name="decision" value="REFUSE"/>
            <ui:bouton label="Refuser" type="submit" style="btn-refuser"/>
          </ui:formulaire>
        </c:if>
      </c:forEach>
      <c:if test="${candidature.statut == 'ACCEPTEE_ENTRETIEN'}">
        <h4 style="margin-top:var(--e-4);">Planifier un entretien</h4>
        <ui:formulaire action="/rh/candidatures/${candidature.id}/entretiens">
          <ui:champ label="Date et heure" name="dateHeure" type="datetime-local" requis="true"/>
          <ui:champ label="Lieu" name="lieu"/>
          <ui:bouton label="Planifier" type="submit" style="btn-principal btn-petit"/>
        </ui:formulaire>
      </c:if>
    </div></div>
  </div>

  <div class="grille-2" style="margin-top:var(--e-6);">
    <div class="fiche"><div class="fiche-corps">
      <h3>Tests</h3>
      <p class="texte-doux">Gestion des tests par niveau pour l'offre.</p>
      <a class="btn btn-secondaire btn-petit" href="${pageContext.request.contextPath}/rh/offres/${candidature.offre.id}/tests">Gérer les tests</a>
      <c:forEach var="p" items="${passages}">
        <div class="rangee" style="margin-top:var(--e-3);justify-content:space-between;">
          <span><c:out value="${p.test.titre}"/> — note ${p.note}/20</span>
          <ui:statut valeur="${p.decisionRh}"/>
        </div>
        <c:if test="${p.decisionRh == 'EN_ATTENTE' and p.datePassage != null}">
          <ui:formulaire action="/rh/passages/${p.id}/decider" cssClass="rangee">
            <input type="hidden" name="decision" value="REUSSI"/>
            <ui:bouton label="Réussi" type="submit" style="btn-valider btn-petit"/>
          </ui:formulaire>
          <ui:formulaire action="/rh/passages/${p.id}/decider" cssClass="rangee">
            <input type="hidden" name="decision" value="ECHEC"/>
            <ui:bouton label="Échec" type="submit" style="btn-refuser btn-petit"/>
          </ui:formulaire>
        </c:if>
      </c:forEach>
    </div></div>

    <div class="fiche"><div class="fiche-corps">
      <h3>Évaluations</h3>
      <c:if test="${candidature.statut == 'EN_EVALUATION'}">
        <ui:formulaire action="/rh/candidatures/${candidature.id}/evaluations" cssClass="rangee">
          <ui:champ label="Titre" name="titre" requis="true"/>
          <select class="saisie" name="format"><option value="QCM">QCM</option><option value="TEXTE">Texte libre</option></select>
          <ui:bouton label="Créer" type="submit" style="btn-principal btn-petit"/>
        </ui:formulaire>
      </c:if>
      <c:forEach var="ev" items="${evaluations}">
        <div class="rangee" style="margin-top:var(--e-3);justify-content:space-between;">
          <a href="${pageContext.request.contextPath}/rh/evaluations/${ev.id}"><c:out value="${ev.titre}"/> (<c:out value="${ev.format}"/>)</a>
          <ui:statut valeur="${ev.decisionRh}"/>
        </div>
        <c:if test="${ev.decisionRh == 'EN_ATTENTE' and ev.dateEvaluation != null}">
          <ui:formulaire action="/rh/evaluations/${ev.id}/decider" cssClass="rangee">
            <input type="hidden" name="decision" value="ACCEPTE"/>
            <ui:bouton label="Accepter" type="submit" style="btn-valider btn-petit"/>
          </ui:formulaire>
          <ui:formulaire action="/rh/evaluations/${ev.id}/decider" cssClass="rangee">
            <input type="hidden" name="decision" value="REFUSE"/>
            <ui:bouton label="Refuser" type="submit" style="btn-refuser btn-petit"/>
          </ui:formulaire>
        </c:if>
      </c:forEach>
    </div></div>
  </div>

  <c:if test="${candidature.statut == 'RETENUE'}">
    <div class="fiche" style="margin-top:var(--e-6);"><div class="fiche-corps">
      <h3>Acceptation finale</h3>
      <p>Le service du poste est repris automatiquement. S’il manque, choisissez le service d’accueil ci-dessous.</p>
      <ui:formulaire action="/rh/candidatures/${candidature.id}/embauche" cssClass="rangee">
        <select class="saisie" name="role">
          <option value="EMPLOYE">Employé</option>
          <option value="CHEF_SERVICE">Chef de service</option>
          <option value="DIRECTION">Direction</option>
        </select>
        <ui:champ-liste label="Service d’accueil si le poste n’en possède pas" name="serviceId" items="${services}" valueProperty="id" labelProperty="nom"/>
        <ui:bouton label="Attribuer le poste et embaucher" type="submit" style="btn-valider"/>
      </ui:formulaire>
    </div></div>
  </c:if>
</ui:layout>
