(() => {
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
})();
