(() => {
  const typeDemande = document.getElementById('champ-type');
  const besoinRecrutement = document.getElementById('besoin-recrutement');
  if (typeDemande && besoinRecrutement) {
    const actualiserBesoin = () => {
      const recrutement = typeDemande.value === 'RECRUTEMENT';
      besoinRecrutement.hidden = !recrutement;
      besoinRecrutement.disabled = !recrutement;
      besoinRecrutement.querySelectorAll('[data-besoin-requis]').forEach(champ => { champ.required = recrutement; });
      const poste = document.getElementById('champ-posteCibleId');
      if (poste) poste.required = recrutement || ['TRANSFERT', 'PROMOTION'].includes(typeDemande.value);
    };
    typeDemande.addEventListener('change', actualiserBesoin);
    actualiserBesoin();
  }
  const employesRecherche = document.getElementById('recherche-employes');
  if (employesRecherche) {
    const lignes = [...document.querySelectorAll('[data-employe]')];
    employesRecherche.addEventListener('input', () => {
      const normaliser = valeur => valeur.normalize('NFD').replace(/[\u0300-\u036f]/g, '').toLowerCase();
      const terme = normaliser(employesRecherche.value.trim());
      lignes.forEach(ligne => { ligne.hidden = !normaliser(ligne.textContent).includes(terme); });
      document.getElementById('resultats-employes').textContent = lignes.filter(ligne => !ligne.hidden).length + ' résultat(s) sur cette page';
    });
    const onglets = [...document.querySelectorAll('[data-employes-target]')];
    const afficher = id => {
      document.querySelectorAll('[data-employes-panel]').forEach(panel => { panel.hidden = panel.id !== id; });
      onglets.forEach(lien => {
        if (lien.dataset.employesTarget === id) lien.setAttribute('aria-current', 'true');
        else lien.removeAttribute('aria-current');
      });
      employesRecherche.closest('.employes-recherche').hidden = id !== 'effectif';
      document.querySelector('.employes-pagination').hidden = id !== 'effectif';
    };
    onglets.forEach(lien => lien.addEventListener('click', event => { event.preventDefault(); afficher(lien.dataset.employesTarget); }));
    afficher(location.hash === '#departs' ? 'departs' : 'effectif');
  }
  document.querySelectorAll('.employes-menu').forEach(menu => {
    menu.addEventListener('toggle', () => {
      if (menu.open) document.querySelectorAll('.employes-menu[open]').forEach(autre => { if (autre !== menu) autre.open = false; });
    });
  });
  document.addEventListener('click', event => {
    document.querySelectorAll('.employes-menu[open]').forEach(menu => { if (!menu.contains(event.target)) menu.open = false; });
  });
  document.addEventListener('keydown', event => {
    if (event.key === 'Escape') document.querySelectorAll('.employes-menu[open]').forEach(menu => { menu.open = false; menu.querySelector('summary').focus(); });
  });
  // Keep large tables inside their own scroll area on small screens.
  document.querySelectorAll('.tableau').forEach(table => {
    if (table.closest('.tableau-cadre, .lecture-tableau, [style*="overflow-x"]')) return;
    const cadre = document.createElement('div');
    cadre.className = 'lecture-tableau';
    cadre.tabIndex = 0;
    cadre.setAttribute('role', 'region');
    cadre.setAttribute('aria-label', table.caption?.textContent.trim() || 'Tableau de données');
    table.before(cadre);
    cadre.append(table);
  });
  const ouvrirParents = element => {
    for (let parent = element; parent; parent = parent.parentElement) {
      if (parent instanceof HTMLDetailsElement) parent.open = true;
    }
  };
  const ouvrirAncre = () => {
    if (!location.hash) return;
    let id;
    try { id = decodeURIComponent(location.hash.slice(1)); } catch { return; }
    const cible = document.getElementById(id);
    if (cible) { ouvrirParents(cible); cible.scrollIntoView({block: 'start'}); }
  };
  ouvrirAncre();
  window.addEventListener('hashchange', ouvrirAncre);
  // Required fields remain reachable when a secondary form is folded.
  document.addEventListener('invalid', event => ouvrirParents(event.target), true);
  const identite = document.getElementById('identite');
  // Rubriques repliables (.lecture-section) et blocs signalés par data-sommaire, dans l'ordre de la page.
  const sections = [...document.querySelectorAll('.lecture-section[id], [data-sommaire][id]')];
  if (identite && sections.length >= 3) {
    const sommaire = document.createElement('nav');
    sommaire.className = 'lecture-sommaire';
    sommaire.setAttribute('aria-label', 'Rubriques du dossier');
    [{id: 'identite', titre: 'Identité'}, ...sections.map(section => ({
      id: section.id,
      titre: section.dataset.sommaire || section.querySelector(':scope > summary')?.textContent.trim() || section.id
    }))].forEach(rubrique => {
      const lien = document.createElement('a');
      lien.href = '#' + rubrique.id;
      lien.textContent = rubrique.titre;
      lien.addEventListener('click', () => ouvrirParents(document.getElementById(rubrique.id)));
      sommaire.append(lien);
    });
    identite.before(sommaire);
  }
  document.querySelectorAll('[data-recherche-liste]').forEach(champ => {
    const lignes = [...document.querySelectorAll('[data-liste="' + champ.dataset.rechercheListe + '"]')];
    const resultat = document.querySelector('[data-resultats="' + champ.dataset.rechercheListe + '"]');
    const normaliser = s => s.normalize('NFD').replace(/[\u0300-\u036f]/g, '').toLowerCase();
    champ.addEventListener('input', () => {
      const mot = normaliser(champ.value.trim()); let compte = 0;
      lignes.forEach(l => { l.hidden = !normaliser(l.textContent).includes(mot); if (!l.hidden) compte++; });
      resultat.textContent = compte ? compte + ' résultat(s)' : 'Aucun résultat pour cette recherche.';
    });
  });
  const bouton = document.querySelector('.interface-menu');
  const navigation = document.getElementById('navigation-principale');
  if (bouton && navigation) {
    document.documentElement.classList.add('interface-js');
    const fermer = () => { bouton.setAttribute('aria-expanded', 'false'); navigation.dataset.open = 'false'; };
    bouton.addEventListener('click', () => {
      const ouvert = bouton.getAttribute('aria-expanded') !== 'true';
      bouton.setAttribute('aria-expanded', String(ouvert)); navigation.dataset.open = String(ouvert);
    });
    document.addEventListener('keydown', e => {
      if (e.key === 'Escape' && bouton.getAttribute('aria-expanded') === 'true') { fermer(); bouton.focus(); }
    });
    navigation.querySelectorAll('.lateral-lien.actif').forEach(lien => lien.setAttribute('aria-current', 'page'));
  }
  const recherche = document.getElementById('recherche-offres');
  const filtre = document.getElementById('statut-offres');
  if (recherche && filtre) {
    const offres = [...document.querySelectorAll('.offre-apercu')];
    const normaliser = s => s.normalize('NFD').replace(/[\u0300-\u036f]/g, '').toLowerCase();
    const actualiser = () => {
      const mot = normaliser(recherche.value.trim());
      let visibles = 0;
      offres.forEach(offre => {
        offre.hidden = !normaliser(offre.dataset.recherche).includes(mot) || (filtre.value && offre.dataset.statut !== filtre.value);
        if (!offre.hidden) visibles++;
      });
      document.getElementById('resultats-offres').textContent = `${visibles} offre(s)`;
      document.getElementById('offres-introuvables').hidden = visibles !== 0;
    };
    recherche.addEventListener('input', actualiser);
    filtre.addEventListener('change', actualiser);
  }
  const typeConge = document.getElementById('conge-type');
  const pieceConge = document.getElementById('conge-piece');
  if (typeConge && pieceConge) {
    const actualiserPiece = () => {
      const requis = typeConge.selectedOptions[0]?.dataset.justificatif === 'true';
      pieceConge.required = requis;
      pieceConge.setAttribute('aria-required', String(requis));
      pieceConge.setAttribute('aria-describedby', 'conge-piece-aide');
      document.getElementById('conge-piece-aide').textContent = requis
        ? 'Un justificatif PDF est obligatoire pour ce type de congé (5 Mo maximum).'
        : 'Justificatif PDF facultatif pour ce type de congé (5 Mo maximum).';
    };
    typeConge.addEventListener('change', actualiserPiece);
    actualiserPiece();
    const debut = pieceConge.form?.querySelector('[name="debut"]');
    const fin = pieceConge.form?.querySelector('[name="fin"]');
    if (debut && fin) {
      const actualiserDates = () => { fin.min = debut.value || debut.min; };
      debut.addEventListener('change', actualiserDates);
      actualiserDates();
    }
  }
})();
