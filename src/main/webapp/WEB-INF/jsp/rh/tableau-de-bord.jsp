<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="ui" tagdir="/WEB-INF/tags" %>
<ui:layout title="Tableau de bord" activeMenu="tableau-de-bord">
  <header class="lecture-hero">
    <span class="lecture-surtitre">ESPACE RESSOURCES HUMAINES</span>
    <h1>Accompagner les talents, au quotidien.</h1>
    <p>Recrutement, dossiers et demandes.</p>
    <div class="rangee" style="margin-top:var(--e-5);">
      <ui:bouton label="Traiter les demandes" href="/rh/demandes" style="btn-principal"/>
      <ui:bouton label="Consulter les employés" href="/rh/employes" style="btn-secondaire"/>
    </div>
  </header>
  <div class="lecture-accueil">
    <div class="fiche"><div class="fiche-corps">
      <span class="lecture-surtitre">RECRUTEMENT</span>
      <h2>Offres d'emploi</h2>
      <p class="texte-doux">Créer, publier et clôturer les offres.</p>
      <a class="btn btn-secondaire btn-petit" href="${pageContext.request.contextPath}/rh/offres">Voir les offres</a>
    </div></div>
    <div class="fiche"><div class="fiche-corps">
      <span class="lecture-surtitre">CANDIDATS</span>
      <h2>Candidatures</h2>
      <p class="texte-doux">Entretiens, tests et évaluations.</p>
      <a class="btn btn-secondaire btn-petit" href="${pageContext.request.contextPath}/rh/candidatures">Voir les candidatures</a>
    </div></div>
    <div class="fiche"><div class="fiche-corps">
      <span class="lecture-surtitre">SUIVI</span>
      <h2>Demandes &amp; ordres</h2>
      <p class="texte-doux">Exécuter les demandes validées par le DG et les ordres directs.</p>
      <a class="btn btn-secondaire btn-petit" href="${pageContext.request.contextPath}/rh/demandes">Voir les demandes</a>
      <a class="btn btn-secondaire btn-petit" href="${pageContext.request.contextPath}/rh/ordres">Voir les ordres</a>
    </div></div>
    <div class="fiche"><div class="fiche-corps">
      <span class="lecture-surtitre">ÉQUIPES</span>
      <h2>Dossiers et organisation</h2>
      <p class="texte-doux">Contrats, documents, parcours et structure de l’entreprise.</p>
      <div class="rangee">
        <ui:bouton label="Employés" href="/rh/employes" style="btn-secondaire btn-petit"/>
        <ui:bouton label="Structure" href="/rh/organisation" style="btn-secondaire btn-petit"/>
      </div>
    </div></div>
  </div>
</ui:layout>
