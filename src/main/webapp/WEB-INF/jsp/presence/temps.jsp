<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="ui" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<ui:layout title="Temps de travail" activeMenu="presence">
  <h1>Temps de travail</h1>
  <c:if test="${not empty succes}"><p role="status"><c:out value="${succes}"/></p></c:if>
  <c:if test="${not empty erreur}"><ui:alerte type="erreur"><c:out value="${erreur}"/></ui:alerte></c:if>
  <form method="get" class="presence-filtres">
    <c:if test="${estRh}"><div class="champ"><label for="presence-salarie">Salarié</label><select class="saisie" id="presence-salarie" name="dossierId" required><option value="">Choisir un salarié</option><c:forEach var="d" items="${dossiers}"><option value="${d.id}" ${d.id == dossier.id ? 'selected' : ''}><c:out value="${d.prenom} ${d.nom}"/></option></c:forEach></select></div></c:if>
    <ui:champ label="Du" name="debut" type="date" value="${debut}"/>
    <ui:champ label="Au" name="fin" type="date" value="${fin}"/>
    <ui:bouton label="Afficher" type="submit" style="btn-secondaire"/>
  </form>
  <c:if test="${dossier != null}">
    <p class="texte-doux"><c:out value="${dossier.prenom} ${dossier.nom}"/></p>
    <c:if test="${not estRh}">
      <section class="fiche fiche-corps presence-pointage">
        <h2>Pointer aujourd’hui</h2>
        <c:set var="s" value="${situation}"/>
        <c:choose>
          <c:when test="${s.entreePrevue != null}">
            <p>Votre date d’entrée est le <ui:date valeur="${s.entreePrevue}"/> : le pointage sera possible à partir de ce jour.</p>
          </c:when>
          <c:when test="${s.ouvertAnterieur != null}">
            <ui:alerte type="alerte">Votre pointage du <ui:date valeur="${s.ouvertAnterieur.jour}"/> (entrée enregistrée le
              <ui:date valeur="${s.ouvertAnterieur.entree}" heure="true"/>) n’a pas de sortie. Une sortie ne peut plus être
              saisie un autre jour : demandez aux RH de corriger ce pointage pour pouvoir pointer de nouveau.</ui:alerte>
          </c:when>
          <c:when test="${s.sortiePossible}">
            <p>Entrée : <ui:date valeur="${s.ouvertDuJour.entree}" heure="true"/></p>
            <ui:formulaire action="/presence/sortie" cssClass="presence-filtres">
              <ui:champ label="Pause prise (min)" name="pauseMinutes" type="number" min="0" value="${s.ouvertDuJour.pauseMinutes}" requis="true"/>
              <ui:bouton label="Enregistrer la sortie" type="submit" style="btn-principal"/>
            </ui:formulaire>
          </c:when>
          <c:when test="${s.termineDuJour != null}">
            <p>Pointage du jour terminé : entrée <ui:date valeur="${s.termineDuJour.entree}" heure="true"/>,
              sortie <ui:date valeur="${s.termineDuJour.sortie}" heure="true"/>. Pour une correction, adressez-vous aux RH.</p>
          </c:when>
          <c:when test="${s.absence != null}">
            <p>Absence enregistrée aujourd’hui (<c:out value="${s.absence}"/>) : aucun pointage à faire.</p>
          </c:when>
          <c:otherwise><ui:formulaire action="/presence/entree"><ui:bouton label="Enregistrer l’entrée" type="submit" style="btn-principal"/></ui:formulaire></c:otherwise>
        </c:choose>
      </section>
    </c:if>
    <div class="presence-indicateurs">
      <div><span>Travaillé</span><strong>${travaillees} min</strong></div>
      <div><span>Retards</span><strong>${retards} min</strong></div>
      <div><span>Supplémentaires validées</span><strong>${supplementaires} min</strong></div>
    </div>
    <section class="fiche fiche-corps">
      <div class="rangee" style="justify-content:space-between;"><h2>Planning et pointages</h2>
        <c:if test="${estRh}"><a class="btn btn-secondaire" href="${pageContext.request.contextPath}/rh/presence/variables.csv?dossierId=${dossier.id}&amp;debut=${debut}&amp;fin=${fin}">Variables de paie · CSV</a></c:if>
      </div>
      <table class="tableau">
        <caption>Durées en minutes. Dépassements soumis à validation RH.</caption>
        <thead><tr><th>Jour</th><th>Horaire</th><th>Entrée</th><th>Sortie</th><th>Travaillé</th><th>Retard</th><th>Dépassement</th><th>Suivi</th></tr></thead>
        <tbody><c:forEach var="j" items="${jours}"><tr>
          <td><ui:date valeur="${j.date}"/></td><td><c:out value="${j.horaire}"/></td><td><c:out value="${j.entree}"/></td><td><c:out value="${j.sortie}"/></td>
          <td>${j.travaillees}</td><td>${j.retard}</td><td>${j.supplementaires}</td><td><c:out value="${j.absence}"/><c:if test="${not empty j.anomalie}"><span class="statut statut--attente"><c:out value="${j.anomalie}"/></span></c:if></td>
        </tr></c:forEach></tbody>
      </table>
    </section>
    <c:if test="${estRh}">
      <details class="fiche lecture-section"><summary>Horaires habituels</summary><div class="fiche-corps">
        <p class="texte-doux">Par défaut : lundi–vendredi, 09 h–18 h, pause de 60 min.</p>
        <ui:formulaire action="/rh/presence/regime">
          <input type="hidden" name="dossierId" value="${dossier.id}">
          <div class="grille-2"><ui:champ label="À partir du" name="depuis" type="date" value="${aujourdhui}" requis="true"/>
            <ui:champ label="Début" name="debut" type="time" value="09:00" requis="true"/><ui:champ label="Fin" name="fin" type="time" value="18:00" requis="true"/>
            <ui:champ label="Pause prévue (min)" name="pauseMinutes" type="number" min="0" value="60" requis="true"/></div>
          <fieldset class="besoin-champs"><legend>Jours travaillés</legend><div class="rangee">
            <label><input type="checkbox" name="semaine" value="1" checked> Lun</label><label><input type="checkbox" name="semaine" value="2" checked> Mar</label><label><input type="checkbox" name="semaine" value="3" checked> Mer</label><label><input type="checkbox" name="semaine" value="4" checked> Jeu</label><label><input type="checkbox" name="semaine" value="5" checked> Ven</label><label><input type="checkbox" name="semaine" value="6"> Sam</label><label><input type="checkbox" name="semaine" value="7"> Dim</label>
          </div></fieldset>
          <ui:bouton label="Enregistrer le régime" type="submit" style="btn-principal"/>
        </ui:formulaire>
      </div></details>
      <details class="fiche lecture-section"><summary>Modifier un jour du planning</summary><div class="fiche-corps">
        <ui:formulaire action="/rh/presence/planning"><input type="hidden" name="dossierId" value="${dossier.id}">
          <ui:champ label="Jour" name="jour" type="date" requis="true"/>
          <label><input type="checkbox" name="repos" value="true"> Jour non travaillé</label>
          <div class="grille-2"><ui:champ label="Début" name="debut" type="time"/><ui:champ label="Fin" name="fin" type="time"/></div>
          <ui:champ label="Pause (min)" name="pauseMinutes" type="number" min="0" value="0"/>
          <ui:bouton label="Enregistrer le planning" type="submit" style="btn-principal"/>
        </ui:formulaire>
      </div></details>
      <details class="fiche lecture-section"><summary>Corriger un pointage</summary><div class="fiche-corps">
        <ui:formulaire action="/rh/presence/correction"><input type="hidden" name="dossierId" value="${dossier.id}">
          <div class="grille-2"><ui:champ label="Jour" name="jour" type="date" requis="true"/><ui:champ label="Entrée" name="entree" type="time" requis="true"/><ui:champ label="Sortie" name="sortie" type="time" requis="true"/><ui:champ label="Pause réelle (min)" name="pauseMinutes" type="number" min="0" value="60" requis="true"/></div>
          <ui:champ label="Supplémentaires validées (min)" name="supplementairesValidees" type="number" min="0" value="0" requis="true"/>
          <ui:champ label="Motif" name="motif" maxlength="255" requis="true"/>
          <ui:bouton label="Enregistrer la correction" type="submit" style="btn-principal"/>
        </ui:formulaire>
      </div></details>
    </c:if>
  </c:if>
</ui:layout>
