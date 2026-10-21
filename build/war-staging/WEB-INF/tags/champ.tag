<%@ tag description="Champ de formulaire texte/email/password/date/number uniforme." pageEncoding="UTF-8" trimDirectiveWhitespaces="true" %>
<%@ attribute name="label" required="true" %>
<%@ attribute name="name" required="true" %>
<%@ attribute name="type" required="false" %>
<%@ attribute name="value" required="false" %>
<%@ attribute name="requis" required="false" %>
<%@ attribute name="aide" required="false" %>
<%@ attribute name="erreur" required="false" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<div class="champ">
  <label class="champ-libelle" for="champ-${name}">
    <c:out value="${label}"/>
    <c:if test="${requis != 'true'}"> <span class="facultatif">(facultatif)</span></c:if>
  </label>
  <input class="saisie ${not empty erreur ? 'invalide' : ''}" id="champ-${name}" name="${name}"
         type="${not empty type ? type : 'text'}" value="${value}"
         ${requis == 'true' ? 'required' : ''}/>
  <c:if test="${not empty aide}"><span class="champ-aide"><c:out value="${aide}"/></span></c:if>
  <c:if test="${not empty erreur}"><span class="champ-erreur"><c:out value="${erreur}"/></span></c:if>
</div>
