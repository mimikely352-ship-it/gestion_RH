<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="ui" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<ui:layout-public title="Mes candidatures" pageCss="mes-candidatures.css">
  <main class="site-conteneur site-main" id="suivi-candidatures">
    <div class="page-titre">
      <h1>Mes candidatures</h1>
      <c:if test="${not empty succes}"><p role="status"><c:out value="${succes}"/></p></c:if>
      <p class="texte-doux">Suivez l'avancement de chacune de vos candidatures.</p>
    </div>

    <div class="profil-resume">
      <span class="avatar">${candidat.prenom.substring(0,1)}${candidat.nom.substring(0,1)}</span>
      <div class="profil-resume__infos">
        <h3><c:out value="${candidat.prenom} ${candidat.nom}"/></h3>
        <p><c:out value="${candidat.utilisateur.email}"/></p>
      </div>
      <div class="profil-resume__stats">
        <div><b>${candidatures.size()}</b><span>Candidature(s)</span></div>
      </div>
    </div>

    <c:choose>
      <c:when test="${empty candidatures}">
        <div class="vide-candidatures">
          <h3>Vous n'avez pas encore postulé</h3>
          <p>Consultez les <a href="${pageContext.request.contextPath}/offres">offres d'emploi</a> disponibles.</p>
        </div>
      </c:when>
      <c:otherwise>
        <div class="candidature-liste">
          <c:forEach var="c" items="${candidatures}">
            <div class="candidature">
              <div class="candidature__entete">
                <div class="candidature__infos">
                  <h3><c:out value="${c.offre.titre}"/></h3>
                  <p><strong><c:out value="${c.offre.entrepriseNom}"/></strong></p>
                  <div class="meta">
                    <span><c:out value="${c.offre.poste.intitule}"/></span>
                    <span>Déposée le <ui:date valeur="${c.dateCandidature}" heure="true"/></span>
                  </div>
                </div>
                <div class="candidature__actions">
                  <c:if test="${not empty c.piecesComplementaires}">
                    <details><summary>Mes pièces complémentaires</summary><ul>
                      <c:forEach var="piece" items="${c.piecesComplementaires}"><li><a href="${pageContext.request.contextPath}/documents/${piece.id}/telecharger" target="_blank" rel="noopener"><c:out value="${piece.libelle}"/></a></li></c:forEach>
                    </ul></details>
                  </c:if>
                  <c:if test="${c.statut == 'EN_TEST'}">
                    <a class="btn btn-principal btn-petit" href="${pageContext.request.contextPath}/candidat/candidatures/${c.id}/tests">Passer mes tests</a>
                  </c:if>
                  <c:if test="${c.statut == 'EN_EVALUATION'}">
                    <a class="btn btn-principal btn-petit" href="${pageContext.request.contextPath}/candidat/candidatures/${c.id}/evaluations">Mes évaluations</a>
                  </c:if>
                  <ui:statut valeur="${c.statut}"/>
                </div>
              </div>
              <ol class="suivi" aria-label="Étapes du recrutement">
                <c:forEach var="i" begin="0" end="4" varStatus="st">
                  <c:set var="libelles" value="Reçue,Entretien,Test,Évaluation,Décision"/>
                  <c:set var="classe" value="${i < c.etapeOrdinale ? 'fait' : (i == c.etapeOrdinale ? (c.refusee ? 'refus' : 'fait') : '')}"/>
                  <li class="suivi-etape ${classe}" ${i == c.etapeOrdinale ? 'aria-current="step"' : ''}>
                    <span>${fn:split(libelles, ',')[i]}</span>
                  </li>
                </c:forEach>
              </ol>
              <div class="candidature__pied">
                <c:choose>
                  <c:when test="${c.statut == 'RECUE'}"><p>Votre dossier a été reçu. L’équipe RH va l’examiner.</p></c:when>
                  <c:when test="${c.statut == 'ACCEPTEE_ENTRETIEN'}"><p>Votre candidature est retenue pour un entretien. La date vous sera communiquée après sa planification.</p></c:when>
                  <c:when test="${c.statut == 'EN_ENTRETIEN'}"><div><p>Consultez les informations de votre entretien :</p><ul>
                    <c:forEach var="ent" items="${entretiensParCandidature[c.id]}"><li><ui:date valeur="${ent.dateHeure}" heure="true"/> — <c:out value="${ent.lieu}" default="Lieu à préciser"/></li></c:forEach>
                  </ul></div></c:when>
                  <c:when test="${c.refusee}"><p>Votre candidature n’a pas été retenue. Vous pouvez consulter les autres offres disponibles.</p></c:when>
                  <c:when test="${c.statut == 'RETENUE'}"><p>Vous avez été retenu pour ce poste. L’équipe RH prépare la suite de votre recrutement.</p></c:when>
                  <c:when test="${c.statut == 'EMBAUCHE'}"><p>Votre recrutement a été finalisé.</p></c:when>
                  <c:otherwise><p>Utilisez les actions du dossier pour poursuivre votre recrutement et consultez vos notifications.</p></c:otherwise>
                </c:choose>
              </div>
            </div>
          </c:forEach>
        </div>
      </c:otherwise>
    </c:choose>
  </main>
</ui:layout-public>
