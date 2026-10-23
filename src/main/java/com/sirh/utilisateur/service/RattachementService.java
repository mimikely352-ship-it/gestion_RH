package com.sirh.utilisateur.service;

import com.sirh.commun.enums.Role;
import com.sirh.commun.exception.BusinessException;
import com.sirh.organisation.repository.ServiceRepository;
import com.sirh.utilisateur.repository.UtilisateurRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RattachementService {
    private final UtilisateurRepository utilisateurs;
    private final ServiceRepository services;

    @Transactional
    public void completer(Long employeId, Long serviceId) {
        var employe = utilisateurs.findById(employeId)
                .orElseThrow(() -> new BusinessException("Employé introuvable."));
        if (employe.getRole() != Role.EMPLOYE || !employe.isActif()) {
            throw new BusinessException("Ce compte n’est pas un employé actif.");
        }
        if (employe.getService() != null) {
            throw new BusinessException("Cet employé possède déjà un service. Un changement doit suivre le circuit de transfert.");
        }
        var service = services.findById(serviceId)
                .orElseThrow(() -> new BusinessException("Service introuvable."));
        var chef = service.getChefService();
        if (chef == null || chef.getRole() != Role.CHEF_SERVICE || !chef.isActif()) {
            throw new BusinessException("Choisissez un service avec un chef de service actif pour permettre la validation des demandes.");
        }
        employe.setService(service);
        utilisateurs.save(employe);
    }
}
