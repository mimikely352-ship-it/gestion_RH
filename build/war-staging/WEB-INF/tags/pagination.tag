<%@ tag description="Pagination : <ui:pagination page='${page}' totalPages='${n}' baseUrl='/rh/candidatures?statut=X'/>" pageEncoding="UTF-8" trimDirectiveWhitespaces="true" %>
<%@ attribute name="page" required="true" type="java.lang.Integer" %>
<%@ attribute name="totalPages" required="true" type="java.lang.Integer" %>
<%@ attribute name="baseUrl" required="true" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<c:if test="${totalPages > 1}">
  <nav class="pagination" aria-label="Pagination">
    <c:set var="sep" value="${fn:contains(baseUrl, '?') ? '&' : '?'}"/>
    <a class="${page <= 0 ? 'desactive' : ''}" href="${pageContext.request.contextPath}${baseUrl}${sep}page=${page - 1}">‹</a>
    <c:forEach begin="0" end="${totalPages - 1}" varStatus="st">
      <c:choose>
        <c:when test="${st.index == page}"><span class="courant">${st.index + 1}</span></c:when>
        <c:otherwise><a href="${pageContext.request.contextPath}${baseUrl}${sep}page=${st.index}">${st.index + 1}</a></c:otherwise>
      </c:choose>
    </c:forEach>
    <a class="${page >= totalPages - 1 ? 'desactive' : ''}" href="${pageContext.request.contextPath}${baseUrl}${sep}page=${page + 1}">›</a>
  </nav>
</c:if>
