<%@ tag description="Lien d'action pour une ligne de <ui:list>. 'url' peut contenir {id}, remplacé par row.id de la ligne courante." pageEncoding="UTF-8" trimDirectiveWhitespaces="true" %>
<%@ attribute name="label" required="true" %>
<%@ attribute name="url" required="true" %>
<%@ attribute name="style" required="false" %>
<%@ attribute name="confirmation" required="false" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<a class="btn btn-petit ${not empty style ? style : 'btn-secondaire'}"
   href="${pageContext.request.contextPath}${fn:replace(url, '{id}', row.id)}"
   ${not empty confirmation ? 'onclick="return confirm(\'' : ''}${not empty confirmation ? confirmation : ''}${not empty confirmation ? '\');"' : ''}
><c:out value="${label}"/></a>
