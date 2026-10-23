<%@ tag description="Icônes SVG locales, décoratives : le libellé est porté par le lien ou le bouton." pageEncoding="UTF-8" trimDirectiveWhitespaces="true" %>
<%@ attribute name="nom" required="true" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<svg class="oria-icone" xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true" focusable="false">
  <c:choose>
    <c:when test="${nom == 'notification'}"><path d="M18 8a6 6 0 0 0-12 0c0 7-3 7-3 9h18c0-2-3-2-3-9"/><path d="M10 21h4M12 2V1"/></c:when>
    <c:when test="${nom == 'retour'}"><path d="m12 5-7 7 7 7M5 12h15"/></c:when>
    <c:when test="${nom == 'suivant'}"><path d="m12 5 7 7-7 7M4 12h15"/></c:when>
    <c:when test="${nom == 'bas'}"><path d="m5 12 7 7 7-7M12 4v15"/></c:when>
    <c:when test="${nom == 'valide'}"><path d="m5 12 4 4L19 6"/></c:when>
    <c:otherwise><path d="M3 7V5a2 2 0 0 1 2-2h5l2 3h7a2 2 0 0 1 2 2v11a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V7Z"/><path d="M3 9h18"/></c:otherwise>
  </c:choose>
</svg>
