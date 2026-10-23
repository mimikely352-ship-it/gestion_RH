<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="ui" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<ui:layout title="Offres d’emploi" activeMenu="offres">
  <div class="offre-espace">
    <header class="offre-entete"><div><p class="offre-surtitre">RECRUTEMENT</p><h1>Offres d’emploi</h1><p class="offre-metier">Préparez vos annonces et suivez les dossiers reçus.</p></div><ui:bouton label="Créer une offre" href="/rh/offres/nouvelle" style="btn-principal"/></header>
    <c:choose><c:when test="${empty offres}">
      <section class="offre-carte offre-etat-vide"><h2>Votre première offre commence ici</h2><p>Choisissez un métier ou décrivez votre besoin, puis enregistrez votre annonce en brouillon.</p><ui:bouton label="Créer ma première offre" href="/rh/offres/nouvelle" style="btn-principal"/></section>
    </c:when><c:otherwise>
      <div class="offre-filtres">
        <div class="champ"><label class="champ-libelle" for="recherche-offres">Rechercher une offre</label><input class="saisie" id="recherche-offres" type="search" placeholder="Titre, métier ou catégorie"/></div>
        <div class="champ"><label class="champ-libelle" for="statut-offres">État de l’offre</label><select class="saisie" id="statut-offres"><option value="">Tous les états</option><option value="BROUILLON">Brouillons</option><option value="PUBLIEE">Publiées</option><option value="CLOTUREE">Clôturées</option></select></div>
      </div>
      <p class="offre-texte-secondaire" id="resultats-offres" role="status">${offres.size()} offre(s)</p>
      <div class="offre-catalogue">
        <c:forEach var="o" items="${offres}">
          <article class="offre-carte offre-apercu" data-statut="${o.statut}" data-recherche="${fn:escapeXml(o.titre)} ${fn:escapeXml(o.posteIntitule)} ${fn:escapeXml(o.poste.categorie)}">
            <ui:statut valeur="${o.statut}"/>
            <h2><a href="${pageContext.request.contextPath}/rh/offres/${o.id}"><c:out value="${o.titre}"/></a></h2>
            <p><strong><c:out value="${o.entrepriseNom}"/></strong></p>
            <p class="offre-texte-secondaire"><c:out value="${o.posteIntitule}"/></p>
            <dl><div><dt>Personnes à recruter</dt><dd>${o.nbPostes}</dd></div><div><dt>Date limite</dt><dd><c:choose><c:when test="${not empty o.dateLimite}"><fmt:parseDate value="${o.dateLimite}" pattern="yyyy-MM-dd" var="fin"/><fmt:formatDate value="${fin}" pattern="dd/MM/yyyy"/></c:when><c:otherwise>Sans date limite</c:otherwise></c:choose></dd></div></dl>
            <a class="offre-lien-dossier" href="${pageContext.request.contextPath}/rh/offres/${o.id}">Consulter l’offre <ui:icone nom="suivant"/><span class="interface-visuel-cache"> : <c:out value="${o.titre}"/></span></a>
          </article>
        </c:forEach>
      </div>
      <p class="offre-message" id="offres-introuvables" hidden>Aucune offre ne correspond à ces critères. Modifiez la recherche ou choisissez « Tous les états ».</p>
    </c:otherwise></c:choose>
  </div>
</ui:layout>
