<%@ tag description="Zone de texte libre (textarea) uniforme." pageEncoding="UTF-8" trimDirectiveWhitespaces="true" %>
<%@ attribute name="label" required="true" %>
<%@ attribute name="name" required="true" %>
<%@ attribute name="value" required="false" %>
<%@ attribute name="requis" required="false" %>
<%@ attribute name="aide" required="false" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<div class="champ">
  <label class="champ-libelle" for="champ-${name}">
    <c:out value="${label}"/>
    <c:if test="${requis != 'true'}"> <span class="facultatif">(facultatif)</span></c:if>
  </label>
  <textarea class="saisie" id="champ-${name}" name="${name}" ${requis == 'true' ? 'required' : ''}><c:out value="${value}"/></textarea>
  <c:if test="${not empty aide}"><span class="champ-aide"><c:out value="${aide}"/></span></c:if>
</div>
