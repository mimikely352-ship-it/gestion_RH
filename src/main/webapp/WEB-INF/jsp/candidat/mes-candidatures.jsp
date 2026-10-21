<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="ui" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<ui:layout-public title="Mes candidatures">
  <main class="site-conteneur site-main">
    <div class="page-titre">
      <h1>Mes candidatures</h1>
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
                  <div class="meta">
                    <span><c:out value="${c.offre.poste.intitule}"/></span>
                    <span>Candidature déposée le <c:out value="${c.dateCandidature}"/></span>
                  </div>
                </div>
                <div class="candidature__actions">
                  <c:if test="${c.statut == 'EN_TEST'}">
                    <a class="btn btn-principal btn-petit" href="${pageContext.request.contextPath}/candidat/candidatures/${c.id}/tests">Passer mes tests</a>
                  </c:if>
                  <c:if test="${c.statut == 'EN_EVALUATION'}">
                    <a class="btn btn-principal btn-petit" href="${pageContext.request.contextPath}/candidat/candidatures/${c.id}/evaluations">Mes évaluations</a>
                  </c:if>
                  <ui:statut valeur="${c.statut}"/>
                </div>
              </div>
              <div class="suivi">
                <c:forEach var="i" begin="0" end="4" varStatus="st">
                  <c:set var="libelles" value="Reçue,Entretien,Test,Évaluation,Décision"/>
                  <c:set var="classe" value="${i < c.etapeOrdinale ? 'fait' : (i == c.etapeOrdinale ? (c.refusee ? 'refus' : 'fait') : '')}"/>
                  <div class="suivi-etape ${classe}">
                    <span>${fn:split(libelles, ',')[i]}</span>
                  </div>
                </c:forEach>
              </div>
            </div>
          </c:forEach>
        </div>
      </c:otherwise>
    </c:choose>
  </main>
</ui:layout-public>
