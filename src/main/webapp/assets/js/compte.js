(() => {
  const meta = document.querySelector('meta[name="compte-connecte"]');
  if (!meta) return;
  const racine = meta.dataset.racine;
  let verification = false;
  async function verifier() {
    if (verification || document.visibilityState === 'hidden') return;
    verification = true;
    try {
      const reponse = await fetch(racine + '/compte/session', {cache: 'no-store', credentials: 'same-origin'});
      if (reponse.ok && (await reponse.json()).compte !== meta.content) {
        window.location.replace(racine + '/compte/accueil');
      }
    } catch (_) {
      // Une coupure réseau ne change pas le compte affiché.
    } finally { verification = false; }
  }
  window.addEventListener('pageshow', verifier);
  window.addEventListener('focus', verifier);
  document.addEventListener('visibilitychange', verifier);
})();
