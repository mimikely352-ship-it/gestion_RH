(() => {
  const bouton = document.querySelector('.connexion-voir');
  const champ = document.getElementById('connexion-mot-de-passe');
  if (!bouton || !champ) return;
  bouton.hidden = false;
  bouton.addEventListener('click', () => {
    const visible = champ.type === 'password';
    champ.type = visible ? 'text' : 'password';
    bouton.textContent = visible ? 'Masquer' : 'Afficher';
    bouton.setAttribute('aria-pressed', String(visible));
  });
})();
