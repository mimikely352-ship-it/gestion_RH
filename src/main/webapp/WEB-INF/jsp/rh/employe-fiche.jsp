<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="ui" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<c:set var="h" value="${fiche.habilitation}"/>
<c:set var="rh" value="${h.modification}"/>
<ui:layout title="Dossier salarié" activeMenu="${rh ? 'employes' : 'dossiers'}">
  <p>
    <c:choose>
      <c:when test="${rh}"><a href="${pageContext.request.contextPath}/rh/employes">← Retour aux employés</a></c:when>
      <c:when test="${h.audit or not h.donneesPersonnelles}"><a href="${pageContext.request.contextPath}/dossiers">← Retour aux dossiers</a></c:when>
    </c:choose>
  </p>
  <div class="rangee" style="justify-content:space-between;align-items:flex-start;">
    <div>
      <p class="texte-doux" style="margin:0;">Dossier salarié <strong><c:out value="${fiche.matricule}"/></strong></p>
      <h1><c:out value="${fiche.nomComplet}"/></h1>
      <p class="texte-doux"><c:out value="${fiche.poste}"/> — <c:out value="${fiche.service}"/>
        <c:if test="${fiche.contratEnCours != null}"> — <c:out value="${fiche.contratEnCours.type}"/></c:if></p>
    </div>
    <c:choose>
      <c:when test="${fiche.actif and fiche.departEnCours != null}"><span class="statut statut--attente">Départ en cours</span></c:when>
      <c:when test="${fiche.actif}"><span class="statut statut--succes">En poste</span></c:when>
      <c:otherwise><span class="statut statut--danger">A quitté l’entreprise</span></c:otherwise>
    </c:choose>
  </div>
  <c:if test="${not rh}">
    <ui:alerte type="info">Consultation en lecture seule.
      <c:if test="${not h.donneesPersonnelles}"> Votre habilitation donne accès à la situation professionnelle uniquement : données personnelles, rémunération et documents sont confidentiels.</c:if>
      Chaque consultation est enregistrée dans le journal d’audit du dossier.</ui:alerte>
  </c:if>
  <c:if test="${not empty message}"><ui:alerte type="succes"><c:out value="${message}"/></ui:alerte></c:if>
  <c:if test="${not empty erreur}"><ui:alerte type="erreur"><c:out value="${erreur}"/></ui:alerte></c:if>
  <c:if test="${not fiche.actif and fiche.departCloture != null}">
    <ui:alerte type="alerte">
      <c:out value="${fiche.departCloture.libelleType}"/><c:if test="${not empty fiche.departCloture.motif}"> (<c:out value="${fiche.departCloture.motif}"/>)</c:if>
      — dernier jour le <c:out value="${fiche.departCloture.dateEffet}"/>. Le compte de connexion est désactivé ; le dossier reste consultable.
    </ui:alerte>
  </c:if>

  <%-- ================= IDENTITÉ / COORDONNÉES / SITUATION PROFESSIONNELLE ================= --%>
  <div class="grille-2" id="identite" style="margin-top:var(--e-6);">
    <div class="fiche"><div class="fiche-corps">
      <h2>Identité et coordonnées</h2>
      <c:choose>
        <c:when test="${h.donneesPersonnelles}">
          <dl class="infos">
            <div><dt>Matricule</dt><dd><c:out value="${fiche.matricule}"/></dd></div>
            <div><dt>Nom</dt><dd><c:out value="${fiche.nom}"/></dd></div>
            <div><dt>Prénom</dt><dd><c:out value="${fiche.prenom}"/></dd></div>
            <div><dt>Sexe</dt><dd><c:out value="${empty fiche.sexe ? '—' : fiche.sexe}"/></dd></div>
            <div><dt>Naissance</dt><dd><c:out value="${empty fiche.dateNaissance ? '—' : fiche.dateNaissance}"/><c:if test="${not empty fiche.lieuNaissance}"> à <c:out value="${fiche.lieuNaissance}"/></c:if></dd></div>
            <div><dt>Nationalité</dt><dd><c:out value="${empty fiche.nationalite ? '—' : fiche.nationalite}"/></dd></div>
            <div><dt>CIN</dt><dd><c:out value="${empty fiche.cin ? '—' : fiche.cin}"/><c:if test="${not empty fiche.cinDelivrance}"><br><small class="texte-doux">délivrée <c:out value="${fiche.cinDelivrance}"/></small></c:if></dd></div>
            <div><dt>Situation familiale</dt><dd><c:out value="${empty fiche.situationFamiliale ? '—' : fiche.situationFamiliale}"/><c:if test="${not empty fiche.nombreEnfants}">, <c:out value="${fiche.nombreEnfants}"/> enfant(s)</c:if></dd></div>
            <div><dt>N° CNaPS</dt><dd><c:out value="${empty fiche.numeroCnaps ? '—' : fiche.numeroCnaps}"/></dd></div>
            <div><dt>Adresse</dt><dd><c:out value="${empty fiche.adresse ? '—' : fiche.adresse}"/></dd></div>
            <div><dt>Téléphone</dt><dd><c:out value="${empty fiche.telephone ? '—' : fiche.telephone}"/></dd></div>
            <div><dt>E-mail personnel</dt><dd><c:out value="${empty fiche.emailPersonnel ? '—' : fiche.emailPersonnel}"/></dd></div>
            <div><dt>Contact d’urgence</dt><dd><c:out value="${empty fiche.contactUrgence ? '—' : fiche.contactUrgence}"/></dd></div>
          </dl>
          <c:if test="${not empty fiche.misAJour}"><p class="texte-doux" style="margin-top:var(--e-3);">Mis à jour le <c:out value="${fiche.misAJour}"/></p></c:if>
          <c:if test="${rh and fiche.saisie != null}">
            <c:set var="s" value="${fiche.saisie}"/>
            <details style="margin-top:var(--e-4);">
              <summary>Modifier l’identité et les coordonnées</summary>
              <ui:formulaire action="/rh/employes/${fiche.dossierId}/identite">
                <div class="grille-2">
                  <div><label for="id-nom">Nom</label><input class="saisie" id="id-nom" name="nom" value="${fn:escapeXml(s.nom)}" required maxlength="100"></div>
                  <div><label for="id-prenom">Prénom</label><input class="saisie" id="id-prenom" name="prenom" value="${fn:escapeXml(s.prenom)}" required maxlength="100"></div>
                  <div><label for="id-sexe">Sexe</label><select class="saisie" id="id-sexe" name="sexe"><option value="">—</option>
                    <option value="F" ${s.sexe == 'F' ? 'selected' : ''}>Féminin</option><option value="M" ${s.sexe == 'M' ? 'selected' : ''}>Masculin</option></select></div>
                  <div><label for="id-naissance">Date de naissance</label><input class="saisie" id="id-naissance" type="date" name="dateNaissance" value="${s.dateNaissance}"></div>
                  <div><label for="id-lieu">Lieu de naissance</label><input class="saisie" id="id-lieu" name="lieuNaissance" value="${fn:escapeXml(s.lieuNaissance)}" maxlength="150"></div>
                  <div><label for="id-nat">Nationalité</label><input class="saisie" id="id-nat" name="nationalite" value="${fn:escapeXml(s.nationalite)}" maxlength="80"></div>
                  <div><label for="id-cin">N° CIN</label><input class="saisie" id="id-cin" name="cin" value="${fn:escapeXml(s.cin)}" maxlength="30"></div>
                  <div><label for="id-cin-le">CIN délivrée le</label><input class="saisie" id="id-cin-le" type="date" name="cinDelivreLe" value="${s.cinDelivreLe}"></div>
                  <div><label for="id-cin-a">CIN délivrée à</label><input class="saisie" id="id-cin-a" name="cinDelivreA" value="${fn:escapeXml(s.cinDelivreA)}" maxlength="100"></div>
                  <div><label for="id-sit">Situation familiale</label><select class="saisie" id="id-sit" name="situationFamiliale"><option value="">—</option>
                    <c:forEach var="sf" items="${situationsFamiliales}"><option value="${sf}" ${s.situationFamiliale == sf ? 'selected' : ''}><c:out value="${sf}"/></option></c:forEach></select></div>
                  <div><label for="id-enf">Nombre d’enfants</label><input class="saisie" id="id-enf" type="number" min="0" max="30" name="nombreEnfants" value="${s.nombreEnfants}"></div>
                  <div><label for="id-cnaps">N° CNaPS</label><input class="saisie" id="id-cnaps" name="numeroCnaps" value="${fn:escapeXml(s.numeroCnaps)}" maxlength="30"></div>
                  <div><label for="id-adr">Adresse</label><input class="saisie" id="id-adr" name="adresse" value="${fn:escapeXml(s.adresse)}" maxlength="255"></div>
                  <div><label for="id-tel">Téléphone</label><input class="saisie" id="id-tel" type="tel" name="telephone" value="${fn:escapeXml(s.telephone)}" maxlength="30"></div>
                  <div><label for="id-mail">E-mail personnel</label><input class="saisie" id="id-mail" type="email" name="emailPersonnel" value="${fn:escapeXml(s.emailPersonnel)}" maxlength="150"></div>
                  <div><label for="id-urg">Contact d’urgence (nom, lien)</label><input class="saisie" id="id-urg" name="contactUrgenceNom" value="${fn:escapeXml(s.contactUrgenceNom)}" maxlength="150"></div>
                  <div><label for="id-urg-tel">Téléphone d’urgence</label><input class="saisie" id="id-urg-tel" type="tel" name="contactUrgenceTelephone" value="${fn:escapeXml(s.contactUrgenceTelephone)}" maxlength="30"></div>
                </div>
                <ui:bouton label="Enregistrer" type="submit" style="btn-principal"/>
              </ui:formulaire>
            </details>
          </c:if>
        </c:when>
        <c:otherwise>
          <dl class="infos">
            <div><dt>Matricule</dt><dd><c:out value="${fiche.matricule}"/></dd></div>
            <div><dt>Nom</dt><dd><c:out value="${fiche.nom}"/></dd></div>
            <div><dt>Prénom</dt><dd><c:out value="${fiche.prenom}"/></dd></div>
            <div><dt>E-mail professionnel</dt><dd><c:out value="${fiche.email}"/></dd></div>
          </dl>
          <p class="texte-doux">Données personnelles confidentielles.</p>
        </c:otherwise>
      </c:choose>
    </div></div>

    <div class="fiche"><div class="fiche-corps">
      <h2>Situation professionnelle</h2>
      <dl class="infos">
        <div><dt>${fiche.actif ? 'Poste actuel' : 'Dernier poste'}</dt><dd><c:out value="${fiche.poste}"/></dd></div>
        <div><dt>Service</dt><dd><c:out value="${fiche.service}"/></dd></div>
        <div><dt>Contrat en cours</dt><dd>
          <c:choose>
            <c:when test="${fiche.contratEnCours != null}"><c:out value="${fiche.contratEnCours.libelleType}"/> depuis le <c:out value="${fiche.contratEnCours.debut}"/></c:when>
            <c:otherwise>Aucun</c:otherwise>
          </c:choose>
        </dd></div>
        <c:if test="${h.salaire}"><div><dt>Salaire en vigueur</dt><dd><c:out value="${empty fiche.salaireActuel ? 'Non renseigné' : fiche.salaireActuel}"/></dd></div></c:if>
        <div><dt>Date d’entrée</dt><dd><c:out value="${fiche.dateEntree}"/></dd></div>
        <div><dt>Ancienneté</dt><dd><c:out value="${fiche.anciennete}"/></dd></div>
        <div><dt>Identifiant de connexion</dt><dd><c:out value="${fiche.email}"/></dd></div>
        <div><dt>Rôle dans l’application</dt><dd><c:out value="${fiche.role}"/></dd></div>
        <div><dt>Recrutement</dt><dd>
          <c:choose>
            <c:when test="${fiche.candidatureId != null}"><a href="${pageContext.request.contextPath}/rh/candidatures/${fiche.candidatureId}"><c:out value="${fiche.offreRecrutement}"/></a></c:when>
            <c:when test="${not empty fiche.offreRecrutement}"><c:out value="${fiche.offreRecrutement}"/></c:when>
            <c:otherwise>Création directe par les RH</c:otherwise>
          </c:choose>
        </dd></div>
      </dl>
    </div></div>
  </div>

  <%-- ================= CONTRATS ================= --%>
  <div class="fiche" id="contrats" style="margin-top:var(--e-6);"><div class="fiche-corps">
    <h2>Contrats</h2>
    <p class="texte-doux">Chaque nouveau contrat (renouvellement de CDD, passage en CDI...) clôt le précédent sans l’effacer.</p>
    <c:if test="${fiche.contratEnCours != null and fiche.contratEnCours.echu}">
      <ui:alerte type="alerte">Le contrat en cours devait se terminer le <c:out value="${fiche.contratEnCours.finPrevue}"/> : renouvelez-le, passez à un autre contrat ou ouvrez un dossier de départ.</ui:alerte>
    </c:if>
    <c:if test="${empty fiche.contrats}"><p>Aucun contrat enregistré.</p></c:if>
    <c:if test="${not empty fiche.contrats}">
      <div style="overflow-x:auto"><table class="tableau">
        <thead><tr><th scope="col">Type</th><th scope="col">Début</th><th scope="col">Fin prévue</th><th scope="col">Fin effective</th>
          <th scope="col">Statut</th><c:if test="${h.salaire}"><th scope="col">Salaire initial</th></c:if><c:if test="${h.documents}"><th scope="col">Exemplaire signé</th></c:if></tr></thead>
        <tbody><c:forEach var="k" items="${fiche.contrats}"><tr>
          <td><strong><c:out value="${k.type}"/></strong><br><small class="texte-doux"><c:out value="${k.libelleType}"/></small></td>
          <td><c:out value="${k.debut}"/></td><td><c:out value="${k.finPrevue}"/></td>
          <td><c:out value="${empty k.finEffective ? '—' : k.finEffective}"/>
            <c:if test="${not empty k.motifFin}"><br><small class="texte-doux"><c:out value="${k.motifFin}"/></small></c:if></td>
          <td><c:choose>
            <c:when test="${k.statut == 'EN_COURS'}"><span class="statut statut--succes">En cours</span></c:when>
            <c:when test="${k.statut == 'ROMPU'}"><span class="statut statut--danger">Rompu</span></c:when>
            <c:otherwise><span class="statut statut--neutre">Terminé</span></c:otherwise>
          </c:choose></td>
          <c:if test="${h.salaire}"><td><c:out value="${k.salaire}"/></td></c:if>
          <c:if test="${h.documents}"><td>
            <c:choose>
              <c:when test="${k.documentId != null}"><a href="${pageContext.request.contextPath}/documents/${k.documentId}/telecharger" target="_blank" rel="noopener">Signé le <c:out value="${k.signeLe}"/></a></c:when>
              <c:when test="${rh}">
                <ui:formulaire action="/rh/employes/${fiche.dossierId}/contrats/${k.id}/signature" enctype="multipart/form-data">
                  <label for="signe-${k.id}" class="champ-aide">Non signé — déposer le PDF signé</label>
                  <input class="saisie" id="signe-${k.id}" type="file" name="fichier" accept="application/pdf" required>
                  <ui:bouton label="Déposer" type="submit" style="btn-secondaire btn-petit"/>
                </ui:formulaire>
              </c:when>
              <c:otherwise>Non signé</c:otherwise>
            </c:choose>
          </td></c:if>
        </tr></c:forEach></tbody>
      </table></div>
    </c:if>
    <c:if test="${rh and fiche.actif}">
      <details style="margin-top:var(--e-4);" ${empty fiche.contrats ? 'open' : ''}>
        <summary>${empty fiche.contrats ? 'Enregistrer le contrat de l’employé' : 'Nouveau contrat (renouvellement, passage en CDI...)'}</summary>
        <ui:formulaire action="/rh/employes/${fiche.dossierId}/contrats" enctype="multipart/form-data">
          <div class="grille-2">
            <div><label for="contrat-type">Type de contrat</label>
              <select class="saisie" id="contrat-type" name="type" required>
                <c:forEach var="t" items="${typesContrat}"><option value="${t}"><c:out value="${t.libelle}"/></option></c:forEach>
              </select></div>
            <div><label for="contrat-salaire">Salaire brut mensuel (facultatif)</label>
              <input class="saisie" id="contrat-salaire" type="number" name="salaireBrut" min="0" step="0.01"></div>
            <div><label for="contrat-debut">Date de début</label>
              <input class="saisie" id="contrat-debut" type="date" name="dateDebut" required></div>
            <div><label for="contrat-fin">Date de fin prévue</label>
              <input class="saisie" id="contrat-fin" type="date" name="dateFinPrevue" aria-describedby="aide-contrat-fin">
              <small id="aide-contrat-fin" class="champ-aide">Obligatoire sauf pour un CDI.</small></div>
          </div>
          <label for="contrat-fichier">Exemplaire signé (PDF, facultatif — peut être déposé plus tard)</label>
          <input class="saisie" id="contrat-fichier" type="file" name="fichier" accept="application/pdf">
          <ui:bouton label="Enregistrer le contrat" type="submit" style="btn-principal"/>
        </ui:formulaire>
      </details>
    </c:if>
  </div></div>

  <%-- ================= RÉMUNÉRATION ================= --%>
  <c:if test="${h.salaire}">
    <div class="fiche" id="remuneration" style="margin-top:var(--e-6);"><div class="fiche-corps">
      <h2>Rémunération</h2>
      <p class="texte-doux">Confidentiel. Chaque révision ajoute une ligne ; les montants précédents ne sont jamais modifiés.</p>
      <c:if test="${empty fiche.remunerations}"><p>Aucune rémunération enregistrée.</p></c:if>
      <c:if test="${not empty fiche.remunerations}">
        <div style="overflow-x:auto"><table class="tableau">
          <thead><tr><th scope="col">Date d’effet</th><th scope="col">Salaire brut</th><th scope="col">Motif</th><th scope="col">Saisie</th></tr></thead>
          <tbody><c:forEach var="r" items="${fiche.remunerations}"><tr>
            <td><c:out value="${r.dateEffet}"/><c:if test="${r.actuelle}"> <span class="statut statut--succes">En vigueur</span></c:if></td>
            <td><strong><c:out value="${r.salaire}"/></strong></td><td><c:out value="${r.motif}"/></td>
            <td><small class="texte-doux"><c:out value="${r.saisie}"/></small></td>
          </tr></c:forEach></tbody>
        </table></div>
      </c:if>
      <c:if test="${rh and fiche.actif and fiche.contratEnCours != null}">
        <details style="margin-top:var(--e-4);">
          <summary>Réviser le salaire</summary>
          <ui:formulaire action="/rh/employes/${fiche.dossierId}/salaires">
            <div class="grille-2">
              <div><label for="sal-effet">Date d’effet</label><input class="saisie" id="sal-effet" type="date" name="dateEffet" required></div>
              <div><label for="sal-montant">Nouveau salaire brut mensuel</label><input class="saisie" id="sal-montant" type="number" min="0" step="0.01" name="salaireBrut" required></div>
            </div>
            <label for="sal-motif">Motif</label>
            <input class="saisie" id="sal-motif" name="motif" maxlength="150" required placeholder="Augmentation annuelle, promotion, ancienneté...">
            <ui:bouton label="Enregistrer la révision" type="submit" style="btn-principal"/>
          </ui:formulaire>
        </details>
      </c:if>
    </div></div>
  </c:if>

  <%-- ================= DOCUMENTS ADMINISTRATIFS ================= --%>
  <c:if test="${h.documents}">
    <div class="fiche" id="documents" style="margin-top:var(--e-6);"><div class="fiche-corps">
      <h2>Documents administratifs</h2>
      <c:if test="${not empty fiche.piecesAdministrativesManquantes}">
        <ui:alerte type="alerte">Dossier incomplet — pièces obligatoires manquantes ou expirées :
          <c:forEach var="m" items="${fiche.piecesAdministrativesManquantes}" varStatus="st"><c:out value="${m}"/><c:if test="${not st.last}">, </c:if></c:forEach>.</ui:alerte>
      </c:if>
      <c:if test="${empty fiche.documentsAdministratifs}"><p>Aucun document administratif déposé.</p></c:if>
      <c:if test="${not empty fiche.documentsAdministratifs}">
        <div style="overflow-x:auto"><table class="tableau">
          <thead><tr><th scope="col">Document</th><th scope="col">Numéro</th><th scope="col">Émis le</th><th scope="col">Expire le</th><th scope="col">Validité</th><th scope="col">Fichier</th></tr></thead>
          <tbody><c:forEach var="a" items="${fiche.documentsAdministratifs}"><tr>
            <td><c:out value="${a.libelle}"/></td><td><c:out value="${empty a.numero ? '—' : a.numero}"/></td>
            <td><c:out value="${a.emission}"/></td><td><c:out value="${a.expiration}"/></td>
            <td><c:choose>
              <c:when test="${a.remplace}"><span class="statut statut--neutre">Remplacé</span></c:when>
              <c:when test="${a.validite == 'EXPIRE'}"><span class="statut statut--danger">Expiré</span></c:when>
              <c:when test="${a.validite == 'A_RENOUVELER'}"><span class="statut statut--attente">À renouveler</span></c:when>
              <c:when test="${a.validite == 'VALIDE'}"><span class="statut statut--succes">Valide</span></c:when>
              <c:otherwise><span class="statut statut--neutre">Sans échéance</span></c:otherwise>
            </c:choose></td>
            <td><a href="${pageContext.request.contextPath}/documents/${a.id}/telecharger" target="_blank" rel="noopener"><c:out value="${a.nomFichier}"/></a></td>
          </tr></c:forEach></tbody>
        </table></div>
      </c:if>
      <c:if test="${rh}">
        <details style="margin-top:var(--e-4);">
          <summary>Ajouter un document administratif</summary>
          <ui:formulaire action="/rh/employes/${fiche.dossierId}/documents" enctype="multipart/form-data">
            <div class="grille-2">
              <div><label for="doc-type">Type</label><select class="saisie" id="doc-type" name="type" required>
                <c:forEach var="t" items="${typesDocument}"><option value="${t}"><c:out value="${t.libelle}"/><c:if test="${t.obligatoire}"> *</c:if><c:if test="${t.validiteMois > 0}"> — valable <c:out value="${t.validiteMois}"/> mois</c:if></option></c:forEach>
              </select></div>
              <div><label for="doc-num">Numéro (facultatif)</label><input class="saisie" id="doc-num" name="numero" maxlength="60"></div>
              <div><label for="doc-emis">Date d’émission</label><input class="saisie" id="doc-emis" type="date" name="dateEmission" max="${aujourdhui}"></div>
              <div><label for="doc-exp">Date d’expiration</label><input class="saisie" id="doc-exp" type="date" name="dateExpiration" aria-describedby="aide-doc-exp">
                <small id="aide-doc-exp" class="champ-aide">Calculée depuis l’émission si la durée de validité est connue ; à saisir sinon (permis, passeport).</small></div>
            </div>
            <label for="doc-fichier">Fichier PDF</label>
            <input class="saisie" id="doc-fichier" type="file" name="fichier" accept="application/pdf" required>
            <ui:bouton label="Ajouter le document" type="submit" style="btn-principal"/>
          </ui:formulaire>
        </details>
      </c:if>
      <c:if test="${not empty fiche.autresDocuments}">
        <h3 style="margin-top:var(--e-5);">Autres documents (contrats, départ, recrutement)</h3>
        <ul><c:forEach var="doc" items="${fiche.autresDocuments}">
          <li><a href="${pageContext.request.contextPath}/documents/${doc.id}/telecharger" target="_blank" rel="noopener"><c:out value="${doc.libelle}"/></a>
            — <c:out value="${doc.nomFichier}"/> <small class="texte-doux"><c:out value="${doc.date}"/></small></li>
        </c:forEach></ul>
      </c:if>
    </div></div>
  </c:if>

  <%-- ================= ONBOARDING ================= --%>
  <div class="fiche" id="onboarding" style="margin-top:var(--e-6);"><div class="fiche-corps">
    <div class="rangee" style="justify-content:space-between;">
      <h2>Intégration (onboarding)</h2>
      <c:choose>
        <c:when test="${fiche.onboarding.termine}"><span class="statut statut--succes">Terminé<c:if test="${not empty fiche.onboarding.termineLe}"> le <c:out value="${fiche.onboarding.termineLe}"/></c:if></span></c:when>
        <c:otherwise><span class="statut statut--attente">En cours — <c:out value="${fiche.onboarding.faits}"/> / <c:out value="${fiche.onboarding.total}"/></span></c:otherwise>
      </c:choose>
    </div>
    <div role="progressbar" aria-label="Avancement de l’intégration" aria-valuemin="0" aria-valuemax="100" aria-valuenow="${fiche.onboarding.pourcentage}"
         style="height:8px;border-radius:4px;background:var(--surface-2);overflow:hidden;margin:var(--e-3) 0 var(--e-4);">
      <span style="display:block;height:100%;width:${fiche.onboarding.pourcentage}%;background:var(--vert);"></span>
    </div>
    <ui:formulaire action="/rh/employes/${fiche.dossierId}/onboarding">
      <div style="overflow-x:auto"><table class="tableau">
        <thead><tr><th scope="col">Fait</th><th scope="col">Tâche</th><th scope="col">Responsable</th><th scope="col">Suivi</th></tr></thead>
        <tbody><c:forEach var="e" items="${fiche.onboarding.etapes}"><tr>
          <td><input type="checkbox" id="tache-${e.code}" aria-label="${e.libelle}"
                     <c:if test="${not e.automatique}">name="taches" value="${e.code}"</c:if>
                     ${e.fait ? 'checked' : ''} ${e.automatique or not fiche.actif or not rh ? 'disabled' : ''}></td>
          <td><label for="tache-${e.code}"><c:out value="${e.libelle}"/></label>
            <c:if test="${e.automatique}"><br><em class="texte-doux">automatique</em></c:if></td>
          <td><c:out value="${e.responsable}"/></td>
          <td><small class="texte-doux"><c:out value="${empty e.detail ? 'À faire' : e.detail}"/></small></td>
        </tr></c:forEach></tbody>
      </table></div>
      <c:if test="${rh and fiche.actif}"><ui:bouton label="Enregistrer la checklist" type="submit" style="btn-principal"/></c:if>
    </ui:formulaire>
  </div></div>

  <%-- ================= DÉPART ================= --%>
  <c:if test="${h.depart}">
  <div class="fiche" id="depart" style="margin-top:var(--e-6);"><div class="fiche-corps">
    <h2>Départ : démission ou renvoi</h2>
    <c:set var="d" value="${fiche.departEnCours}"/>
    <c:choose>
      <c:when test="${d != null}">
        <dl class="infos">
          <div><dt>Type</dt><dd><c:out value="${d.libelleType}"/></dd></div>
          <c:if test="${not empty d.motif}"><div><dt>Motif</dt><dd><c:out value="${d.motif}"/></dd></div></c:if>
          <div><dt>Notifié le</dt><dd><c:out value="${d.dateNotification}"/></dd></div>
          <div><dt>Dernier jour</dt><dd><c:out value="${d.dateEffet}"/></dd></div>
          <div><dt>Ouvert le</dt><dd><c:out value="${d.cree}"/></dd></div>
        </dl>
        <c:if test="${not empty d.description}"><p><c:out value="${d.description}"/></p></c:if>
        <h3 style="margin-top:var(--e-5);">Pièces du dossier</h3>
        <div style="overflow-x:auto"><table class="tableau">
          <thead><tr><th scope="col">Pièce</th><th scope="col">État</th><th scope="col">Fichiers</th></tr></thead>
          <tbody><c:forEach var="p" items="${d.pieces}"><tr>
            <td><c:out value="${p.libelle}"/><c:if test="${p.obligatoire}"> <small class="texte-doux">(obligatoire)</small></c:if></td>
            <td><c:choose>
              <c:when test="${p.deposee}"><span class="statut statut--succes">Déposée</span></c:when>
              <c:when test="${p.obligatoire}"><span class="statut statut--danger">Manquante</span></c:when>
              <c:otherwise><span class="statut statut--neutre">Facultative</span></c:otherwise>
            </c:choose></td>
            <td><c:forEach var="f" items="${p.fichiers}">
              <a href="${pageContext.request.contextPath}/documents/${f.id}/telecharger" target="_blank" rel="noopener"><c:out value="${f.nomFichier}"/></a>
              <small class="texte-doux"><c:out value="${f.date}"/></small><br></c:forEach></td>
          </tr></c:forEach></tbody>
        </table></div>
        <c:if test="${rh}">
          <ui:formulaire action="/rh/employes/${fiche.dossierId}/departs/${d.id}/pieces" enctype="multipart/form-data">
            <div class="grille-2">
              <div><label for="piece-type">Pièce</label>
                <select class="saisie" id="piece-type" name="piece" required>
                  <c:forEach var="p" items="${d.pieces}"><option value="${p.code}"><c:out value="${p.libelle}"/></option></c:forEach>
                </select></div>
              <div><label for="piece-fichier">Fichier PDF</label>
                <input class="saisie" id="piece-fichier" type="file" name="fichier" accept="application/pdf" required></div>
            </div>
            <ui:bouton label="Ajouter la pièce" type="submit" style="btn-secondaire"/>
          </ui:formulaire>
          <h3 style="margin-top:var(--e-5);">Clôture</h3>
          <c:choose>
            <c:when test="${d.pret}"><p>Toutes les pièces obligatoires sont là et la date d’effet est atteinte.</p></c:when>
            <c:when test="${not d.effetAtteint}"><p class="texte-doux">La clôture sera possible à partir du <c:out value="${d.dateEffet}"/>.</p></c:when>
            <c:otherwise><p class="texte-doux">Déposez d’abord les pièces obligatoires manquantes.</p></c:otherwise>
          </c:choose>
          <p class="texte-doux">La clôture rompt le contrat en cours, termine l’affectation et désactive le compte de connexion de l’employé.</p>
          <div class="rangee">
            <ui:formulaire action="/rh/employes/${fiche.dossierId}/departs/${d.id}/cloturer" cssClass="rangee">
              <ui:bouton label="Clôturer le départ" type="submit" style="btn-refuser"/>
            </ui:formulaire>
            <ui:formulaire action="/rh/employes/${fiche.dossierId}/departs/${d.id}/annuler" cssClass="rangee">
              <ui:bouton label="Annuler ce dossier (ex. démission retirée)" type="submit" style="btn-secondaire"/>
            </ui:formulaire>
          </div>
        </c:if>
      </c:when>
      <c:when test="${rh and fiche.actif}">
        <p class="texte-doux">Ouvrez un dossier de départ ; les pièces à réunir dépendent du type choisi.</p>
        <details>
          <summary>Ouvrir un dossier de démission ou de renvoi</summary>
          <ui:formulaire action="/rh/employes/${fiche.dossierId}/departs">
            <fieldset style="border:0;padding:0;margin:0;">
              <legend>Type de départ</legend>
              <c:forEach var="t" items="${typesDepart}">
                <label class="case"><input type="radio" name="type" value="${t}" required> <c:out value="${t.libelle}"/></label>
              </c:forEach>
            </fieldset>
            <div class="grille-2">
              <div><label for="depart-motif">Motif du renvoi</label>
                <select class="saisie" id="depart-motif" name="motif" aria-describedby="aide-depart-motif">
                  <option value="">—</option>
                  <c:forEach var="m" items="${motifsLicenciement}"><option value="${m}"><c:out value="${m.libelle}"/></option></c:forEach>
                </select>
                <small id="aide-depart-motif" class="champ-aide">Obligatoire pour un renvoi, ignoré pour une démission.</small></div>
              <div><label for="depart-notification">Date de notification</label>
                <input class="saisie" id="depart-notification" type="date" name="dateNotification" max="${aujourdhui}" required></div>
              <div><label for="depart-effet">Dernier jour dans l’entreprise</label>
                <input class="saisie" id="depart-effet" type="date" name="dateEffet" required></div>
            </div>
            <label for="depart-description">Précisions (préavis, circonstances...)</label>
            <textarea class="saisie" id="depart-description" name="description" rows="3"></textarea>
            <ui:bouton label="Ouvrir le dossier de départ" type="submit" style="btn-refuser"/>
          </ui:formulaire>
        </details>
      </c:when>
      <c:when test="${empty fiche.departs}"><p class="texte-doux">Aucun départ.</p></c:when>
    </c:choose>
    <c:forEach var="hd" items="${fiche.departs}">
      <c:if test="${not hd.enCours}">
        <h3 style="margin-top:var(--e-5);"><c:out value="${hd.libelleType}"/> — clôturé le <c:out value="${hd.cloture}"/></h3>
        <p><c:if test="${not empty hd.motif}">Motif : <c:out value="${hd.motif}"/>. </c:if>Notifié le <c:out value="${hd.dateNotification}"/>, dernier jour le <c:out value="${hd.dateEffet}"/>.</p>
        <c:if test="${not empty hd.description}"><p class="texte-doux"><c:out value="${hd.description}"/></p></c:if>
        <ul><c:forEach var="p" items="${hd.pieces}"><c:forEach var="f" items="${p.fichiers}">
          <li><c:out value="${p.libelle}"/> : <a href="${pageContext.request.contextPath}/documents/${f.id}/telecharger" target="_blank" rel="noopener"><c:out value="${f.nomFichier}"/></a></li>
        </c:forEach></c:forEach></ul>
      </c:if>
    </c:forEach>
  </div></div>
  </c:if>

  <%-- ================= CARRIÈRE ================= --%>
  <div class="fiche" style="margin-top:var(--e-6);"><div class="fiche-corps">
    <h2>Historique de carrière (affectations)</h2>
    <c:if test="${empty fiche.parcours}"><p class="texte-doux">Aucune affectation enregistrée.</p></c:if>
    <c:if test="${not empty fiche.parcours}">
      <div style="overflow-x:auto"><table class="tableau">
        <thead><tr><th scope="col">Mouvement</th><th scope="col">Poste</th><th scope="col">Service</th><th scope="col">Du</th><th scope="col">Au</th></tr></thead>
        <tbody><c:forEach var="p" items="${fiche.parcours}"><tr>
          <td><c:out value="${p.origine}"/></td>
          <td><strong><c:out value="${p.poste}"/></strong><c:if test="${p.actuelle}"> <span class="statut statut--succes">Actuel</span></c:if></td>
          <td><c:out value="${p.service}"/></td><td><c:out value="${p.debut}"/></td><td><c:out value="${p.fin}"/></td>
        </tr></c:forEach></tbody>
      </table></div>
    </c:if>
    <h3 style="margin-top:var(--e-5);">Demandes (promotion, transfert, formation...)</h3>
    <c:if test="${empty fiche.demandes}"><p class="texte-doux">Aucune demande.</p></c:if>
    <c:if test="${not empty fiche.demandes}">
      <div style="overflow-x:auto"><table class="tableau">
        <thead><tr><th scope="col">Date</th><th scope="col">Type</th><th scope="col">Description</th><th scope="col">Statut</th></tr></thead>
        <tbody><c:forEach var="dm" items="${fiche.demandes}"><tr>
          <td><c:out value="${dm.date}"/></td><td><c:out value="${dm.type}"/></td>
          <td><c:out value="${dm.description}"/></td><td><ui:statut valeur="${dm.statut}"/></td>
        </tr></c:forEach></tbody>
      </table></div>
    </c:if>
    <c:if test="${not empty fiche.corrections}">
      <h3 style="margin-top:var(--e-5);">Corrections de la date d’entrée</h3>
      <ul><c:forEach var="k" items="${fiche.corrections}">
        <li><c:out value="${k.horodatage}"/> — <c:out value="${k.ancienne}"/> → <c:out value="${k.nouvelle}"/> (par <c:out value="${k.auteur}"/>)</li>
      </c:forEach></ul>
    </c:if>
    <h3 style="margin-top:var(--e-5);">Diplômes</h3>
    <c:if test="${empty fiche.diplomes}"><p class="texte-doux">Aucun diplôme enregistré.</p></c:if>
    <ul><c:forEach var="dip" items="${fiche.diplomes}"><li><c:out value="${dip}"/></li></c:forEach></ul>
    <h3>Expériences antérieures</h3>
    <c:if test="${empty fiche.experiences}"><p class="texte-doux">Aucune expérience enregistrée.</p></c:if>
    <ul><c:forEach var="x" items="${fiche.experiences}">
      <li><strong><c:out value="${x.intitule}"/></strong> — <c:out value="${x.entreprise}"/> (<c:out value="${x.periode}"/>)
        <c:if test="${not empty x.description}"><br><small class="texte-doux"><c:out value="${x.description}"/></small></c:if></li>
    </c:forEach></ul>
  </div></div>

  <c:if test="${rh and fiche.actif}">
    <div class="fiche" style="margin-top:var(--e-6);"><div class="fiche-corps">
      <h2>Autres actions RH</h2>
      <div class="grille-2">
        <ui:formulaire action="/rh/employes/date-entree">
          <h3>Renseigner ou corriger la date d’entrée</h3>
          <input type="hidden" name="dossierId" value="${fiche.dossierId}">
          <input type="hidden" name="depuisFiche" value="true">
          <label for="entree-fiche">Date réelle d’entrée</label>
          <input class="saisie" id="entree-fiche" type="date" name="dateEntree" required>
          <ui:bouton label="Enregistrer la date" type="submit" style="btn-secondaire"/>
        </ui:formulaire>
        <ui:formulaire action="/rh/employes/${fiche.dossierId}/promouvoir">
          <h3>Promouvoir / muter directement</h3>
          <input type="hidden" name="depuisFiche" value="true">
          <ui:champ-liste label="Nouveau poste" name="posteCibleId" items="${postes}" valueProperty="id" labelProperty="intitule" requis="true"/>
          <ui:champ label="Date d’effet" name="dateEffet" type="date"/>
          <ui:bouton label="Appliquer la promotion" type="submit" style="btn-principal"/>
        </ui:formulaire>
      </div>
    </div></div>
  </c:if>

  <%-- ================= JOURNAL D'AUDIT ================= --%>
  <c:if test="${h.audit}">
    <div class="fiche" id="audit" style="margin-top:var(--e-6);"><div class="fiche-corps">
      <h2>Journal d’audit</h2>
      <p class="texte-doux">Consultations et modifications du dossier (100 dernières). Le journal ne peut pas être modifié depuis l’application.</p>
      <c:if test="${empty fiche.audit}"><p>Aucune entrée.</p></c:if>
      <c:if test="${not empty fiche.audit}">
        <div style="overflow-x:auto"><table class="tableau">
          <thead><tr><th scope="col">Date</th><th scope="col">Par</th><th scope="col">Action</th><th scope="col">Détail</th></tr></thead>
          <tbody><c:forEach var="j" items="${fiche.audit}"><tr>
            <td><c:out value="${j.horodatage}"/></td>
            <td><c:out value="${j.acteur}"/><c:if test="${not empty j.role}"><br><small class="texte-doux"><c:out value="${j.role}"/></small></c:if></td>
            <td><c:out value="${j.action}"/></td><td><small><c:out value="${j.detail}"/></small></td>
          </tr></c:forEach></tbody>
        </table></div>
      </c:if>
    </div></div>
  </c:if>
</ui:layout>
