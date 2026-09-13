(function () {
  const TITLES = {
    home: 'Panoramica',
    prenotazioni: 'Prenotazioni',
    menu: 'Menu',
    evidenza: 'In evidenza',
    promozioni: 'Promozioni',
    utenti: 'Utenti',
    eventi: 'Eventi',
    categorie: 'Categorie'
  };

  const origFetch = window.fetch.bind(window);
  const cache = new Map();
  const TTL = 20000;

  window.fetch = async function (input, init = {}) {
    const url = String(input);
    const method = String(init.method || 'GET').toUpperCase();
    if (method === 'GET' && url.startsWith('/admin') && !url.includes('/attendees')) {
      const hit = cache.get(url);
      if (hit && Date.now() - hit.t < TTL) {
        return new Response(JSON.stringify(hit.d), {
          status: 200,
          headers: { 'Content-Type': 'application/json' }
        });
      }
      const res = await origFetch(input, init);
      const clone = res.clone();
      clone.json().then((data) => cache.set(url, { t: Date.now(), d: data })).catch(() => {});
      return res;
    }
    if (method !== 'GET') cache.clear();
    return origFetch(input, init);
  };

  function setBusy(on) {
    document.body.classList.toggle('bo-busy', !!on);
  }

  function markNav(section) {
    document.querySelectorAll('.bo-nav-btn, .bo-dock button').forEach((btn) => {
      btn.classList.toggle('is-active', btn.dataset.section === section);
    });
    const title = document.getElementById('boPageTitle');
    if (title) title.textContent = TITLES[section] || 'Back office';
  }

  async function loadHome() {
    const stats = document.getElementById('boStats');
    const todayBox = document.getElementById('boTodayList');
    const eventBox = document.getElementById('boEventList');
    if (!stats) return;
    setBusy(true);
    try {
      const [bookings, events, menu] = await Promise.all([
        fetch('/admin/reservations?type=table').then((r) => r.ok ? r.json() : []),
        fetch('/admin/events').then((r) => r.ok ? r.json() : []),
        fetch('/admin/menu').then((r) => r.ok ? r.json() : [])
      ]);
      const todayIso = new Date().toISOString().slice(0, 10);
      const todayBookings = (bookings || []).filter((b) => String(b.date || '').startsWith(todayIso));
      const people = todayBookings.reduce((sum, b) => sum + Number(b.people || 0), 0);
      const now = Date.now();
      const upcoming = (events || []).filter((e) => new Date(e.data).getTime() >= now);
      const visible = (menu || []).filter((m) => m.visibile !== false && m.visible !== false);

      stats.innerHTML = `
        <div class="bo-stat"><b>${todayBookings.length}</b><span>Tavoli oggi</span></div>
        <div class="bo-stat"><b>${people}</b><span>Coperti oggi</span></div>
        <div class="bo-stat"><b>${upcoming.length}</b><span>Eventi in arrivo</span></div>
        <div class="bo-stat"><b>${visible.length}</b><span>Piatti visibili</span></div>
      `;

      todayBox.innerHTML = todayBookings.length
        ? todayBookings.slice(0, 8).map((b) => `
            <button type="button" onclick="showSection('prenotazioni')">
              <strong>${b.time || ''} · ${(b.name || '')} ${b.surname || ''}</strong>
              <small>${b.people || 0} persone${b.tableNumber ? ' · tavolo ' + b.tableNumber : ''}</small>
            </button>`).join('')
        : '<p class="text-muted mb-0">Nessuna prenotazione per oggi.</p>';

      eventBox.innerHTML = (upcoming.length ? upcoming : events || []).slice(0, 6).map((e) => `
        <button type="button" onclick="showSection('eventi')">
          <strong>${e.titolo || 'Evento'}</strong>
          <small>${e.postiDisponibili != null ? e.postiDisponibili + ' posti liberi' : 'Posti illimitati'}</small>
        </button>`).join('') || '<p class="text-muted mb-0">Nessun evento.</p>';
    } catch (err) {
      stats.innerHTML = '<div class="bo-stat"><b>—</b><span>Impossibile caricare i dati</span></div>';
    } finally {
      setBusy(false);
    }
  }

  window.loadHome = loadHome;

  function closeNav() {
    document.body.classList.remove('bo-nav-open');
    const menuBtn = document.getElementById('boMenuBtn');
    if (menuBtn) {
      menuBtn.setAttribute('aria-label', 'Apri menu');
      menuBtn.innerHTML = '<i class="fas fa-bars"></i>';
    }
  }

  const origShow = window.showSection;
  window.showSection = function (section) {
    markNav(section);
    try { history.replaceState(null, '', '#' + section); } catch (e) {}
    sessionStorage.setItem('bo.section', section);
    if (section === 'home') {
      ['prenotazioni', 'menu', 'evidenza', 'promozioni', 'utenti', 'eventi', 'categorie'].forEach((s) => {
        const el = document.getElementById('section' + s.charAt(0).toUpperCase() + s.slice(1));
        if (el) el.classList.add('hidden');
      });
      const home = document.getElementById('sectionHome');
      if (home) home.classList.remove('hidden');
      loadHome();
      window.scrollTo(0, 0);
      closeNav();
      return;
    }
    const home = document.getElementById('sectionHome');
    if (home) home.classList.add('hidden');
    origShow(section);
    window.scrollTo(0, 0);
    closeNav();
  };

  function filterPage(q) {
    const query = q.trim().toLowerCase();
    document.querySelectorAll('.custom-table tbody tr, .event-registration-card').forEach((row) => {
      row.style.display = !query || row.textContent.toLowerCase().includes(query) ? '' : 'none';
    });
  }

  document.addEventListener('input', (e) => {
    if (e.target && e.target.id === 'boQuickSearch') filterPage(e.target.value);
  });

  document.addEventListener('keydown', (e) => {
    if ((e.metaKey || e.ctrlKey) && e.key.toLowerCase() === 'k') {
      e.preventDefault();
      document.getElementById('boQuickSearch')?.focus();
    }
  });

  document.addEventListener('DOMContentLoaded', () => {
    const start = (location.hash || '').replace('#', '') || sessionStorage.getItem('bo.section') || 'home';
    showSection(TITLES[start] ? start : 'home');

    const menuBtn = document.getElementById('boMenuBtn');
    const scrim = document.getElementById('boNavScrim');
    const toggleNav = () => {
      const open = !document.body.classList.contains('bo-nav-open');
      document.body.classList.toggle('bo-nav-open', open);
      if (menuBtn) {
        menuBtn.setAttribute('aria-label', open ? 'Chiudi menu' : 'Apri menu');
        menuBtn.innerHTML = open ? '<i class="fas fa-times"></i>' : '<i class="fas fa-bars"></i>';
      }
    };
    menuBtn?.addEventListener('click', toggleNav);
    scrim?.addEventListener('click', closeNav);
  });
})();
