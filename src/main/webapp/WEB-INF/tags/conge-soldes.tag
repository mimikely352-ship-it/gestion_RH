<%@ tag pageEncoding="UTF-8" %>
<%@ attribute name="soldes" required="true" type="java.util.Collection" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<section class="fiche fiche-corps"><h2>Soldes</h2>
  <table class="tableau"><thead><tr><th>Type</th><th>Année</th><th>Droits</th><th>Réservés</th><th>Acceptés</th><th>Disponibles</th></tr></thead><tbody>
    <c:forEach var="s" items="${soldes}"><tr><td><c:out value="${s.type.nom}"/></td><td>${s.annee}</td>
      <c:choose><c:when test="${s.type.deduitSolde}"><td>${s.droits}</td><td>${s.reserves}</td><td>${s.consommes}</td><td><strong>${s.disponible}</strong></td></c:when><c:otherwise><td colspan="4">Sans décompte</td></c:otherwise></c:choose>
    </tr></c:forEach>
  </tbody></table>
  <c:forEach var="s" items="${soldes}">
    <c:if test="${s.type.deduitSolde and s.droits == 0}"><p class="champ-aide"><c:out value="${s.type.nom}"/> : aucun droit attribué pour ${s.annee}. Contactez les RH.</p></c:if>
  </c:forEach>
</section>
