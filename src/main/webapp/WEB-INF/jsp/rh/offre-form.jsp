<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="ui" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<ui:layout title="${empty offreId ? 'Créer une offre' : 'Modifier le brouillon'}" activeMenu="offres">
  <div class="offre-assistant" data-etape-initiale="${empty etapeErreur ? 0 : etapeErreur}">
    <a class="offre-retour" href="${pageContext.request.contextPath}/rh/offres">Toutes les offres</a>
    <header class="assistant-entete"><h1>${empty offreId ? 'Créer une offre d’emploi' : 'Modifier le brouillon'}</h1><p>Renseignez le poste, rédigez l’annonce, puis vérifiez les informations avant publication.</p></header>
    <c:if test="${not empty erreur}"><div class="assistant-erreur-globale" role="alert" tabindex="-1"><c:out value="${erreur}"/></div></c:if>
    <c:set var="formulaireAction" value="/rh/offres"/>
    <c:if test="${not empty offreId}"><c:set var="formulaireAction" value="/rh/offres/${offreId}/modifier"/></c:if>
    <ui:formulaire action="${formulaireAction}" cssClass="assistant-formulaire">
      <input type="hidden" name="actionOffre" value="brouillon"/>
      <c:if test="${not empty demandeId}"><input type="hidden" name="demandeId" value="${demandeId}"/></c:if>
      <nav aria-label="Étapes de création" class="assistant-navigation"><ol>
        <li><button type="button" data-aller="0" aria-current="step"><span>1</span> Le poste</button></li>
        <li><button type="button" data-aller="1"><span>2</span> L’annonce</button></li>
        <li><button type="button" data-aller="2"><span>3</span> Vérification et publication</button></li>
      </ol></nav>
      <p class="assistant-obligatoire">Les champs marqués d’un astérisque (*) sont obligatoires.</p>
      <section class="assistant-etape" data-etape="0" aria-labelledby="titre-etape-poste">
        <h2 id="titre-etape-poste" tabindex="-1">Le poste à pourvoir</h2>
        <div class="champ"><label class="champ-libelle" for="champ-entrepriseId">Entreprise qui recrute *</label>
          <select class="saisie" id="champ-entrepriseId" name="entrepriseId" required aria-describedby="erreur-entrepriseId">
            <option value="">Sélectionner une entreprise</option>
            <c:forEach var="entreprise" items="${entreprises}"><option value="${entreprise.id}" ${saisie.entrepriseId == entreprise.id ? 'selected' : ''}><c:out value="${entreprise.nom}"/></option></c:forEach>
          </select>
          <span class="champ-erreur" id="erreur-entrepriseId"><c:out value="${erreursChamps.entrepriseId}"/></span>
          <c:if test="${empty entreprises}"><p class="champ-aide">Aucune entreprise n’est encore enregistrée. Ajoutez les entreprises avant de créer une offre.</p></c:if>
        </div>
        <c:choose><c:when test="${empty offreId}">
          <c:set var="modeInitial" value="${empty saisie.modePoste ? (empty postes ? 'NOUVEAU' : 'EXISTANT') : saisie.modePoste}"/>
          <fieldset class="assistant-mode"><legend>Définir le poste</legend>
            <label><input type="radio" name="modePoste" value="EXISTANT" ${modeInitial == 'EXISTANT' ? 'checked' : ''}/> Sélectionner un poste existant</label>
            <label><input type="radio" name="modePoste" value="NOUVEAU" ${modeInitial == 'NOUVEAU' ? 'checked' : ''}/> Saisir un autre intitulé</label>
          </fieldset>
          <fieldset id="poste-existant"><legend class="interface-visuel-cache">Poste existant</legend>
            <div class="champ"><label class="champ-libelle" for="champ-posteId">Poste du catalogue *</label>
              <select class="saisie" name="posteId" id="champ-posteId" aria-describedby="erreur-posteId"><option value="">Sélectionner un poste</option>
                <c:forEach var="p" items="${postes}"><option value="${p.id}" data-intitule="${fn:escapeXml(p.intitule)}" data-description="${fn:escapeXml(p.description)}" data-section="${fn:escapeXml(p.section.nom)}" ${saisie.posteId == p.id ? 'selected' : ''}><c:out value="${p.intitule}"/></option></c:forEach>
              </select><span class="champ-erreur" id="erreur-posteId"><c:out value="${erreursChamps.posteId}"/></span>
            </div><p class="assistant-indication" id="section-existante">Le poste du catalogue ne sera pas modifié par cette annonce.</p>
          </fieldset>
        </c:when><c:otherwise><p class="assistant-indication">Poste associé : <strong><c:out value="${posteIntitule}"/></strong>. Rattachement : <c:out value="${posteSection}"/>. La fiche du catalogue reste inchangée.</p></c:otherwise></c:choose>
        <ui:champ label="Intitulé du poste et de l’annonce *" name="titre" value="${fn:escapeXml(saisie.titre)}" requis="true" maxlength="150" erreur="${erreursChamps.titre}" aide="Un seul intitulé, visible dans l’annonce. Vous pouvez l’adapter sans modifier le poste du catalogue."/>
        <div class="assistant-colonnes">
          <ui:champ label="Nombre de postes à pourvoir *" name="nbPostes" type="number" min="1" value="${empty saisie.nbPostes ? '1' : fn:escapeXml(saisie.nbPostes)}" requis="true" erreur="${erreursChamps.nbPostes}"/>
          <c:if test="${empty offreId}"><fieldset id="poste-personnalise"><legend class="interface-visuel-cache">Rattachement du nouveau poste</legend>
            <ui:champ-liste label="Section de rattachement (facultatif)" name="sectionId" items="${sections}" valueProperty="id" labelProperty="nom" selected="${saisie.sectionId}"/>
            <span class="champ-erreur" id="erreur-sectionId"><c:out value="${erreursChamps.sectionId}"/></span>
          </fieldset></c:if>
        </div>
        <c:if test="${empty offreId}"><details id="informations-poste"><summary>Informations complémentaires du poste</summary>
          <ui:champ label="Catégorie du nouveau poste" name="categorie" maxlength="150" value="${fn:escapeXml(saisie.categorie)}" erreur="${erreursChamps.categorie}" aide="Utilisée uniquement lors de la création d’un nouveau poste."/>
        </details></c:if>
      </section>
      <section class="assistant-etape" data-etape="1" aria-labelledby="titre-etape-annonce">
        <h2 id="titre-etape-annonce" tabindex="-1">Le contenu de l’annonce</h2>
        <div class="champ"><label class="champ-libelle" for="champ-missions">Missions principales *</label>
          <textarea class="saisie" id="champ-missions" name="missions" rows="6" required aria-describedby="aide-missions erreur-missions"><c:out value="${saisie.missions}"/></textarea>
          <span id="aide-missions" class="champ-aide">Exemple : accueillir les visiteurs, gérer les rendez-vous et assurer le suivi administratif.</span>
          <span id="erreur-missions" class="champ-erreur"><c:out value="${erreursChamps.missions}"/></span>
        </div>
        <div class="champ"><label class="champ-libelle" for="champ-profil">Profil recherché *</label>
          <textarea class="saisie" id="champ-profil" name="profil" rows="5" required aria-describedby="aide-profil erreur-profil"><c:out value="${saisie.profil}"/></textarea>
          <span id="aide-profil" class="champ-aide">Exemple : sens de l’accueil, organisation et maîtrise des outils bureautiques.</span>
          <span id="erreur-profil" class="champ-erreur"><c:out value="${erreursChamps.profil}"/></span>
        </div>
        <details class="assistant-criteres" ${not empty diplomesSelectionnes or not empty saisie.experienceMinMois ? 'open' : ''}><summary>Préciser l’expérience et les diplômes (facultatif)</summary>
          <ui:champ label="Expérience minimale en mois" name="experienceMinMois" type="number" min="0" value="${fn:escapeXml(saisie.experienceMinMois)}" erreur="${erreursChamps.experienceMinMois}" aide="Vide ou 0 : débutants acceptés. Le score reste indicatif."/>
          <label class="champ-libelle" for="filtre-diplomes">Rechercher un diplôme</label><input class="saisie" id="filtre-diplomes" type="search" placeholder="Niveau, filière ou spécialité"/>
          <div class="assistant-diplomes" id="liste-diplomes">
            <c:forEach var="d" items="${diplomes}"><label><input type="checkbox" name="diplomeIds" value="${d.id}" ${not empty diplomesSelectionnes and diplomesSelectionnes.contains(d.id) ? 'checked' : ''}/><span><c:out value="${d.libelle}"/></span></label></c:forEach>
          </div><p class="champ-aide" id="compteur-diplomes" aria-live="polite">Aucun diplôme exigé si aucune case n’est cochée.</p>
          <span id="erreur-diplomeIds" class="champ-erreur"><c:out value="${erreursChamps.diplomeIds}"/></span>
        </details>
      </section>
      <section class="assistant-etape" data-etape="2" aria-labelledby="titre-etape-verification">
        <h2 id="titre-etape-verification" tabindex="-1">Vérification et publication</h2>
        <div class="assistant-recap"><div class="assistant-recap-entete"><h3>Le poste</h3><button type="button" data-aller="0">Modifier<span class="interface-visuel-cache"> le poste</span></button></div>
          <dl><div><dt>Intitulé</dt><dd id="recap-titre">—</dd></div><div><dt>Postes à pourvoir</dt><dd id="recap-effectif">—</dd></div><div><dt>Rattachement</dt><dd id="recap-section"><c:out value="${posteSection}" default="Non précisé"/></dd></div></dl>
        </div>
        <div class="assistant-recap"><div class="assistant-recap-entete"><h3>L’annonce</h3><button type="button" data-aller="1">Modifier<span class="interface-visuel-cache"> l’annonce</span></button></div>
          <h4>Missions principales</h4><p class="assistant-texte" id="recap-missions"></p><h4>Profil recherché</h4><p class="assistant-texte" id="recap-profil"></p>
          <p class="assistant-indication" id="recap-experience"></p><ul id="recap-diplomes"></ul>
        </div>
        <h3 class="assistant-sous-titre">Paramètres de candidature</h3>
        <ui:champ label="Date limite de candidature" name="dateLimite" type="date" value="${fn:escapeXml(saisie.dateLimite)}" erreur="${erreursChamps.dateLimite}" aide="Facultatif. Sans date limite, l’offre reste ouverte jusqu’à sa clôture."/>
        <div class="champ"><label class="champ-libelle" for="champ-piecesRequises">Documents complémentaires demandés (facultatif)</label>
          <textarea class="saisie" id="champ-piecesRequises" name="piecesRequises" rows="3" aria-describedby="aide-piecesRequises erreur-piecesRequises"><c:out value="${saisie.piecesRequises}"/></textarea>
          <span class="champ-aide" id="aide-piecesRequises">Une pièce par ligne, 8 maximum, 100 caractères par libellé. Un PDF de 5 Mo maximum sera demandé au candidat pour chaque pièce.</span>
          <span class="champ-erreur" id="erreur-piecesRequises"><c:out value="${erreursChamps.piecesRequises}"/></span>
        </div>
        <p class="assistant-publication">Le brouillon reste privé. « Publier l’offre » rend l’annonce visible sur la page des offres et ouvre les candidatures.</p>
      </section>
      <footer class="assistant-actions">
        <button type="button" class="btn btn-secondaire" data-retour>Retour</button>
        <span id="assistant-progression" role="status" aria-live="polite">Étape 1 sur 3</span>
        <div><button type="button" class="btn btn-principal" data-continuer>Continuer</button>
          <button type="submit" class="btn btn-secondaire" data-enregistrer="brouillon">Enregistrer le brouillon</button>
          <button type="submit" class="btn btn-principal" data-enregistrer="publier">Publier l’offre</button></div>
      </footer>
      <p id="assistant-enregistrement" class="champ-aide" role="status" aria-live="polite"></p>
    </ui:formulaire>
  </div>
  <script src="${pageContext.request.contextPath}/assets/js/offre-form.js" defer></script>
</ui:layout>
