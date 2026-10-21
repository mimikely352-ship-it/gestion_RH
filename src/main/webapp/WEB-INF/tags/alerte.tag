<%@ tag description="Message d'alerte générique : <ui:alerte type='erreur'>...</ui:alerte>. type = info|succes|alerte|erreur" pageEncoding="UTF-8" trimDirectiveWhitespaces="true" %>
<%@ attribute name="type" required="false" %>
<div class="alerte alerte--${not empty type ? type : 'info'}">
  <div><jsp:doBody/></div>
</div>
