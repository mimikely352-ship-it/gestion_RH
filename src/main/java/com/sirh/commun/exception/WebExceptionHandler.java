package com.sirh.commun.exception;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.http.HttpStatus;
@ControllerAdvice(annotations = org.springframework.stereotype.Controller.class)
@org.springframework.core.annotation.Order(1)
public class WebExceptionHandler {
    @ExceptionHandler(BusinessException.class)
    ModelAndView metier(BusinessException ex) { return page(HttpStatus.CONFLICT, ex.getMessage()); }
    @ExceptionHandler(ResourceNotFoundException.class)
    ModelAndView absent(ResourceNotFoundException ex) { return page(HttpStatus.NOT_FOUND, "Ce dossier n’existe pas ou n’est plus disponible."); }
    @ExceptionHandler(org.springframework.security.access.AccessDeniedException.class)
    ModelAndView interdit(Exception ex) { return page(HttpStatus.FORBIDDEN, "Votre compte ne peut pas effectuer cette action."); }
    @ExceptionHandler({org.springframework.dao.OptimisticLockingFailureException.class, jakarta.persistence.OptimisticLockException.class})
    ModelAndView conflit(Exception ex) { return page(HttpStatus.CONFLICT, "Ce dossier a changé. Rechargez la page avant de réessayer."); }
    @ExceptionHandler(org.springframework.dao.DataIntegrityViolationException.class)
    ModelAndView integrite(Exception ex) { return page(HttpStatus.CONFLICT, "Une donnée a déjà été enregistrée ou n’est plus disponible. Rechargez le dossier."); }
    @ExceptionHandler({IllegalArgumentException.class, org.springframework.web.bind.MissingServletRequestParameterException.class})
    ModelAndView saisie(Exception ex) { return page(HttpStatus.BAD_REQUEST, "Vérifiez les champs et dates du formulaire."); }
    @ExceptionHandler({org.springframework.validation.BindException.class, org.springframework.web.method.annotation.MethodArgumentTypeMismatchException.class})
    ModelAndView format(Exception ex) { return page(HttpStatus.BAD_REQUEST, "Certains champs ont un format invalide. Vérifiez votre saisie."); }
    private ModelAndView page(HttpStatus statut, String message) {
        var vue = new ModelAndView("commun/erreur"); vue.setStatus(statut); vue.addObject("messageErreur", message); return vue;
    }
}
