<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="ui" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<ui:layout-public title="${not empty offreCandidature ? 'Candidater à une offre' : 'Devenir candidat'}">
  <main class="site-conteneur site-main" style="max-width:720px;">
    <div class="page-titre">
      <c:choose><c:when test="${not empty offreCandidature}">
        <a href="${pageContext.request.contextPath}/offres/${offreCandidature.id}">Retour à l’offre</a>
        <h1>Candidater : <c:out value="${offreCandidature.titre}"/></h1>
        <p>Entreprise : <strong><c:out value="${offreCandidature.entrepriseNom}"/></strong></p>
        <p>Un seul poste par entreprise est autorisé pour chaque candidat.</p>
        <p>Remplissez votre dossier. Votre compte sera créé et votre candidature envoyée en une seule fois.</p>
        <p>Déjà inscrit ? <a href="${pageContext.request.contextPath}/login?offreId=${offreCandidature.id}">J’ai déjà un compte</a></p>
      </c:when><c:otherwise><h1>Créer mon dossier de candidature</h1><p class="texte-doux">Informations personnelles, CV, expériences et diplômes.</p></c:otherwise></c:choose>
    </div>

    <c:if test="${not empty erreur}"><ui:alerte type="erreur"><c:out value="${erreur}"/></ui:alerte></c:if>

    <ui:formulaire action="${not empty actionInscription ? actionInscription : '/candidats/inscription'}" enctype="multipart/form-data">
      <h3>Informations personnelles</h3>
      <div class="formulaire-grille">
        <ui:champ label="Email" name="email" type="email" value="${form.email}" requis="true"/>
        <ui:champ label="Mot de passe pour suivre ma candidature" name="motDePasse" type="password" requis="true" aide="10 caractères minimum. Il servira à consulter l’avancement de votre candidature."/>
        <ui:champ label="Nom" name="nom" value="${form.nom}" requis="true"/>
        <ui:champ label="Prénom" name="prenom" value="${form.prenom}" requis="true"/>
        <ui:champ label="Téléphone" name="telephone" value="${form.telephone}"/>
        <ui:champ label="Date de naissance" name="dateNaissance" type="date" value="${form.dateNaissance}"/>
        <ui:champ label="Lieu de naissance" name="lieuNaissance" value="${form.lieuNaissance}"/>
        <ui:champ label="CIN" name="cin" value="${form.cin}"/>
        <div class="plein"><ui:champ label="Adresse" name="adresse" value="${form.adresse}"/></div>
      </div>

      <h3 style="margin-top:var(--e-6);">CV</h3>
      <ui:champ label="Téléverser votre CV (PDF)" name="cv" type="file" requis="${not empty offreCandidature}" aide="Format PDF uniquement, 5 Mo maximum."/>
      <c:if test="${not empty offreCandidature.piecesRequises}">
        <h3>Pièces demandées pour cette offre</h3>
        <p>Chaque pièce est obligatoire : PDF, 5 Mo maximum par fichier.</p>
        <c:forEach var="piece" items="${offreCandidature.listePiecesRequises}" varStatus="ligne">
          <div class="champ"><label class="champ-libelle" for="piece-${ligne.index}"><c:out value="${piece}"/></label>
            <input class="saisie" id="piece-${ligne.index}" type="file" name="piece_${ligne.index}" accept=".pdf,application/pdf" required>
          </div>
        </c:forEach>
      </c:if>

      <h3 style="margin-top:var(--e-6);">Diplômes</h3>
      <ui:champ-liste label="Vos diplômes" name="diplomeIds" items="${diplomes}" valueProperty="id" labelProperty="libelle" multiple="true" selected="${form.diplomeIds}" aide="Maintenez Ctrl (Windows) ou Cmd (Mac) pour sélectionner plusieurs diplômes."/>

      <h3 style="margin-top:var(--e-6);">Expériences professionnelles</h3>
      <c:forEach var="i" begin="0" end="2">
        <div class="fiche" style="margin-bottom:var(--e-4);">
          <div class="fiche-corps formulaire-grille">
            <ui:champ label="Intitulé du poste" name="experiences[${i}].intitule" value="${form.experiences[i].intitule}"/>
            <ui:champ label="Entreprise" name="experiences[${i}].entreprise" value="${form.experiences[i].entreprise}"/>
            <ui:champ label="Date de début" name="experiences[${i}].dateDebut" value="${form.experiences[i].dateDebut}" type="date"/>
            <ui:champ label="Date de fin" name="experiences[${i}].dateFin" value="${form.experiences[i].dateFin}" type="date" aide="Laisser vide si poste actuel."/>
            <div class="plein"><ui:champ-texte label="Description" name="experiences[${i}].description" value="${form.experiences[i].description}"/></div>
          </div>
        </div>
      </c:forEach>

      <div class="formulaire-actions">
        <ui:bouton label="${not empty offreCandidature ? 'Créer mon compte et envoyer ma candidature' : 'Créer mon compte'}" type="submit" style="btn-principal"/>
      </div>
    </ui:formulaire>
  </main>
</ui:layout-public>
