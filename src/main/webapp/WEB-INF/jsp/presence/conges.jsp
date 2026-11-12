<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="ui" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<ui:layout title="Mes congés" activeMenu="conges">
  <h1>Mes congés</h1>
  <c:if test="${not empty succes}"><p role="status"><c:out value="${succes}"/></p></c:if>
  <c:if test="${not empty erreur}"><ui:alerte type="erreur"><c:out value="${erreur}"/></ui:alerte></c:if>
  <form method="get" class="presence-filtres"><ui:champ label="Année" name="annee" type="number" min="2000" value="${annee}" requis="true"/><ui:bouton label="Afficher les soldes" type="submit" style="btn-secondaire"/></form>
  <ui:conge-soldes soldes="${soldes}"/>
  <details class="fiche lecture-section" ${not empty congeSaisie ? 'open' : ''}><summary>Demander un congé</summary><div class="fiche-corps">
    <p class="texte-doux"><c:out value="${modeDecompte}"/> · Journées entières · Une année par demande.</p>
    <ui:formulaire action="/conges" enctype="multipart/form-data">
      <div class="champ"><label for="conge-type">Type</label><select class="saisie" id="conge-type" name="typeId" required><c:forEach var="t" items="${types}"><option value="${t.id}" data-justificatif="${t.justificatifRequis}" ${congeSaisie.typeId == t.id ? 'selected' : ''}><c:out value="${t.nom}"/>${t.justificatifRequis ? ' · PDF requis' : ''}</option></c:forEach></select></div>
      <div class="grille-2"><ui:champ label="Du" name="debut" type="date" min="${aujourdhui}" value="${congeSaisie.debut}" requis="true"/><ui:champ label="Au" name="fin" type="date" min="${aujourdhui}" value="${congeSaisie.fin}" requis="true"/></div>
      <ui:champ-texte label="Motif" name="motif" value="${congeSaisie.motif}" requis="true"/>
      <div class="champ"><label for="conge-piece">Justificatif PDF · 5 Mo maximum</label><input class="saisie" type="file" name="justificatif" id="conge-piece" accept="application/pdf"></div>
      <p id="conge-piece-aide" class="texte-doux" aria-live="polite">Le justificatif est obligatoire pour les types indiqués « PDF requis ».</p>
      <c:if test="${pieceAReselectionner}"><p role="status">Veuillez sélectionner à nouveau le justificatif PDF ; les autres informations ont été conservées.</p></c:if>
      <ui:bouton label="Soumettre la demande" type="submit" style="btn-principal"/>
    </ui:formulaire>
  </div></details>
  <ui:conge-historique demandes="${demandes}" dossierId="${dossier.id}" aujourdhui="${aujourdhui}"/>
  <c:if test="${not empty absences}"><section class="fiche fiche-corps"><h2>Autres absences</h2><table class="tableau"><thead><tr><th>Période</th><th>Motif</th><th>Justificatif</th></tr></thead><tbody>
    <c:forEach var="a" items="${absences}"><tr><td><ui:date valeur="${a.debut}"/> → <ui:date valeur="${a.fin}"/></td><td><c:out value="${a.libelle}"/></td><td><c:if test="${a.justificatifChemin != null}"><a href="${pageContext.request.contextPath}/absences/${a.id}/justificatif">PDF</a></c:if></td></tr></c:forEach>
  </tbody></table></section></c:if>
</ui:layout>
