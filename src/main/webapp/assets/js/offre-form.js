(() => {
  const racine = document.querySelector('.offre-assistant');
  if (!racine) return;
  const form = racine.querySelector('form');
  const etapes = [...form.querySelectorAll('[data-etape]')];
  const champ = nom => form.elements.namedItem(nom);
  const titre = champ('titre');
  let etape = Number(racine.dataset.etapeInitiale || 0);
  let enregistrement = false;
  let dernierTitreAuto = '';
  form.noValidate = true;
  racine.classList.add('assistant-actif');
  function erreur(controle, message) {
    const id = 'erreur-' + controle.name;
    let sortie = document.getElementById(id);
    if (!sortie) {
      sortie = document.createElement('span'); sortie.id = id; sortie.className = 'champ-erreur';
      controle.closest('.champ')?.append(sortie);
    }
    sortie.textContent = message;
    controle.setAttribute('aria-invalid', message ? 'true' : 'false');
    const ids = new Set((controle.getAttribute('aria-describedby') || '').split(' ').filter(Boolean)); ids.add(id);
    controle.setAttribute('aria-describedby', [...ids].join(' '));
  }
  function valider(index) {
    let premier = null;
    for (const controle of etapes[index].querySelectorAll('input,select,textarea')) {
      if (controle.disabled || controle.type === 'radio' || controle.type === 'checkbox' || controle.type === 'search') continue;
      let message = '';
      if (controle.required && !controle.value.trim()) message = 'Renseignez ce champ pour continuer.';
      else if (!controle.validity.valid) message = controle.validity.rangeUnderflow ? 'La valeur doit être supérieure ou égale à ' + controle.min + '.' : 'Vérifiez la valeur saisie.';
      if (controle.name === 'titre' && controle.value.length > 150) message = 'L’intitulé est limité à 150 caractères.';
      if (controle.name === 'dateLimite' && controle.value && controle.value < controle.min) message = 'La date limite ne peut pas être passée.';
      if (controle.name === 'piecesRequises') {
        const pieces = [...new Set(controle.value.split(/\r?\n/).map(x => x.trim().toLocaleLowerCase('fr')).filter(Boolean))];
        if (pieces.length > 8 || pieces.some(x => x.length > 100)) message = 'Saisissez au maximum 8 pièces de 100 caractères chacune, une par ligne.';
      }
      erreur(controle, message);
      if (message && !premier) premier = controle;
    }
    if (premier) {
      afficher(index, false);
      const details = premier.closest('details'); if (details) details.open = true;
      premier.focus();
      return false;
    }
    return true;
  }
  function recapitulatif() {
    document.getElementById('recap-titre').textContent = titre.value.trim() || 'Non renseigné';
    document.getElementById('recap-effectif').textContent = champ('nbPostes').value;
    const section = champ('sectionId');
    const metier = champ('posteId');
    const mode = form.querySelector('[name=modePoste]:checked')?.value;
    if (mode) document.getElementById('recap-section').textContent = mode === 'NOUVEAU'
      ? (section?.value ? section.selectedOptions[0].textContent : 'Sans rattachement')
      : metier?.selectedOptions[0]?.dataset.section || 'Sans rattachement';
    document.getElementById('recap-missions').textContent = champ('missions').value;
    document.getElementById('recap-profil').textContent = champ('profil').value;
    const mois = Number(champ('experienceMinMois').value);
    document.getElementById('recap-experience').textContent = mois > 0 ? 'Expérience minimale : ' + mois + ' mois.' : 'Débutants acceptés.';
    const liste = document.getElementById('recap-diplomes'); liste.replaceChildren();
    const selection = [...form.querySelectorAll('[name=diplomeIds]:checked')];
    if (!selection.length) { const li = document.createElement('li'); li.textContent = 'Aucun diplôme exigé.'; liste.append(li); }
    selection.forEach(c => { const li = document.createElement('li'); li.textContent = c.closest('label').textContent.trim(); liste.append(li); });
    document.getElementById('compteur-diplomes').textContent = selection.length ? selection.length + ' diplôme(s) accepté(s).' : 'Aucun diplôme exigé.';
  }
  function afficher(index, focus = true) {
    etape = index;
    etapes.forEach((section, i) => { section.hidden = i !== index; });
    form.querySelectorAll('.assistant-navigation [data-aller]').forEach(b => {
      if (Number(b.dataset.aller) === index) b.setAttribute('aria-current', 'step'); else b.removeAttribute('aria-current');
    });
    form.querySelector('[data-retour]').hidden = index === 0;
    form.querySelector('[data-continuer]').hidden = index === 2;
    form.querySelectorAll('[data-enregistrer]').forEach(b => { b.hidden = index !== 2; });
    document.getElementById('assistant-progression').textContent = 'Étape ' + (index + 1) + ' sur 3';
    recapitulatif();
    if (focus) etapes[index].querySelector('h2').focus();
  }
  function aller(index) {
    if (index > etape) {
      for (let i = 0; i < index; i++) if (!valider(i)) return;
    }
    afficher(index);
  }
  function actualiserMode() {
    const mode = form.querySelector('[name=modePoste]:checked');
    if (!mode) return;
    const nouveau = mode.value === 'NOUVEAU';
    const catalogue = document.getElementById('poste-existant');
    const personnalise = document.getElementById('poste-personnalise');
    catalogue.hidden = nouveau; catalogue.disabled = nouveau;
    champ('posteId').required = !nouveau;
    personnalise.hidden = !nouveau; personnalise.disabled = !nouveau;
    const infos = document.getElementById('informations-poste'); infos.hidden = !nouveau;
    infos.querySelectorAll('input').forEach(c => { c.disabled = !nouveau; });
  }
  form.querySelectorAll('[data-aller]').forEach(b => b.addEventListener('click', () => aller(Number(b.dataset.aller))));
  form.querySelector('[data-retour]').addEventListener('click', () => aller(etape - 1));
  form.querySelector('[data-continuer]').addEventListener('click', () => aller(etape + 1));
  form.querySelectorAll('[name=modePoste]').forEach(c => c.addEventListener('change', actualiserMode));
  champ('posteId')?.addEventListener('change', () => {
    const option = champ('posteId').selectedOptions[0];
    if (!titre.value.trim() || titre.value === dernierTitreAuto) { titre.value = option.dataset.intitule || ''; dernierTitreAuto = titre.value; }
    if (!champ('missions').value.trim()) champ('missions').value = option.dataset.description || '';
    document.getElementById('section-existante').textContent = option.dataset.section ? 'Rattachement : ' + option.dataset.section + '. Le poste du catalogue reste inchangé.' : 'Sans section de rattachement. Le poste du catalogue reste inchangé.';
  });
  form.addEventListener('input', e => {
    if (e.target.matches('input,select,textarea') && e.target.getAttribute('aria-invalid') === 'true') erreur(e.target, '');
    recapitulatif();
  });
  const recherche = document.getElementById('filtre-diplomes');
  const normaliser = s => s.normalize('NFD').replace(/[\u0300-\u036f]/g, '').toLowerCase();
  recherche.addEventListener('input', () => {
    const mot = normaliser(recherche.value);
    form.querySelectorAll('.assistant-diplomes label').forEach(l => { l.hidden = !normaliser(l.textContent).includes(mot); });
  });
  const maintenant = new Date();
  champ('dateLimite').min = maintenant.getFullYear() + '-' + String(maintenant.getMonth() + 1).padStart(2, '0') + '-' + String(maintenant.getDate()).padStart(2, '0');
  form.addEventListener('submit', e => {
    if (enregistrement) { e.preventDefault(); return; }
    if (etape < 2) { e.preventDefault(); aller(etape + 1); return; }
    for (let i = 0; i < 3; i++) if (!valider(i)) { e.preventDefault(); return; }
    champ('actionOffre').value = e.submitter?.dataset.enregistrer || 'brouillon';
    enregistrement = true;
    form.setAttribute('aria-busy', 'true');
    form.querySelectorAll('button').forEach(b => { b.disabled = true; });
    document.getElementById('assistant-enregistrement').textContent = champ('actionOffre').value === 'publier' ? 'Publication en cours…' : 'Enregistrement en cours…';
  });
  window.addEventListener('pageshow', () => {
    enregistrement = false; form.removeAttribute('aria-busy');
    form.querySelectorAll('button').forEach(b => { b.disabled = false; });
    document.getElementById('assistant-enregistrement').textContent = '';
  });
  actualiserMode(); afficher(etape, false);
  const serveur = form.querySelector('[aria-invalid=true], .champ-erreur:not(:empty)');
  if (serveur) {
    const conteneur = serveur.closest('.assistant-etape'); if (conteneur) afficher(Number(conteneur.dataset.etape), false);
    const details = serveur.closest('details'); if (details) details.open = true;
    const controle = serveur.matches('input,textarea,select') ? serveur : serveur.closest('.champ')?.querySelector('input,select,textarea');
    (controle || racine.querySelector('.assistant-erreur-globale'))?.focus();
  }
})();
