// Seleção EXCLUSIVA + mostrar form + submit do visível (Konto hinzufügen)
document.addEventListener('DOMContentLoaded', function(){
  const optsWrap = document.getElementById('kontoAddOptions');
  if(!optsWrap) return;

  const boxes = Array.from(optsWrap.querySelectorAll('input[type="checkbox"][data-opt]'));
  const forms = {
    bank:   document.getElementById('form-bank'),
    krypto: document.getElementById('form-krypto'),
    custom: document.getElementById('form-custom'),
  };
  const submitBtn = document.getElementById('btnAddSubmit');

  function showForm(kind){
    Object.entries(forms).forEach(([k,f]) => {
      if (!f) return;
      if (k === kind){ f.style.display = 'block'; f.setAttribute('aria-hidden','false'); }
      else { f.style.display = 'none'; f.setAttribute('aria-hidden','true'); }
    });
  }
  function checkedKind(){
    const cur = boxes.find(b => b.checked);
    return cur ? cur.getAttribute('data-opt') : null;
  }
  function setExclusive(target){
    boxes.forEach(b => {
      const isTarget = (b === target);
      b.checked = isTarget;
      b.setAttribute('aria-checked', isTarget ? 'true' : 'false');
    });
    showForm(target.getAttribute('data-opt'));
  }

  // Torna exclusivo ao clicar/alterar
  boxes.forEach(b => {
    b.addEventListener('change', () => {
      if (b.checked) setExclusive(b);
      else showForm(null);
    });
    b.addEventListener('click', (e) => {
      if (!b.checked){ setExclusive(b); e.stopPropagation(); }
    });
  });

  // DEFAULT: selecionar BANK ao carregar
  const bankBox = document.querySelector('#opt-bank');
  if (bankBox){ setExclusive(bankBox); }

  // Botão: submete o form visível
  submitBtn.addEventListener('click', () => {
    const kind = checkedKind();
    if (!kind){ alert('Bitte wählen Sie eine Kontoart aus.'); return; }
    const form = forms[kind];
    if (form) form.submit();
  });
});
