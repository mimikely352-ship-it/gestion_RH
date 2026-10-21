<%@ tag description="Liste déroulante alimentée depuis la base : <ui:champ-liste label='Diplôme' name='diplomeIds' items='${diplomes}' valueProperty='id' labelProperty='libelle' multiple='true'/>" pageEncoding="UTF-8" trimDirectiveWhitespaces="true" %>
<%@ attribute name="label" required="true" %>
<%@ attribute name="name" required="true" %>
<%@ attribute name="items" required="true" type="java.util.Collection" %>
<%@ attribute name="valueProperty" required="true" %>
<%@ attribute name="labelProperty" required="true" %>
<%@ attribute name="selected" required="false" type="java.lang.Object" %>
<%@ attribute name="requis" required="false" %>
<%@ attribute name="multiple" required="false" %>
<%@ attribute name="aide" required="false" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<div class="champ">
  <label class="champ-libelle" for="champ-${name}"><c:out value="${label}"/></label>
  <select class="saisie" id="champ-${name}" name="${name}"
          ${requis == 'true' ? 'required' : ''} ${multiple == 'true' ? 'multiple' : ''}>
    <c:if test="${multiple != 'true'}"><option value="">—</option></c:if>
    <c:forEach var="item" items="${items}">
      <option value="${item[valueProperty]}" ${selected == item[valueProperty] ? 'selected' : ''}>
        <c:out value="${item[labelProperty]}"/>
      </option>
    </c:forEach>
  </select>
  <c:if test="${not empty aide}"><span class="champ-aide"><c:out value="${aide}"/></span></c:if>
</div>
