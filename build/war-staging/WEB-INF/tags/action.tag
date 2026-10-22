<%@ tag description="Lien d'action pour une ligne de <ui:list>. 'url' peut contenir {id}, remplacé par row.id (ou row[idProperty] si idProperty est fourni, ex. 'candidatureId'). 'row' doit être passé explicitement (ex. row='${row}') : les fichiers .tag isolent leurs variables de script, donc le 'row' exposé par <c:forEach> dans list.tag n'est pas visible ici autrement." pageEncoding="UTF-8" trimDirectiveWhitespaces="true" %>
<%@ attribute name="label" required="true" %>
<%@ attribute name="url" required="true" %>
<%@ attribute name="row" required="true" type="java.lang.Object" %>
<%@ attribute name="style" required="false" %>
<%@ attribute name="confirmation" required="false" %>
<%@ attribute name="idProperty" required="false" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<c:set var="identifiant" value="${not empty idProperty ? row[idProperty] : row.id}"/>
<a class="btn btn-petit ${not empty style ? style : 'btn-secondaire'}"
   href="${pageContext.request.contextPath}${fn:replace(url, '{id}', identifiant)}"
   ${not empty confirmation ? 'onclick="return confirm(\'' : ''}${not empty confirmation ? confirmation : ''}${not empty confirmation ? '\');"' : ''}
><c:out value="${label}"/></a>
