// Filtre les offres par recherche texte + boutons de filtre
let filtreActif = 'tous';

function filtrerOffres() {
  const q = document.getElementById('recherche').value.toLowerCase().trim();
  const cartes = document.querySelectorAll('.offre-carte');
  let visible = 0;

  cartes.forEach(carte => {
    const texte = carte.textContent.toLowerCase();
    const tags  = carte.dataset.tags.toLowerCase();
    const contrat = carte.dataset.contrat;

    const matchTexte = !q || texte.includes(q) || tags.includes(q);
    const matchFiltre = filtreActif === 'tous'
      || contrat === filtreActif
      || carte.dataset.tags.includes(filtreActif);

    const ok = matchTexte && matchFiltre;
    carte.style.display = ok ? '' : 'none';
    if (ok) visible++;
  });

  document.getElementById('compteur-offres').textContent =
    visible + ' offre' + (visible > 1 ? 's' : '') + ' disponible' + (visible > 1 ? 's' : '');
  document.getElementById('site-vide').hidden = visible > 0;
  document.getElementById('grille-offres').style.display = visible > 0 ? '' : 'none';
}

function reinitialiserFiltres() {
  document.getElementById('recherche').value = '';
  filtreActif = 'tous';
  document.querySelectorAll('.filtre').forEach(f => f.classList.remove('actif'));
  document.querySelector('.filtre[data-filtre="tous"]').classList.add('actif');
  filtrerOffres();
}

document.querySelectorAll('.filtre').forEach(btn => {
  btn.addEventListener('click', () => {
    document.querySelectorAll('.filtre').forEach(f => f.classList.remove('actif'));
    btn.classList.add('actif');
    filtreActif = btn.dataset.filtre;
    filtrerOffres();
  });
});

// Recherche en temps réel
document.getElementById('recherche').addEventListener('input', filtrerOffres);