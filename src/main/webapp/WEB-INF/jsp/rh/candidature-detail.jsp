<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="ui" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<ui:layout title="Candidature" activeMenu="candidatures">
  <c:if test="${not empty succes}"><p role="status"><c:out value="${succes}"/></p></c:if>
  <c:if test="${not empty erreur}"><ui:alerte type="erreur"><c:out value="${erreur}"/></ui:alerte></c:if>
  <div class="rangee" style="justify-content:space-between;">
    <div>
      <h1><c:out value="${candidature.candidatNomComplet}"/></h1>
      <p class="texte-doux">Candidature pour <c:out value="${candidature.offreTitre}"/></p>
    </div>
    <ui:statut valeur="${candidature.statut}"/>
  </div>
  <c:if test="${not empty prochaineEtape}">
    <ui:alerte type="info"><strong>Prochaine étape :</strong> <c:out value="${prochaineEtape}"/></ui:alerte>
  </c:if>

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
        <p class="texte-doux">Aucun CV disponible.</p>
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
      <h3>Entretiens</h3>
      <c:if test="${empty entretiens}"><p class="texte-doux">Aucun entretien planifié.</p></c:if>
      <c:forEach var="ent" items="${entretiens}" varStatus="rang">
        <div class="entretien" style="margin-top:var(--e-3);">
          <div class="rangee" style="justify-content:space-between;">
            <strong>Entretien n° ${rang.index + 1}</strong>
            <c:choose>
              <c:when test="${ent.decisionRh == 'EN_ATTENTE'}"><ui:statut valeur="PLANIFIE"/></c:when>
              <c:when test="${ent.decisionRh == 'REFUSE'}"><ui:statut valeur="REFUSE"/></c:when>
              <c:otherwise><ui:statut valeur="ACCEPTE"/></c:otherwise>
            </c:choose>
          </div>
          <dl class="infos">
            <div><dt>Date</dt><dd><ui:date valeur="${ent.dateHeure}" heure="true"/></dd></div>
            <div><dt>Lieu</dt><dd><c:out value="${ent.lieu}" default="Non précisé"/></dd></div>
            <c:if test="${ent.decisionRh != 'EN_ATTENTE'}">
              <div><dt>Suite donnée</dt><dd><c:choose>
                <c:when test="${ent.decisionRh == 'REFUSE'}">Candidature refusée</c:when>
                <c:when test="${not rang.last}">Entretien suivant</c:when>
                <c:when test="${candidature.statut == 'EN_ENTRETIEN'}">Entretien suivant (à planifier)</c:when>
                <c:otherwise>Passage aux tests</c:otherwise>
              </c:choose></dd></div>
              <div><dt>Commentaire</dt><dd><c:out value="${ent.commentaire}" default="—"/></dd></div>
            </c:if>
          </dl>
          <c:if test="${ent.decisionRh == 'EN_ATTENTE' and candidature.statut == 'EN_ENTRETIEN'}">
            <ui:formulaire action="/rh/entretiens/${ent.id}/decider">
              <div class="champ"><label for="commentaire-${ent.id}">Commentaire (facultatif)</label>
                <textarea class="saisie" id="commentaire-${ent.id}" name="commentaire" rows="2" maxlength="5000"></textarea></div>
              <div class="rangee">
                <button class="btn btn-secondaire btn-petit" type="submit" name="suite" value="ENTRETIEN_SUIVANT">Entretien suivant</button>
                <button class="btn btn-valider btn-petit" type="submit" name="suite" value="TESTS"
                        ${offreAvecTests ? '' : 'disabled title="Créez d’abord un test pour cette offre"'}>Passer aux tests</button>
                <button class="btn btn-refuser btn-petit" type="submit" name="suite" value="REFUS">Refuser</button>
              </div>
              <c:if test="${not offreAvecTests}"><p class="texte-doux">Aucun test n’est défini pour cette offre : le passage aux tests est indisponible.</p></c:if>
              <p class="texte-doux">La décision n’est possible qu’une fois l’entretien passé.</p>
            </ui:formulaire>
          </c:if>
        </div>
      </c:forEach>
      <c:if test="${peutPlanifierEntretien}">
        <h4 style="margin-top:var(--e-4);">Planifier l’entretien n° ${fn:length(entretiens) + 1}</h4>
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

  <c:if test="${candidature.statut == 'RETENUE' or candidature.statut == 'EMBAUCHE'}">
    <div class="fiche" id="proposition" style="margin-top:var(--e-6);"><div class="fiche-corps">
      <h3>Proposition d’embauche</h3>
      <c:choose>
        <c:when test="${candidature.statut == 'RETENUE' and candidature.propositionStatut == null}">
          <p class="texte-doux">À l’acceptation du candidat, le système crée automatiquement son dossier salarié et son matricule,
            son compte employé, son premier contrat, son affectation au poste et sa checklist d’intégration.</p>
          <c:if test="${budgetRestant != null}">
            <p>Budget global du besoin : <c:out value="${candidature.offre.demandeOrigine.budgetGlobalRecrutement}"/> Ar / an —
              reste disponible : <strong><c:out value="${budgetRestant}"/> Ar / an</strong>
              (soit un salaire mensuel maximal de <c:out value="${budgetRestant / 12}"/> Ar).</p>
          </c:if>
          <c:choose>
            <c:when test="${postesDisponibles <= 0}">
              <ui:alerte type="alerte">Les <c:out value="${candidature.offre.nbPostes}"/> poste(s) de cette offre sont déjà pourvus
                ou proposés à d’autres candidats. Une proposition refusée libère le poste.</ui:alerte>
            </c:when>
            <c:otherwise>
              <p>Poste(s) encore disponible(s) sur l’offre : <strong><c:out value="${postesDisponibles}"/></strong>
                sur <c:out value="${candidature.offre.nbPostes}"/>.</p>
              <ui:formulaire action="/rh/candidatures/${candidature.id}/proposition">
                <div class="champ"><label for="proposition-type">Contrat</label><select class="saisie" id="proposition-type" name="typeContrat" required><c:forEach var="t" items="${typesContrat}"><option value="${t}"><c:out value="${t.libelle}"/></option></c:forEach></select></div>
                <div class="champ"><label for="proposition-salaire">Salaire brut mensuel (Ar)</label><input class="saisie" id="proposition-salaire" name="salaire" type="number" min="0.01" step="0.01" value="<c:out value='${candidature.offre.demandeOrigine.salaireMensuelParPoste}'/>" required></div>
                <ui:champ label="Date d’entrée" name="debut" type="date" requis="true"/>
                <ui:champ label="Fin prévue (hors CDI)" name="fin" type="date"/>
                <div class="champ"><label for="proposition-role">Rôle dans l’application après l’embauche</label>
                  <select class="saisie" id="proposition-role" name="role" required>
                    <option value="EMPLOYE">Employé</option>
                    <option value="CHEF_SERVICE">Chef de service</option>
                    <option value="DIRECTION">Direction</option>
                  </select></div>
                <c:choose>
                  <c:when test="${servicePoste != null}">
                    <p>Service d’accueil : <strong><c:out value="${servicePoste.nom}"/></strong> (service du poste).</p>
                  </c:when>
                  <c:otherwise>
                    <ui:champ-liste label="Service d’accueil (le poste n’est rattaché à aucun service)" name="serviceId"
                                    items="${services}" valueProperty="id" labelProperty="nom" requis="true"/>
                  </c:otherwise>
                </c:choose>
                <ui:bouton label="Envoyer la proposition" type="submit" style="btn-principal"/>
              </ui:formulaire>
            </c:otherwise>
          </c:choose>
        </c:when>
        <c:when test="${candidature.propositionStatut != null}">
          <dl class="infos">
            <div><dt>État</dt><dd><c:choose>
              <c:when test="${candidature.statut == 'EMBAUCHE'}"><span class="statut statut--succes">Acceptée — embauché(e)</span></c:when>
              <c:when test="${candidature.propositionStatut == 'EN_ATTENTE'}"><span class="statut statut--attente">En attente de la réponse du candidat</span></c:when>
              <c:when test="${candidature.propositionStatut == 'REFUSEE'}"><span class="statut statut--danger">Refusée par le candidat</span></c:when>
              <c:otherwise><span class="statut statut--attente">Acceptée — embauche à finaliser</span></c:otherwise>
            </c:choose></dd></div>
            <div><dt>Contrat</dt><dd><c:out value="${candidature.propositionTypeContrat.libelle}"/></dd></div>
            <div><dt>Salaire mensuel</dt><dd><c:out value="${candidature.propositionSalaire}"/> Ar</dd></div>
            <div><dt>Entrée</dt><dd><ui:date valeur="${candidature.propositionDebut}"/>
              <c:if test="${candidature.propositionFin != null}"> — fin prévue le <ui:date valeur="${candidature.propositionFin}"/></c:if></dd></div>
            <c:if test="${candidature.propositionRole != null}"><div><dt>Rôle attribué</dt><dd><c:choose>
              <c:when test="${candidature.propositionRole == 'CHEF_SERVICE'}">Chef de service</c:when>
              <c:when test="${candidature.propositionRole == 'DIRECTION'}">Direction</c:when>
              <c:otherwise>Employé</c:otherwise>
            </c:choose></dd></div></c:if>
            <c:if test="${not empty serviceProposition}"><div><dt>Service d’accueil</dt><dd><c:out value="${serviceProposition}"/></dd></div></c:if>
            <c:if test="${candidature.propositionEnvoyeeLe != null}"><div><dt>Envoyée</dt><dd>le <ui:date valeur="${candidature.propositionEnvoyeeLe}" heure="true"/>
              <c:if test="${not empty candidature.propositionEnvoyeePar}"> par <c:out value="${candidature.propositionEnvoyeePar}"/></c:if></dd></div></c:if>
            <c:if test="${candidature.propositionRepondueLe != null}"><div><dt>Réponse</dt><dd>le <ui:date valeur="${candidature.propositionRepondueLe}" heure="true"/></dd></div></c:if>
          </dl>
          <c:if test="${candidature.propositionStatut == 'REFUSEE'}">
            <p class="texte-doux">Le poste est de nouveau disponible : vous pouvez faire une proposition à un autre candidat retenu sur cette offre.</p>
          </c:if>
        </c:when>
      </c:choose>

      <c:if test="${candidature.statut == 'EMBAUCHE' and dossierEmbauche != null}">
        <ui:alerte type="succes">Embauche réalisée : dossier salarié
          <a href="${pageContext.request.contextPath}/rh/employes/${dossierEmbauche.id}"><strong><c:out value="${dossierEmbauche.matricule}"/></strong></a>
          — entrée le <ui:date valeur="${dossierEmbauche.dateEntree}"/>. Suivez le contrat et l’intégration depuis le dossier.</ui:alerte>
      </c:if>

      <%-- Reprise : proposition acceptée avant l'embauche automatique (rôle et service choisis ici). --%>
      <c:if test="${candidature.statut == 'RETENUE' and candidature.propositionStatut == 'ACCEPTEE'}">
        <h3>Finaliser l’embauche</h3>
        <p class="texte-doux">Cette proposition a été acceptée avant la création automatique : choisissez le rôle et, si besoin, le service.</p>
        <ui:formulaire action="/rh/candidatures/${candidature.id}/embauche">
          <div class="champ"><label for="finaliser-role">Rôle dans l’application</label>
            <select class="saisie" id="finaliser-role" name="role">
              <option value="EMPLOYE">Employé</option>
              <option value="CHEF_SERVICE">Chef de service</option>
              <option value="DIRECTION">Direction</option>
            </select></div>
          <c:if test="${servicePoste == null}">
            <ui:champ-liste label="Service d’accueil (le poste n’est rattaché à aucun service)" name="serviceId" items="${services}" valueProperty="id" labelProperty="nom" requis="true"/>
          </c:if>
          <ui:bouton label="Créer le dossier salarié et embaucher" type="submit" style="btn-valider"/>
        </ui:formulaire>
      </c:if>
    </div></div>
  </c:if>
</ui:layout>
