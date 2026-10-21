<%@ tag description="Modale simple : <ui:modale id='m1' titre='Confirmer'>...</ui:modale>. Ouverture via onclick=\"document.getElementById('m1').hidden=false\"." pageEncoding="UTF-8" trimDirectiveWhitespaces="true" %>
<%@ attribute name="id" required="true" %>
<%@ attribute name="titre" required="true" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<div class="modal-overlay" id="${id}" hidden>
  <div class="modal" role="dialog" aria-modal="true" aria-labelledby="${id}-titre">
    <div class="modal-entete">
      <h3 id="${id}-titre"><c:out value="${titre}"/></h3>
      <button type="button" class="btn btn-discret btn-petit" onclick="document.getElementById('${id}').hidden=true;">✕</button>
    </div>
    <div class="modal-corps"><jsp:doBody/></div>
  </div>
</div>
