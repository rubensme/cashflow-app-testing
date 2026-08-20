// Menu do usuário
(function(){
  const btn = document.getElementById('userMenuBtn');
  const dd  = document.getElementById('userDropdown');
  const wrap = document.getElementById('userMenuWrap');
  if (!btn || !dd || !wrap) return;
  function closeMenu(){ dd.style.display = 'none'; btn.setAttribute('aria-expanded','false'); }
  function toggleMenu(e){
    e && e.preventDefault();
    const open = dd.style.display === 'block';
    dd.style.display = open ? 'none' : 'block';
    btn.setAttribute('aria-expanded', open ? 'false' : 'true');
  }
  btn.addEventListener('click', function(e){ e.stopPropagation(); toggleMenu(e); });
  document.addEventListener('click', function(e){ if (!wrap.contains(e.target)) closeMenu(); });
  document.addEventListener('keydown', function(e){ if (e.key === 'Escape') closeMenu(); });
})();

// Submenus da navbar
(function(){
  document.addEventListener('click', function(e){
    const btn = e.target.closest('.nav-top');
    if (!btn) return;
    const id = btn.getAttribute('data-target');
    if (!id) return;
    const target = document.getElementById(id);
    if (!target) return;
    document.querySelectorAll('.nav-sub').forEach(el => { if (el !== target) el.style.display='none'; });
    target.style.display = (target.style.display === 'block') ? 'none' : 'block';
  });
  document.addEventListener('click', function(e){
    if (!e.target.closest('.nav-group')) {
      document.querySelectorAll('.nav-sub').forEach(el => el.style.display='none');
    }
  });
})();
