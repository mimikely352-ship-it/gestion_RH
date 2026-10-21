<%@ tag description="Formulaire uniforme : pose automatiquement le jeton CSRF. <ui:formulaire action='/rh/offres' > ... </ui:formulaire>" pageEncoding="UTF-8" trimDirectiveWhitespaces="true" %>
<%@ attribute name="action" required="true" %>
<%@ attribute name="method" required="false" %>
<%@ attribute name="enctype" required="false" %>
<%@ attribute name="cssClass" required="false" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<form action="${pageContext.request.contextPath}${action}" method="${not empty method ? method : 'post'}"
      <c:if test="${not empty enctype}">enctype="${enctype}"</c:if> class="${not empty cssClass ? cssClass : 'pile'}">
  <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}"/>
  <jsp:doBody/>
</form>
