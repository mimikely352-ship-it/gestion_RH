package com.sirh.organisation.controller;
import com.sirh.organisation.repository.*;
import com.sirh.commun.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
@Controller @RequiredArgsConstructor @PreAuthorize("hasRole('RH')")
@RequestMapping("/rh/mobilite")
public class MobiliteController {
    private final PosteRepository postes;
    private final SectionRepository sections;
    @GetMapping public String liste(Model model) {
        model.addAttribute("postes", postes.findAll()); model.addAttribute("sections", sections.findAll());
        return "rh/mobilite";
    }
    @PostMapping @Transactional
    public String modifier(@RequestParam Long posteId, @RequestParam Long sectionId,
            @RequestParam(defaultValue = "false") boolean ouverte, RedirectAttributes flash) {
        var poste = postes.findById(posteId).orElseThrow(() -> new BusinessException("Poste introuvable."));
        var section = sections.findById(sectionId).orElseThrow(() -> new BusinessException("Section introuvable."));
        poste.setSection(section); poste.setMobiliteOuverte(ouverte); postes.save(poste);
        flash.addFlashAttribute("succes", "Poste mis à jour. Les affectations passées conservent leur service d’origine.");
        return "redirect:/rh/mobilite";
    }
}
