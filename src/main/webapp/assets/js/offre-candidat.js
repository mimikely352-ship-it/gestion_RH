(() => {
  document.getElementById('erreur-candidature')?.focus();
  const form = document.querySelector('.annonce-formulaire');
  if (!form) return;
  const fichiers = [...form.querySelectorAll('input[type=file]')];
  const bouton = form.querySelector('button[type=submit]');
  let envoi = false;
  form.noValidate = true;
  function verifier(champ) {
    const fichier = champ.files[0];
    let message = '';
    if (!fichier) message = 'Sélectionnez le document demandé.';
    else if (!fichier.name.toLowerCase().endsWith('.pdf')) message = 'Sélectionnez un fichier au format PDF.';
    else if (!fichier.size || fichier.size > 5 * 1024 * 1024) message = 'Le PDF doit être non vide et ne pas dépasser 5 Mo.';
    document.getElementById('erreur-' + champ.id).textContent = message;
    champ.setAttribute('aria-invalid', message ? 'true' : 'false');
    return !message;
  }
  fichiers.forEach(champ => champ.addEventListener('change', () => {
    verifier(champ);
    document.getElementById('pieces-preparees').textContent = fichiers.filter(f => f.files.length).length + ' / ' + fichiers.length + ' pièce(s) sélectionnée(s)';
  }));
  form.addEventListener('submit', event => {
    if (envoi) { event.preventDefault(); return; }
    let premier = null;
    fichiers.forEach(champ => { if (!verifier(champ) && !premier) premier = champ; });
    if (premier) { event.preventDefault(); premier.focus(); return; }
    envoi = true; bouton.disabled = true; form.setAttribute('aria-busy', 'true');
    document.getElementById('depot-progression').textContent = 'Envoi du dossier en cours…';
  });
  window.addEventListener('pageshow', () => { envoi = false; bouton.disabled = false; form.removeAttribute('aria-busy'); });
})();
