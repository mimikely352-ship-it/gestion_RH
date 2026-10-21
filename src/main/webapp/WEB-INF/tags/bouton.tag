<%@ tag description="Bouton/lien uniforme. Si 'href' est fourni, rend un <a>, sinon un <button> (utile dans un <ui:formulaire>)." pageEncoding="UTF-8" trimDirectiveWhitespaces="true" %>
<%@ attribute name="label" required="true" %>
<%@ attribute name="href" required="false" %>
<%@ attribute name="type" required="false" %>
<%@ attribute name="style" required="false" %>
<%@ attribute name="confirmation" required="false" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="classe" value="btn ${not empty style ? style : 'btn-principal'}"/>
<c:choose>
  <c:when test="${not empty href}">
    <a class="${classe}" href="${pageContext.request.contextPath}${href}"
       ${not empty confirmation ? 'onclick="return confirm(\'' : ''}${not empty confirmation ? confirmation : ''}${not empty confirmation ? '\');"' : ''}
    ><c:out value="${label}"/></a>
  </c:when>
  <c:otherwise>
    <button class="${classe}" type="${not empty type ? type : 'submit'}"
       ${not empty confirmation ? 'onclick="return confirm(\'' : ''}${not empty confirmation ? confirmation : ''}${not empty confirmation ? '\');"' : ''}
    ><c:out value="${label}"/></button>
  </c:otherwise>
</c:choose>
