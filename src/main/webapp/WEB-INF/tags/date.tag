<%@ tag description="Date lisible, sans exposer le format technique LocalDateTime." pageEncoding="UTF-8" trimDirectiveWhitespaces="true" %>
<%@ attribute name="valeur" required="false" %>
<%@ attribute name="heure" required="false" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<c:choose><c:when test="${empty valeur}">Non précisée</c:when><c:otherwise>
  <c:choose><c:when test="${heure == 'true'}">
    <fmt:parseDate value="${fn:substring(valeur,0,16)}" pattern="yyyy-MM-dd'T'HH:mm" var="dateLisible"/>
    <time datetime="${valeur}"><fmt:formatDate value="${dateLisible}" pattern="dd/MM/yyyy 'à' HH:mm"/></time>
  </c:when><c:otherwise>
    <fmt:parseDate value="${fn:substring(valeur,0,10)}" pattern="yyyy-MM-dd" var="dateLisible"/>
    <time datetime="${valeur}"><fmt:formatDate value="${dateLisible}" pattern="dd/MM/yyyy"/></time>
  </c:otherwise></c:choose>
</c:otherwise></c:choose>
