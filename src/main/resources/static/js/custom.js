// =========================
// 🌐 CONFIGURAZIONE BASE
// =========================
// Stesso origine del backend (sito servito da Spring). Non usare l'URL Render.
const BASE_URL = "";

// =========================
// 📌 MENU & PROMOZIONI
// =========================
const MENU_API = `${BASE_URL}/api/menu`;                 // Menu principale
const MENU_HIGHLIGHTS = `${BASE_URL}/api/in_evidenza`;          // Piatti in evidenza
const PROMOTIONS_API = `${BASE_URL}/api/promotions/attive`;    // Promozioni attive

// =========================
// 🎉 EVENTI
// =========================
const EVENTS_API = `${BASE_URL}/api/events`;               // Lista eventi
const EVENT_REGISTER = `${EVENTS_API}`;                        // Registrazione a evento
const EVENT_REGISTRATIONS = `${BASE_URL}/api/reservations/events`; // Tutte le registrazioni evento

// =========================
// 📅 PRENOTAZIONI
// =========================
const RES_API = `${BASE_URL}/api/reservations`;         // Endpoint base prenotazioni
const RES_USER_API = `${RES_API}/user`;                      // Prenotazioni di un utente
const RES_TIMES_API = `${RES_API}/available`;                 // Orari disponibili
const RES_LOOKUP_API = `${RES_API}`;                           // Lookup per telefono/data
const RES_NOTIFY_API = `${RES_API}/notify`;                    // Notifica contatto staff
// =========================
// 📌 RIFERIMENTI DOM MENU
// =========================
const filters = document.getElementById('categoryFilters');
const container = document.getElementById('menuItemsContainer');
const menuSearch = document.getElementById('menuSearch');
const menuCount = document.getElementById('menuCount');

let featuredIds = [];
let allItems = [];
let currentFilter = 'In Evidenza';
let searchQuery = '';

function escapeHtml(value) {
  return String(value ?? '').replace(/[&<>"']/g, char => ({
    '&': '&amp;',
    '<': '&lt;',
    '>': '&gt;',
    '"': '&quot;',
    "'": '&#39;'
  }[char]));
}

const ALLERGEN_PLACEHOLDER = 'Allergeni / Intolleranze alimentari (opzionale) - Inserisci SOLO se necessario per la sicurezza alimentare.';

function isRealAllergenText(value) {
  const text = String(value || '').trim();
  return text.length > 0 && text !== ALLERGEN_PLACEHOLDER;
}

function apiErrorText(err, fallback = 'Errore') {
  if (!err) return fallback;
  if (typeof err === 'string') {
    try {
      const parsed = JSON.parse(err);
      return parsed.error || parsed.message || fallback;
    } catch {
      return err || fallback;
    }
  }
  return err.error || err.message || fallback;
}

const DEFAULT_PHOTO = 'images/about-img.png';
const warmedPhotos = window.__cornerWarmed || (window.__cornerWarmed = new Set());

function photoUrl(url) {
  const value = String(url || '').trim();
  if (!value || value === 'null' || value === 'undefined') return DEFAULT_PHOTO;
  return value;
}

function warmupPhotos(items) {
  const featured = [];
  const rest = [];
  (items || []).forEach(item => {
    const url = photoUrl(item.imageUrl);
    if (!url || url === DEFAULT_PHOTO || warmedPhotos.has(url)) return;
    warmedPhotos.add(url);
    if (featuredIds.includes(item.id)) featured.push(url);
    else rest.push(url);
  });
  featured.concat(rest).forEach(url => {
    const img = new Image();
    img.decoding = 'async';
    img.fetchPriority = 'high';
    img.src = url;
  });
}

function startHeroVideo() {
  const video = document.getElementById('heroVideo');
  if (!video) return;
  const play = () => video.play().catch(() => {});
  video.addEventListener('canplay', play, { once: true });
  video.preload = 'auto';
  if (video.readyState >= 2) {
    play();
    return;
  }
  video.load();
  play();
}

function closeMobileNav() {
  const collapse = document.getElementById('navbarSupportedContent');
  if (!collapse || !collapse.classList.contains('show')) return;
  if (window.jQuery) {
    window.jQuery(collapse).collapse('hide');
  } else {
    collapse.classList.remove('show');
  }
}


function debugPrint(msg) {
  const box = document.getElementById('debugBox');
  if (box) box.innerHTML = msg;
}




// === SEZIONE PROMOZIONI ===
async function loadPromotions() {
  const listContainer = document.getElementById('promoTitlesList');
  const cardsContainer = document.getElementById('promoItemsContainer');
  if (!listContainer || !cardsContainer) return;

  try {
    const response = await fetch(PROMOTIONS_API);
    if (!response.ok) throw new Error(await response.text());
    const promotions = await response.json();

    if (!promotions || promotions.length === 0) {
      togglePromotionsVisibility(false);   // nascondi tutto
      return;
    }

    togglePromotionsVisibility(true);      // mostra
    listContainer.innerHTML = promotions.map((p, idx) => `
      <li class="${idx === 0 ? 'active' : ''}" data-promo-index="${idx}">${p.nome}</li>
    `).join('');

    listContainer.querySelectorAll('li').forEach(li => {
      li.addEventListener('click', () => {
        listContainer.querySelectorAll('li').forEach(el => el.classList.remove('active'));
        li.classList.add('active');
        li.scrollIntoView({ behavior: 'smooth', inline: 'center', block: 'nearest' });
        const index = parseInt(li.dataset.promoIndex, 10);
        renderPromoItems(promotions[index]);
      });
    });

    renderPromoItems(promotions[0]);
  } catch (err) {
    console.error(err);
    togglePromotionsVisibility(false);     // in errore, nascondi
  }
}

// utilità se non l'hai già messa
const fmtEUR = n => `€${Number(n).toFixed(2)}`;

function renderPromoItems(promo) {
  const cardsContainer = document.getElementById('promoItemsContainer');
  if (!cardsContainer) return;

  const prodotti = Array.isArray(promo.items) ? promo.items : [];
  if (prodotti.length === 0) {
    cardsContainer.innerHTML = '<center><div class="col-12 text-center">Nessun prodotto in promozione</div></center>';
    // rimuovi eventuale vecchio summary
    const oldSummary = document.getElementById('promoSummary');
    if (oldSummary) oldSummary.remove();
    return;
  }

  // calcolo totali
  let totaleOriginale = 0;
  let totaleScontato = 0;

  const rows = prodotti.map(item => {
    const prezzoOriginale = Number(item.prezzoOriginale || 0);
    const sconto = Number(item.scontoPercentuale || 0);
    const prezzoFinale = (item.prezzoScontato != null)
      ? Number(item.prezzoScontato)
      : prezzoOriginale * (1 - sconto / 100);
    const imageUrl = photoUrl(item.imageUrl);
    const cat = item.categoryName || '';

    totaleOriginale += prezzoOriginale;
    totaleScontato += prezzoFinale;

    return `
      <li class="promo-item">
        <div class="promo-thumb">
          <img src="${imageUrl}" alt="${item.nome}" loading="eager" fetchpriority="high" decoding="async" onerror="this.onerror=null;this.src='${DEFAULT_PHOTO}'">
        </div>
        <div class="promo-info">
          <h5>${item.nome}</h5>
          <div class="meta">${cat}</div>
        </div>
        <div class="promo-prices">
          ${prezzoOriginale ? `<span class="old">${fmtEUR(prezzoOriginale)}</span>` : ''}
          ${sconto ? `<span class="badge">-${sconto}%</span>` : ''}
          <span class="new">${fmtEUR(prezzoFinale)}</span>
        </div>
      </li>
    `;
  }).join('');

  // costruisco un'unica card
  const html = `
    <div class="col-12">
      <div class="promo-list-card">
        <div class="promo-list-header">
          <div>${promo.nome || 'Promozione'}</div>
          <div style="font-weight:900;">${prodotti.length} articoli</div>
        </div>
        <ul class="promo-items">
          ${rows}
        </ul>
        <div class="promo-totals">
          <div class="line"><span>Totale senza sconto</span><span class="without-discount">${fmtEUR(totaleOriginale)}</span></div>
          <div class="line"><span>Totale con sconto</span><span class="with-discount">${fmtEUR(totaleScontato)}</span></div>
          <div class="line"><span>Risparmio</span><span class="saving">${fmtEUR(totaleOriginale - totaleScontato)}</span></div>
        </div>
      </div>
    </div>
  `;

  // rimuovo eventuale vecchio riepilogo separato
  const oldSummary = document.getElementById('promoSummary');
  if (oldSummary) oldSummary.remove();

  // inserisco la nuova card unica
  cardsContainer.innerHTML = html;

  // piccola animazione d'entrata
  const card = cardsContainer.querySelector('.promo-list-card');
  if (card) {
    card.style.opacity = 0; card.style.transform = 'translateY(8px)';
    requestAnimationFrame(() => {
      card.style.transition = 'opacity .25s ease, transform .25s ease';
      card.style.opacity = 1; card.style.transform = 'translateY(0)';
    });
  }
}




function createPromoCard(promo, item) {
  const categoriaSlug = (item.categoryName || 'generico').replace(/\s+/g, '-');
  const imageUrl = photoUrl(item.imageUrl);
  const prezzoOriginale = Number(item.prezzoOriginale ?? 0);
  const sconto = Number(item.scontoPercentuale ?? 0);
  const prezzoFinale = Number(item.prezzoScontato ?? (prezzoOriginale * (1 - sconto / 100)));

  return `
    <div class="col-sm-6 col-lg-4 all ${categoriaSlug}">
      <div class="box promo-card">
        <div class="img-box">
          <img src="${imageUrl}" alt="${item.nome}" onerror="this.onerror=null;this.src='${DEFAULT_PHOTO}'" />
        </div>
        <div class="detail-box">
          <h5>${item.nome}</h5>
          <p>${item.categoryName || ''}</p>
          <div class="mt-2">
            <div class="d-flex align-items-center gap-2">
              <span class="text-muted text-decoration-line-through">€${prezzoOriginale.toFixed(2)}</span>
              <span class="badge bg-danger">-${sconto}%</span>
            </div>
            <div class="fw-bold text-success mt-1 fs-5">€${prezzoFinale.toFixed(2)}</div>
          </div>
        </div>
      </div>
    </div>`;
}


function initMap() {
  const coords = { lat: 41.1250, lng: 16.7819 };
  const map = new google.maps.Map(document.getElementById('mapContainer'), {
    center: coords,
    zoom: 16,
    disableDefaultUI: true, // pulito senza controlli
  });

  const marker = new google.maps.Marker({
    position: coords,
    map: map,
    title: "Corner Hamburgeria"
  });

  const infoWindow = new google.maps.InfoWindow({
    content: `<div style="font-family: 'Open Sans', sans-serif; color: #222831;">
                <h3 style="margin:0;">Corner Hamburgeria</h3>
                <p>Piazza Duomo 58</p>
                <p>70054 Giovinazzo (BA)</p>
              </div>`
  });

  marker.addListener("click", () => {
    infoWindow.open(map, marker);
  });

  // Mostra info window all'avvio
  infoWindow.open(map, marker);
}

// Chiama initMap dopo che la Google Maps API è caricata

// === POPUP EVENTI AL PRIMO ACCESSO ===
let cachedEvents = [];

function formatEventDate(iso) {
  const date = new Date(iso);
  if (Number.isNaN(date.getTime())) return '';
  return date.toLocaleString('it-IT', {
    weekday: 'short',
    day: 'numeric',
    month: 'short',
    hour: '2-digit',
    minute: '2-digit'
  });
}

function eventPosterUrl(event) {
  return photoUrl(event?.posterUrl || event?.poster_url);
}

function eventTileHTML(event) {
  const posti = event.postiDisponibili != null ? `${event.postiDisponibili} posti` : '';
  return `
    <button type="button" class="event-tile" data-event-id="${event.id}">
      <span class="event-tile-photo">
        <img src="${escapeHtml(eventPosterUrl(event))}" alt="${escapeHtml(event.titolo || 'Evento')}" loading="lazy" onerror="this.onerror=null;this.src='${DEFAULT_PHOTO}'">
      </span>
      <span class="event-tile-body">
        <span class="event-tile-date">${escapeHtml(formatEventDate(event.data))}</span>
        <span class="event-tile-title">${escapeHtml(event.titolo || 'Evento')}</span>
        ${posti ? `<span class="event-tile-seats">${escapeHtml(posti)}</span>` : ''}
        <span class="event-tile-cta">Prenota</span>
      </span>
    </button>`;
}

function bindEventTiles(root, afterClick) {
  root.querySelectorAll('.event-tile').forEach(tile => {
    tile.addEventListener('click', () => {
      const event = cachedEvents.find(item => String(item.id) === String(tile.dataset.eventId));
      if (!event) return;
      if (typeof afterClick === 'function') afterClick();
      openEventBookModal(event);
    });
  });
}

function renderEventCards(events) {
  cachedEvents = Array.isArray(events) ? events : [];
  const grid = document.getElementById('eventCardsGrid');
  const empty = document.getElementById('noEventsMessage');
  if (!grid) return;
  if (!cachedEvents.length) {
    grid.innerHTML = '';
    if (empty) {
      empty.classList.remove('d-none');
      empty.textContent = 'Non ci sono eventi in programma al momento.';
    }
    return;
  }
  if (empty) empty.classList.add('d-none');
  grid.innerHTML = cachedEvents.map(eventTileHTML).join('');
  bindEventTiles(grid);
}

function openEventBookModal(event) {
  const overlay = document.getElementById('eventBookOverlay');
  if (!overlay || !event) return;
  const select = document.getElementById('eventSelect');
  if (select) select.value = String(event.id);
  const title = document.getElementById('eventBookTitle');
  const meta = document.getElementById('eventBookMeta');
  const poster = document.getElementById('eventBookPoster');
  const seats = document.getElementById('postiDisponibili');
  if (title) title.textContent = event.titolo || 'Evento';
  if (meta) meta.textContent = [formatEventDate(event.data), event.descrizione].filter(Boolean).join(' · ');
  if (poster) {
    poster.onerror = function () {
      this.onerror = null;
      this.src = DEFAULT_PHOTO;
    };
    poster.src = eventPosterUrl(event);
    poster.alt = event.titolo || 'Evento';
  }
  if (seats) {
    seats.textContent = event.postiDisponibili != null
      ? `${event.postiDisponibili} posti disponibili`
      : '';
  }
  overlay.hidden = false;
  requestAnimationFrame(() => overlay.classList.add('is-open'));
  document.body.classList.add('no-scroll');
}

function closeEventBookModal() {
  const overlay = document.getElementById('eventBookOverlay');
  const select = document.getElementById('eventSelect');
  if (select) select.value = '';
  if (!overlay) return;
  overlay.classList.remove('is-open');
  document.body.classList.remove('no-scroll');
  setTimeout(() => { overlay.hidden = true; }, 280);
}

function openDishSheet(item) {
  const overlay = document.getElementById('dishOverlay');
  if (!overlay || !item) return;
  const img = document.getElementById('dishSheetImg');
  const hero = overlay.querySelector('.dish-sheet-hero');
  const title = document.getElementById('dishSheetTitle');
  const cat = document.getElementById('dishSheetCat');
  const price = document.getElementById('dishSheetPrice');
  const desc = document.getElementById('dishSheetDesc');
  const allergens = document.getElementById('dishSheetAllergens');
  if (title) title.textContent = item.titolo || 'Piatto';
  if (cat) cat.textContent = item.categoryName || '';
  if (price) price.textContent = `€${Number(item.prezzo || 0).toFixed(2)}`;
  if (desc) {
    desc.textContent = item.descrizione || '';
    desc.style.display = item.descrizione ? '' : 'none';
  }
  if (allergens) allergens.innerHTML = renderAllergens(item.allergens);
  if (img) {
    img.hidden = false;
    img.src = photoUrl(item.imageUrl);
    img.alt = item.titolo || '';
    img.onerror = function () {
      this.onerror = null;
      this.src = DEFAULT_PHOTO;
    };
  }
  if (hero) hero.classList.remove('no-img');
  overlay.hidden = false;
  requestAnimationFrame(() => overlay.classList.add('is-open'));
  document.body.classList.add('no-scroll');
}

function closeDishSheet() {
  const overlay = document.getElementById('dishOverlay');
  if (!overlay) return;
  overlay.classList.remove('is-open');
  document.body.classList.remove('no-scroll');
  setTimeout(() => { overlay.hidden = true; }, 280);
}

function showEventsPopup(events) {
  if (!events || events.length === 0) return;
  cachedEvents = events;

  const modalHTML = `
  <div id="eventsPopupOverlay" class="popup-overlay corner-events-overlay" role="dialog" aria-modal="true" aria-labelledby="eventsWelcomeTitle">
    <div class="popup-container event-welcome sheet-light">
      <button id="closeEventsPopupBtn" class="corner-modal-close" aria-label="Chiudi">&times;</button>
      <p class="event-welcome-kicker">Corner Pub</p>
      <h2 id="eventsWelcomeTitle">Prossimi eventi</h2>
      <p class="event-welcome-lead">Tocca una locandina per prenotare il tuo posto.</p>
      <div class="event-cards-grid events-list">
        ${events.map(eventTileHTML).join('')}
      </div>
    </div>
  </div>`;

  document.body.insertAdjacentHTML('beforeend', modalHTML);
  document.body.classList.add('no-scroll');

  const overlay = document.getElementById('eventsPopupOverlay');
  const closeEventsPopup = () => {
    if (!overlay) return;
    overlay.classList.add('hide');
    overlay.classList.remove('visible');
    setTimeout(() => overlay.remove(), 280);
    if (!document.getElementById('eventBookOverlay')?.classList.contains('is-open')) {
      document.body.classList.remove('no-scroll');
    }
    sessionStorage.setItem('eventsPopupShown', 'true');
  };

  document.getElementById('closeEventsPopupBtn')?.addEventListener('click', closeEventsPopup);
  overlay.addEventListener('click', (e) => {
    if (e.target === overlay) closeEventsPopup();
  });
  bindEventTiles(overlay, closeEventsPopup);

  requestAnimationFrame(() => overlay.classList.add('visible'));
}


async function navigateToEventRegistration(eventId) {
  try {
    if (!cachedEvents.length) {
      await loadEventsForRegistration();
    }
    const event = cachedEvents.find(item => String(item.id) === String(eventId));
    if (event) {
      openEventBookModal(event);
      return;
    }
    const tabBtn = document.querySelector('.tab-btn[data-target="eventoForm"]');
    if (tabBtn) tabBtn.click();
    document.getElementById('book')?.scrollIntoView({ behavior: 'smooth', block: 'start' });
  } catch (e) {
    console.error('navigateToEventRegistration error:', e);
  }
}

async function checkAndShowEvents() {
  // 1. Pagina Check (No alert su privacy/cookie)
  const path = window.location.pathname;
  if (path.includes('privacy.html') || path.includes('cookie.html')) return;

  // 2. GDPR Check (Se manca il consenso, aspetta. Sarà chiamato da saveConsent)
  if (localStorage.getItem('consent.booking') === null) return;

  // Controlla se il popup è già stato mostrato
  if (sessionStorage.getItem('eventsPopupShown')) return;

  try {
    const res = await fetch(EVENTS_API);
    if (!res.ok) throw new Error(res.statusText);
    const events = await res.json();

    if (events && events.length > 0) {
      showEventsPopup(events);
    } else {
      sessionStorage.setItem('eventsPopupShown', 'true');
    }
  } catch (err) {
    console.error('Errore nel caricamento eventi:', err);
  }
}

// === REGISTRAZIONE EVENTI NELLA PRENOTAZIONE ===
async function loadEventsForRegistration() {
  try {
    const res = await fetch(EVENTS_API);
    if (!res.ok) throw new Error(res.statusText);
    const events = await res.json();
    renderEventCards(events);
  } catch (err) {
    console.error('Errore nel caricamento eventi:', err);
    renderEventCards([]);
  }
}

// === INIZIO BLOCCO PER IL MENU DINAMICO ===
const MENU_CATEGORY_ORDER = [
  'panini',
  'bevande',
  'wrap',
  'sfizi',
  'dolci',
  'birre',
  'fritti',
  'starter',
  'insalat',
  'combo',
  'bombette',
  'polpette',
  'carne',
  'pinse',
  'vino',
  'toast'
];

function categoryRank(name) {
  const value = String(name || '').toLowerCase();
  const index = MENU_CATEGORY_ORDER.findIndex(key => value.includes(key));
  return index === -1 ? 999 : index;
}

function sortMenuCategories(categories) {
  return [...new Set(categories.filter(Boolean))].sort((a, b) => {
    const rankA = categoryRank(a);
    const rankB = categoryRank(b);
    if (rankA !== rankB) return rankA - rankB;
    return String(a).localeCompare(String(b), 'it');
  });
}

function renderFilters(categories) {
  if (!filters) return;

  filters.innerHTML = '';

  if (featuredIds.length > 0) {
    const li = document.createElement('li');
    li.setAttribute('data-filter', 'In Evidenza');
    li.innerText = 'In Evidenza';
    filters.appendChild(li);
  }

  categories.forEach(cat => {
    const li = document.createElement('li');
    li.setAttribute('data-filter', cat);
    li.innerText = cat;
    filters.appendChild(li);
  });

  filters.querySelectorAll('li').forEach(btn => {
    btn.addEventListener('click', () => {
      filters.querySelectorAll('li').forEach(li => li.classList.remove('active'));
      btn.classList.add('active');
      currentFilter = btn.getAttribute('data-filter');
      btn.scrollIntoView({ behavior: 'smooth', inline: 'center', block: 'nearest' });
      renderMenuItems();
    });
  });
}
function renderAllergens(allergens = []) {
  if (!Array.isArray(allergens) || allergens.length === 0) return '';
  const chips = allergens.map(a => {
    const isTrace = a.status === 'MAY_CONTAIN';
    const prefix = isTrace ? '(tracce)' : '(contiene)';
    const text = `${prefix} ${a.label.toLowerCase()}`;
    return `<li class="allergen-chip">${text}</li>`;
  }).join('');
  return `<div class="allergens"><ul class="allergen-list">${chips}</ul></div>`;
}
function renderMenuItems() {
  if (!container) return;

  container.innerHTML = '';
  let toShow;
  const query = searchQuery.trim().toLowerCase();

  if (query) {
    toShow = allItems.filter(item => {
      const haystack = `${item.titolo || ''} ${item.descrizione || ''} ${item.categoryName || ''}`.toLowerCase();
      return haystack.includes(query);
    });
  } else if (currentFilter === 'In Evidenza') {
    toShow = allItems.filter(i => featuredIds.includes(i.id));
  } else {
    toShow = allItems.filter(i => i.categoryName === currentFilter);
  }

  if (menuCount) {
    if (query) {
      menuCount.textContent = toShow.length === 1 ? '1 piatto trovato' : `${toShow.length} piatti trovati`;
    } else if (currentFilter === 'In Evidenza') {
      menuCount.textContent = toShow.length === 1 ? '1 piatto in evidenza' : `${toShow.length} piatti in evidenza`;
    } else {
      menuCount.textContent = `${toShow.length} ${currentFilter.toLowerCase()}`;
    }
  }

  if (toShow.length === 0) {
    container.innerHTML = '<p class="menu-empty text-center">Nessun piatto trovato. Prova un’altra ricerca o categoria.</p>';
    return;
  }

  toShow.forEach((item, index) => {
    const title = escapeHtml(item.titolo);
    const description = escapeHtml(item.descrizione || '');
    const imageUrl = escapeHtml(photoUrl(item.imageUrl));
    const price = Number(item.prezzo || 0).toFixed(2);
    const category = escapeHtml(item.categoryName || '');
    const hasDetails = Boolean(item.descrizione) || (Array.isArray(item.allergens) && item.allergens.length > 0);

    const card = `
  <div class="col-sm-6 col-lg-4 all menu-item-col">
    <article class="box menu-dish" data-item-id="${item.id}" tabindex="0" style="animation-delay:${Math.min(index, 8) * 40}ms">
      <div class="img-box position-relative">
        <img src="${imageUrl}" alt="${title}" loading="eager" fetchpriority="high" decoding="async" onerror="this.onerror=null;this.src='${DEFAULT_PHOTO}'" />
        ${featuredIds.includes(item.id)
        ? '<span class="badge badge-warning position-absolute" style="top:8px;right:8px;">★</span>'
        : ''}
      </div>
      <div class="detail-box">
        <div class="dish-head">
          <h5>${title}</h5>
          <h6>€${price}</h6>
        </div>
        ${query && category ? `<span class="dish-cat">${category}</span>` : ''}
        ${description ? `<p class="dish-desc">${description}</p>` : ''}
        ${renderAllergens(item.allergens)}
        ${hasDetails ? '<span class="dish-more">Apri</span>' : ''}
      </div>
    </article>
  </div>`;
    container.insertAdjacentHTML('beforeend', card);
  });

  container.querySelectorAll('.menu-dish').forEach(card => {
    const open = () => {
      const item = allItems.find(entry => String(entry.id) === String(card.dataset.itemId));
      if (item) openDishSheet(item);
    };
    card.addEventListener('click', open);
    card.addEventListener('keydown', event => {
      if (event.key === 'Enter' || event.key === ' ') {
        event.preventDefault();
        open();
      }
    });
  });

  if (window.$grid) {
    $grid.isotope('reloadItems').isotope();
  }
}


async function loadMenu() {
  try {
    const boot = window.__cornerBoot;
    const [menuData, highlights] = boot
      ? await boot
      : await Promise.all([
          fetch(MENU_API).then(r => r.json()),
          fetch(MENU_HIGHLIGHTS).then(r => r.json())
        ]);

    allItems = Array.isArray(menuData) ? menuData : [];
    featuredIds = (Array.isArray(highlights) ? highlights : []).map(h => h.itemId);
    warmupPhotos(allItems);
    startHeroVideo();

    const cats = sortMenuCategories(allItems.map(i => i.categoryName));
    renderFilters(cats);

    const defaultBtn = filters && filters.querySelector('[data-filter="In Evidenza"]');
    const firstCatBtn = filters && filters.querySelector('li');
    if (defaultBtn) {
      defaultBtn.classList.add('active');
      currentFilter = 'In Evidenza';
      renderMenuItems();
    } else if (firstCatBtn) {
      firstCatBtn.classList.add('active');
      currentFilter = firstCatBtn.getAttribute('data-filter');
      renderMenuItems();
    } else if (container) {
      container.innerHTML = '<p class="text-center">Il menu verrà pubblicato a breve.</p>';
    }
  } catch (err) {
    if (container) {
      container.innerHTML = '<p>Errore nel caricamento del menu.</p>';
    }
    console.error(err);
  }
}

// === FINE BLOCCO PER IL MENU DINAMICO ===

// 1) Footer: anno corrente
function getYear() {
  const yearElement = document.querySelector("#displayYear");
  if (yearElement) {
    yearElement.innerText = new Date().getFullYear();
  }
}

// Caricamento orari disponibili
const form = document.getElementById('reservationForm');
const dateInput = document.getElementById('resDate');
const timeSelect = document.getElementById('resTime');

const msgBox = document.getElementById('resMessage');

// === VALIDAZIONE CAMPI TELEFONO ===
(function initPhoneValidation() {
  const ALLOWED_PATTERN = /[^\d\s()+-]/g; // tutto ciò che NON è permesso
  const MIN_DIGITS = 6;                     // minimo ragionevole di cifre

  function sanitize(el) {
    if (!el) return;
    el.addEventListener('input', () => {
      const cleaned = el.value.replace(ALLOWED_PATTERN, '');
      if (cleaned !== el.value) el.value = cleaned; // blocca lettere e simboli strani
    });
    el.addEventListener('blur', () => {
      const digits = (el.value || '').replace(/\D/g, '');
      if (el.value && digits.length < MIN_DIGITS) {
        // Feedback nativo del browser
        el.setCustomValidity('Inserisci un numero valido (minimo 6 cifre; consentiti + ( ) - e spazi).');
        el.reportValidity();
      } else {
        el.setCustomValidity('');
      }
    });
  }

  // aggancia a tutti i campi telefono presenti
  ['resPhone', 'eventPhone', 'lookupPhone'].forEach(id => sanitize(document.getElementById(id)));

  // utilità riusabile nei submit
  window.__isValidPhone = function (value) {
    const digits = (value || '').replace(/\D/g, '');
    return !!value && digits.length >= MIN_DIGITS;
  };
})();

if (dateInput) {
  dateInput.addEventListener('change', async () => {
    const formattedDate = dateInput.value;
    debugPrint("Data scelta change(): " + formattedDate);
    if (!timeSelect) return;

    timeSelect.innerHTML = `<option value="" disabled selected>Caricamento…</option>`;
    if (window.$ && $.fn.niceSelect) {
      $('select').niceSelect('update');
    }

    try {
      const formattedDate = dateInput.value; // già corretto (yyyy-MM-dd)

      const [slotsRes] = await Promise.all([
        fetch(`${RES_TIMES_API}/${formattedDate}`)
      ]);

      const slots = slotsRes.ok ? await slotsRes.json() : [];

      // Popola gli orari
      timeSelect.innerHTML = `<option value="" disabled selected>Seleziona ora</option>`;
      slots.forEach(t => {
        const o = document.createElement('option');
        o.value = t;
        o.innerText = t;
        timeSelect.appendChild(o);
      });
    } catch (err) {
      timeSelect.innerHTML = `<option value="" disabled>Errore nel caricamento</option>`;
      console.error(err);
    }

    if (window.$ && $.fn.niceSelect) {
      $('select').niceSelect('update');
    }
  });
}

// Submit prenotazione
if (form) {
  form.addEventListener('submit', async e => {
    e.preventDefault();
    // PHONE VALIDATION GUARD
    const phoneField = document.getElementById('resPhone');
    if (!window.__isValidPhone(phoneField?.value)) {
      alert('Numero di telefono non valido. Usa solo cifre e i simboli + ( ) - e spazi; minimo 6 cifre.');
      phoneField && phoneField.focus();
      return;
    }
    if (!msgBox) return;

    msgBox.textContent = '';

    // Privacy Check Guard
    const privacyCheck = document.getElementById('privacyCheck');
    if (!privacyCheck || !privacyCheck.checked) {
      Swal.fire({
        title: 'Attenzione',
        text: 'Devi accettare la Privacy Policy per poter prenotare.',
        icon: 'warning',
        confirmButtonColor: '#D4AF37'
      });
      return;
    }

    // Allergen Guard
    const allergensInput = document.getElementById('resAllergens');
    const allergensConsentCheck = document.getElementById('resAllergenConsent');
    const allergensVal = allergensInput ? allergensInput.value.trim() : '';
    const hasAllergens = isRealAllergenText(allergensVal);
    let allergensConsent = false;

    if (hasAllergens) {
      if (!allergensConsentCheck || !allergensConsentCheck.checked) {
        Swal.fire({
          title: 'Consenso Necessario',
          text: 'Se inserisci allergeni/intolleranze, devi acconsentire al trattamento di questi dati sensibili.',
          icon: 'warning',
          confirmButtonColor: '#D4AF37'
        });
        return;
      }
      allergensConsent = true;
    }

    const nameVal = document.getElementById('resName').value.trim();
    const surnameVal = document.getElementById('resSurname').value.trim();
    const phoneVal = document.getElementById('resPhone').value.trim();
    const peopleVal = document.getElementById('resPeople').value.trim();
    const noteVal = document.getElementById('resNote').value.trim();

    // Validation against default values
    if (nameVal === 'Nome' || nameVal === '') {
      Swal.fire({ title: 'Errore', text: 'Inserisci il tuo nome.', icon: 'error', confirmButtonColor: '#d33' });
      return;
    }
    if (surnameVal === 'Cognome' || surnameVal === '') {
      Swal.fire({ title: 'Errore', text: 'Inserisci il tuo cognome.', icon: 'error', confirmButtonColor: '#d33' });
      return;
    }
    if (phoneVal === 'Telefono' || phoneVal === '') {
      Swal.fire({ title: 'Errore', text: 'Inserisci un numero di telefono.', icon: 'error', confirmButtonColor: '#d33' });
      return;
    }
    // Check note is not "Note"
    const finalNote = (noteVal === 'Note') ? '' : noteVal;

    const finalAllergens = hasAllergens ? allergensVal : '';

    // Check people
    if (peopleVal === 'Persone' || peopleVal === '') {
      Swal.fire({ title: 'Errore', text: 'Inserisci il numero di persone.', icon: 'error', confirmButtonColor: '#d33' });
      return;
    }

    const payload = {
      name: nameVal,
      surname: surnameVal,
      phone: phoneVal,
      date: dateInput.value,
      time: timeSelect.value,
      people: parseInt(peopleVal, 10),
      note: finalNote,
      privacyAccepted: true,
      allergensNote: finalAllergens || null,
      allergensConsent: allergensConsent
    };
    debugPrint("Data inviata nel payload submit(): " + payload.date);

    try {
      const res = await fetch(RES_API, {   // NOTA: usa RES_API, NON RES_USER_API
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(payload)
      });


      if (res.status === 201 || res.ok) {
        const dto = await res.json();

        // Formattazione data e ora per il messaggio
        const dateObj = new Date(dto.date);
        const dateStr = dateObj.toLocaleDateString('it-IT', { weekday: 'long', day: 'numeric', month: 'long', year: 'numeric' });
        // Rimuovi i secondi se presenti nell'orario
        const timeStr = (dto.time || '').substring(0, 5);

        Swal.fire({
          title: 'Prenotazione Confermata!',
          html: `
            <div style="text-align: left; font-size: 1.1rem; line-height: 1.6;">
              <p><strong>Data:</strong> ${dateStr}</p>
              <p><strong>Ora:</strong> ${timeStr}</p>
              <p><strong>Nome:</strong> ${payload.name} ${payload.surname}</p>
              <p><strong>Ospiti:</strong> ${payload.people}</p>
              ${payload.note ? `<p><strong>Note:</strong> ${payload.note}</p>` : ''}
              ${payload.allergensNote ? `<p><strong>Allergeni:</strong> ${payload.allergensNote}</p>` : ''}
              <br>
              <p style="text-align:center; font-weight:bold; color:var(--primary);">Ti aspettiamo!</p>
            </div>
          `,
          icon: 'success',
          confirmButtonText: 'Ottimo!',
          confirmButtonColor: '#D4AF37',
          background: '#fff',
          color: '#333'
        });

        form.reset();
        if (timeSelect) {
          timeSelect.innerHTML = `<option value="" disabled selected>Seleziona ora</option>`;
        }

        const eventSelect = document.getElementById('eventSelect');
        if (eventSelect) {
          eventSelect.value = '';
        }

        if (window.$ && $.fn.niceSelect) {
          $('select').niceSelect('update');
        }
      } else {
        const err = await res.json();

        // Gestione specifica errore Privacy
        if (res.status === 422 && err.code === 'PRIVACY_NOT_ACCEPTED') {
          Swal.fire({
            title: 'Privacy Richiesta',
            text: apiErrorText(err, 'Devi accettare la Privacy Policy.'),
            icon: 'warning',
            confirmButtonColor: '#D4AF37',
            footer: '<a href="privacy.html" target="_blank">Leggi Informativa</a>'
          });
        } else {
          Swal.fire({
            title: 'Errore',
            text: apiErrorText(err, res.statusText),
            icon: 'error',
            confirmButtonColor: '#d33'
          });
        }
      }
    } catch (err) {
      Swal.fire({
        title: 'Errore',
        text: 'Impossibile contattare il server.',
        icon: 'error',
        confirmButtonColor: '#d33'
      });
      console.error(err);
    }
  });
}

// Le mie Prenotazioni
const lookupForm = document.getElementById('lookupForm');
const lookupPhoneInput = document.getElementById('lookupPhone');
const reservationsList = document.getElementById('reservationsList');

if (lookupForm) {
  lookupForm.addEventListener('submit', async e => {
    e.preventDefault();
    if (!reservationsList) return;

    reservationsList.innerHTML = '';

    const phone = lookupPhoneInput.value.trim();
    if (!window.__isValidPhone(phone)) {
      reservationsList.innerHTML = `
        <li class="list-group-item text-danger">
          Inserisci un numero di telefono valido (minimo 6 cifre; consentiti + ( ) - e spazi).
        </li>`;
      return;
    }

    try {
      const res = await fetch(`${RES_USER_API}/${encodeURIComponent(phone)}`);
      if (!res.ok) throw new Error(res.statusText);
      const list = await res.json();

      if (list.length === 0) {
        reservationsList.innerHTML = `
        <center>
          <li class="list-group-item">
            Nessuna prenotazione trovata per <strong>${phone}</strong>.
          </li></center>`;
      } else {
        list.forEach(r => {
          const li = document.createElement('li');
          li.className = 'list-group-item d-flex justify-content-between align-items-center';
          li.innerHTML = `
            <div>
              <strong>${r.date} @ ${r.time}</strong><br>
              Persone: ${r.people}<br>
              Note: ${r.note || '-'}<br>
              ${r.isEventRegistration || r.eventRegistration || r.eventId ? '<span class="badge bg-info">Evento</span>' : ''}
            </div>
            <button
              class="btn btn-sm btn-danger cancel-btn"
              data-phone="${r.phone}"
              data-date="${r.date}"
              data-event="${Boolean(r.isEventRegistration || r.eventRegistration || r.eventId)}"
              data-eventid="${r.eventId || ''}"
            >Annulla</button>
          `;
          reservationsList.appendChild(li);
        });

      }
    } catch (err) {
      reservationsList.innerHTML = `
        <li class="list-group-item text-danger">
          Errore durante il recupero delle prenotazioni.
        </li>`;
      console.error(err);
    }
  });
}
if (reservationsList) {
  reservationsList.addEventListener('click', async e => {
    if (!e.target.classList.contains('cancel-btn')) return;

    const btn = e.target;
    const date = btn.dataset.date;
    const phone = btn.dataset.phone;
    const isEvent = btn.dataset.event === 'true';
    const eventId = btn.dataset.eventid;

    if (!confirm(`Vuoi veramente annullare la prenotazione del ${date}?`)) return;

    try {
      let url;
      let options = { method: 'DELETE' };

      if (isEvent && eventId) {
        url = `${EVENTS_API}/${encodeURIComponent(eventId)}/unregister/${encodeURIComponent(phone)}`;
      } else {
        url = `${RES_API}/${encodeURIComponent(phone)}/${encodeURIComponent(date)}`;
      }


      const res = await fetch(url, options);
      if (!res.ok) throw new Error(res.statusText);

      btn.closest('li').remove();
    } catch (err) {
      alert('Errore nell\'annullamento della prenotazione.');
      console.error(err);
    }
  });
}


// Gestione tab Prenotazione / Evento
const bookingTabs = document.getElementById('bookingTabs');
if (bookingTabs) {
  bookingTabs.querySelectorAll('li').forEach(tab => {
    tab.addEventListener('click', () => {
      bookingTabs.querySelectorAll('li').forEach(li => li.classList.remove('active'));
      tab.classList.add('active');

      document.getElementById('tavoloForm').classList.add('d-none');
      document.getElementById('eventoForm').classList.add('d-none');
      document.getElementById(tab.dataset.target).classList.remove('d-none');
    });
  });
}

// Submit evento
const eventForm = document.getElementById('eventForm');
if (eventForm) {
  eventForm.addEventListener('submit', async e => {
    e.preventDefault();
    // PHONE VALIDATION GUARD
    const phoneField = document.getElementById('eventPhone');
    if (!window.__isValidPhone(phoneField?.value)) {
      alert('Numero di telefono non valido. Usa solo cifre e i simboli + ( ) - e spazi; minimo 6 cifre.');
      phoneField && phoneField.focus();
      return;
    }

    // Privacy Check Guard (Event)
    const eventPrivacyCheck = document.getElementById('eventPrivacyCheck');
    if (!eventPrivacyCheck || !eventPrivacyCheck.checked) {
      Swal.fire({
        title: 'Attenzione',
        text: 'Devi accettare la Privacy Policy per poter iscriverti all\'evento.',
        icon: 'warning',
        confirmButtonColor: '#D4AF37'
      });
      return;
    }

    // Allergen Guard (Event)
    const eventAllergensInput = document.getElementById('eventAllergens');
    const eventAllergensConsentCheck = document.getElementById('eventAllergenConsent');
    const eventAllergensVal = eventAllergensInput ? eventAllergensInput.value.trim() : '';
    const hasEventAllergens = isRealAllergenText(eventAllergensVal);
    let eventAllergensConsent = false;

    if (hasEventAllergens) {
      if (!eventAllergensConsentCheck || !eventAllergensConsentCheck.checked) {
        Swal.fire({
          title: 'Consenso Necessario',
          text: 'Se inserisci allergeni/intolleranze, devi acconsentire al trattamento di questi dati sensibili.',
          icon: 'warning',
          confirmButtonColor: '#D4AF37'
        });
        return;
      }
      eventAllergensConsent = true;
    }

    const nameVal = document.getElementById('eventName').value.trim();
    const surnameVal = document.getElementById('eventSurname').value.trim();
    const phoneVal = document.getElementById('eventPhone').value.trim();
    const peopleVal = document.getElementById('eventPartecipanti').value.trim();
    const noteVal = document.getElementById('eventNote').value.trim();

    // Validation against default values
    if (nameVal === 'Nome' || nameVal === '') {
      Swal.fire({ title: 'Errore', text: 'Inserisci il tuo nome.', icon: 'error', confirmButtonColor: '#d33' });
      return;
    }
    if (surnameVal === 'Cognome' || surnameVal === '') {
      Swal.fire({ title: 'Errore', text: 'Inserisci il tuo cognome.', icon: 'error', confirmButtonColor: '#d33' });
      return;
    }
    if (phoneVal === 'Telefono' || phoneVal === '') {
      Swal.fire({ title: 'Errore', text: 'Inserisci un numero di telefono.', icon: 'error', confirmButtonColor: '#d33' });
      return;
    }
    // Check note is not "Note"
    const finalNote = (noteVal === 'Note') ? '' : noteVal;

    const finalAllergens = hasEventAllergens ? eventAllergensVal : '';

    if (peopleVal === 'Partecipanti' || peopleVal === '') {
      Swal.fire({ title: 'Errore', text: 'Inserisci il numero di partecipanti.', icon: 'error', confirmButtonColor: '#d33' });
      return;
    }

    const payload = {
      name: nameVal,
      surname: surnameVal,
      phone: phoneVal,
      partecipanti: parseInt(peopleVal, 10),
      note: finalNote,
      privacyAccepted: true, // Aggiunto per GDPR
      allergensNote: finalAllergens || null,
      allergensConsent: eventAllergensConsent
    };
    const eventId = document.getElementById('eventSelect').value;
    if (!eventId) {
      alert('Seleziona un evento prima di procedere.');
      return;
    }
    try {
      const res = await fetch(`${EVENT_REGISTER}/${eventId}/register`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(payload)
      });
      if (!res.ok) {
        const err = await res.json().catch(() => ({}));
        throw new Error(apiErrorText(err, 'Impossibile completare l\'iscrizione'));
      }
      const data = await res.json();

      Swal.fire({
        title: 'Prenotazione Confermata!',
        html: `
          <div style="text-align: left; font-size: 1.1rem; line-height: 1.6;">
            <p><strong>Evento:</strong> ${data.event.titolo}</p>
            <p><strong>Data:</strong> ${new Date(data.event.data).toLocaleDateString('it-IT', { weekday: 'long', day: 'numeric', month: 'long', hour: '2-digit', minute: '2-digit' })}</p>
            <p><strong>Nome:</strong> ${data.name} ${data.surname}</p>
            <p><strong>Partecipanti:</strong> ${data.partecipanti}</p>
            ${data.note ? `<p><strong>Note:</strong> ${data.note}</p>` : ''}
            ${data.allergensNote ? `<p><strong>Allergeni:</strong> ${data.allergensNote}</p>` : ''}
            <br>
            <p style="text-align:center; font-weight:bold; color:var(--primary);">Ti aspettiamo!</p>
          </div>
        `,
        icon: 'success',
        confirmButtonText: 'Ottimo!',
        confirmButtonColor: '#D4AF37',
        background: '#fff',
        color: '#333'
      });

      eventForm.reset();
      closeEventBookModal();
    } catch (err) {
      Swal.fire({
        title: 'Errore',
        text: err.message,
        icon: 'error',
        confirmButtonColor: '#d33'
      });
    }
  });
}


// Scroll liscio + chiudi il menu mobile
document.querySelectorAll('a[href^="#"]').forEach(a => {
  a.addEventListener('click', e => {
    const href = a.getAttribute('href');
    if (!href || href === '#') return;
    const target = document.querySelector(href);
    if (!target) return;
    e.preventDefault();
    closeMobileNav();
    target.scrollIntoView({ behavior: 'smooth', block: 'start' });
  });
});

const siteHeader = document.querySelector('.header_section');
if (siteHeader) {
  const onScroll = () => siteHeader.classList.toggle('is-scrolled', window.scrollY > 12);
  onScroll();
  window.addEventListener('scroll', onScroll, { passive: true });
}


if (menuSearch) {
  menuSearch.addEventListener('input', () => {
    searchQuery = menuSearch.value;
    renderMenuItems();
  });
}

if (window.jQuery) {
  window.jQuery('#navbarSupportedContent')
    .on('show.bs.collapse', () => document.body.classList.add('nav-open'))
    .on('hidden.bs.collapse', () => document.body.classList.remove('nav-open'));
}

(function initDockSpy() {
  const links = document.querySelectorAll('.app-dock a[data-dock]');
  if (!links.length) return;
  const map = {
    home: document.getElementById('top'),
    menu: document.getElementById('menu'),
    book: document.getElementById('book')
  };
  const setActive = (key) => {
    links.forEach(link => link.classList.toggle('is-active', link.dataset.dock === key));
  };
  const onScroll = () => {
    const y = window.scrollY + 140;
    let current = 'home';
    if (map.book && y >= map.book.offsetTop) current = 'book';
    else if (map.menu && y >= map.menu.offsetTop) current = 'menu';
    setActive(current);
  };
  window.addEventListener('scroll', onScroll, { passive: true });
  onScroll();
})();

// Mappa
function myMap() {
  const mapElement = document.getElementById("googleMap");
  if (!mapElement) return;

  const coords = { lat: 41.1250, lng: 16.7819 };
  const map = new google.maps.Map(mapElement, {
    center: coords,
    zoom: 16
  });

  const marker = new google.maps.Marker({
    position: coords,
    map: map,
    title: "Corner Hamburgeria"
  });

  const infoContent = `
    <div style="font-family: 'Open Sans', sans-serif; color: #222831;">
      <h3 style="margin:0; font-size:1.2rem;">Corner Hamburgeria</h3>
      <p style="margin:4px 0;">Piazza Duomo 58</p>
      <p style="margin:0;">70054 Giovinazzo (BA)</p>
    </div>
  `;

  const infoWindow = new google.maps.InfoWindow({ content: infoContent });
  infoWindow.open(map, marker);
  marker.addListener("click", () => infoWindow.open(map, marker));
}

// Toast notifications
function showToast(message, isError = false) {
  const toast = document.getElementById('customToast');
  if (!toast) return;

  toast.textContent = message;
  toast.style.backgroundColor = isError ? '#dc3545' : '#28a745';
  toast.classList.add('show');

  setTimeout(() => {
    toast.classList.remove('show');
  }, 3000);
}

async function registerForEvent(eventId, name, surname, phone, partecipanti = 1, note = "") {
  try {
    const res = await fetch(`${EVENT_REGISTER}/${eventId}/register`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ name, surname, phone, partecipanti, note, privacyAccepted: true })
    });

    if (!res.ok) {
      const err = await res.json().catch(() => ({}));
      alert(apiErrorText(err, 'Errore iscrizione evento'));
      return;
    }
    alert("Iscrizione evento confermata!");

  } catch (err) {
    console.error(err);
    showToast("Errore di connessione", true);
  }
}

document.querySelectorAll('.tab-btn').forEach(btn => {
  btn.addEventListener('click', async () => {
    // Attiva bottone cliccato
    document.querySelectorAll('.tab-btn').forEach(b => b.classList.remove('active'));
    btn.classList.add('active');

    // Mostra form corrispondente SOLO dentro #book
    const target = btn.dataset.target;
    document.getElementById('tavoloForm')?.classList.toggle('d-none', target !== 'tavoloForm');
    document.getElementById('eventoForm')?.classList.toggle('d-none', target !== 'eventoForm');

    if (target === "eventoForm") {
      await loadEventsForRegistration();
    }
  });
});

function showNewsletterPopup() {
  const popup = document.getElementById('newsletterPopup');
  if (popup) {
    popup.classList.add('visible');
  }
}

function closeNewsletterPopup() {
  const popup = document.getElementById('newsletterPopup');
  if (popup) {
    popup.classList.remove('visible');
    setTimeout(() => popup.remove(), 300);
  }
}

function initMotion() {
  const reveal = document.querySelectorAll(
    '.heading_container, .about_section .detail-box, .about_section .img-box, .book_section .form_container, .footer_section, #googleMap, .offer_section, .location_section'
  );
  reveal.forEach(el => el.classList.add('fx-ready'));

  const io = new IntersectionObserver((entries) => {
    entries.forEach(entry => {
      if (!entry.isIntersecting) return;
      entry.target.classList.add('fx-in');
      io.unobserve(entry.target);
    });
  }, { threshold: 0.14, rootMargin: '0px 0px -8% 0px' });

  document.querySelectorAll('.fx-ready').forEach(el => io.observe(el));
}

document.addEventListener('DOMContentLoaded', () => {
  getYear();
  initMotion();

  if (dateInput) {
    const today = new Date().toISOString().split('T')[0];
    dateInput.setAttribute('min', today);
  }

  loadPromotions();
  loadMenu();
  checkAndShowEvents();
  loadEventsForRegistration();

  // === GESTIONE CAMPI PRE-COMPILATI (Simil-Placeholder) ===
  function initDefaultValue(id, defaultValue) {
    const el = document.getElementById(id);
    if (!el) return;

    // Al caricamento, se vuoto (o reset), rimetti default
    if (!el.value) el.value = defaultValue;

    el.addEventListener('focus', () => {
      if (el.value === defaultValue) {
        el.value = '';
      }
    });

    el.addEventListener('blur', () => {
      if (!el.value.trim()) {
        el.value = defaultValue;
      }
    });
  }

  // Applica ai campi Reservation
  ['resName', 'eventName'].forEach(id => initDefaultValue(id, 'Nome'));
  ['resSurname', 'eventSurname'].forEach(id => initDefaultValue(id, 'Cognome'));
  ['resPhone', 'eventPhone'].forEach(id => initDefaultValue(id, 'Telefono'));

  // Funzione per campi che devono diventare numerici
  function initNumberField(id, defaultValue) {
    const el = document.getElementById(id);
    if (!el) return;

    // Al caricamento, se vuoto (o reset), rimetti default
    if (!el.value) el.value = defaultValue;

    el.addEventListener('focus', () => {
      if (el.value === defaultValue) {
        el.value = '';
      }
      el.type = 'number';
      el.min = '1';
    });

    el.addEventListener('blur', () => {
      if (!el.value.trim()) {
        el.type = 'text';
        el.value = defaultValue;
      }
    });
  }

  initNumberField('resPeople', 'Persone');
  initNumberField('eventPartecipanti', 'Partecipanti');

  // Note e Allergeni
  ['resNote', 'eventNote'].forEach(id => initDefaultValue(id, 'Note'));
  const allergenText = 'Allergeni / Intolleranze alimentari (opzionale) - Inserisci SOLO se necessario per la sicurezza alimentare.';
  ['resAllergens', 'eventAllergens'].forEach(id => initDefaultValue(id, allergenText));

  // Gestione speciale Data (perché cambia type)
  const rd = document.getElementById('resDate');
  if (rd) {
    if (!rd.value) rd.value = "Data";
    rd.addEventListener('focus', function () {
      if (this.value === 'Data') {
        this.value = '';
      }
      this.type = 'date';
      // Se min non è settato, mettilo a oggi
      if (!this.min) this.min = new Date().toISOString().split('T')[0];
    });
    rd.addEventListener('blur', function () {
      if (!this.value) {
        this.type = 'text';
        this.value = 'Data';
      }
    });
  }

});
$(function () {
  $("#heroCarousel").owlCarousel({
    items: 1,
    loop: true,
    autoplay: true,
    autoplayTimeout: 5000, // Più lento per effetto premium
    autoplayHoverPause: false,
    animateOut: "fadeOut",
    animateIn: "fadeIn", // Dissolvenza incrociata
    smartSpeed: 1000,    // Transizione morbida
    dots: false,
    nav: false
  });
});

function togglePromotionsVisibility(hasPromos) {
  const section = document.getElementById('promotionsSection');
  if (!section) return;

  // mostra/nascondi la sezione
  section.style.display = hasPromos ? '' : 'none';

  // ONDA SOPRA: cerca lo svg immediatamente precedente
  (function hidePrevWave() {
    let el = section.previousElementSibling;
    while (el && el.tagName && el.tagName.toLowerCase() !== 'svg') {
      el = el.previousElementSibling;
    }
    if (el && el.tagName && el.tagName.toLowerCase() === 'svg') {
      el.style.display = hasPromos ? '' : 'none';
    }
  })();

  // ONDE SOTTO: blocco subito dopo la sezione (ha due svg)
  (function hideOnlyFirstBottomWave() {
    const after = section.nextElementSibling;           // <div> con due svg
    if (!after) return;
    const firstSvg = after.querySelector('svg:first-of-type');
    if (firstSvg) firstSvg.style.display = hasPromos ? '' : 'none';
    // il secondo svg resta visibile
  })();

  // Link “Promozioni” nel menu
  const navPromoLi = document.querySelector('a.nav-link[href="#promotionsSection"]')?.closest('li');
  if (navPromoLi) navPromoLi.style.display = hasPromos ? '' : 'none';
}
// =========================
// 🔒 GESTIONE CONSENSO GDPR
// =========================
const STORAGE_KEY_BOOKING = 'consent.booking';
const STORAGE_KEY_SOCIAL = 'consent.social';
const MODAL_OVERLAY = document.getElementById('consentModalOverlay');

function initGDPR() {
  const consentBooking = localStorage.getItem(STORAGE_KEY_BOOKING);
  const consentSocial = localStorage.getItem(STORAGE_KEY_SOCIAL);

  // Se manca una scelta, mostra modale
  if (consentBooking === null || consentSocial === null) {
    showConsentModal();
  } else {
    // Applica preferenze salvate
    applyConsents(consentBooking === 'true', consentSocial === 'true');
  }

  // Pre-check form se già acconsentito
  const privacyCheck = document.getElementById('privacyCheck');
  if (privacyCheck && consentBooking === 'true') {
    privacyCheck.checked = true;
  }
  const eventPrivacyCheck = document.getElementById('eventPrivacyCheck');
  if (eventPrivacyCheck && consentBooking === 'true') {
    eventPrivacyCheck.checked = true;
  }
}

function showConsentModal() {
  if (MODAL_OVERLAY) MODAL_OVERLAY.classList.add('show');
}

function hideConsentModal() {
  if (MODAL_OVERLAY) MODAL_OVERLAY.classList.remove('show');
}

function saveConsent() {
  const bookingChecked = document.getElementById('consentBooking').checked;
  const socialChecked = document.getElementById('consentSocial').checked;

  localStorage.setItem(STORAGE_KEY_BOOKING, bookingChecked);
  localStorage.setItem(STORAGE_KEY_SOCIAL, socialChecked);

  applyConsents(bookingChecked, socialChecked);
  hideConsentModal();

  // Ora che ha deciso, possiamo mostrare eventuali popup eventi/offerte
  checkAndShowEvents();

  // Aggiorna checkbox form se necessario
  const privacyCheck = document.getElementById('privacyCheck');
  if (privacyCheck && bookingChecked) {
    privacyCheck.checked = true;
  }
  const eventPrivacyCheck = document.getElementById('eventPrivacyCheck');
  if (eventPrivacyCheck && bookingChecked) {
    eventPrivacyCheck.checked = true;
  }

  if (typeof showToast === 'function') {
    showToast("Preferenze salvate!", false);
  }
}

function applyConsents(booking, social) {
  // Social: Carica o nascondi
  if (social) {
    loadSocialEmbeds();
  }
}

function loadSocialEmbeds() {
  // TikTok
  const ttContainer = document.getElementById('tiktok-embed-container');
  const ttPlaceholder = document.getElementById('tiktok-placeholder');
  if (ttContainer && !ttContainer.innerHTML.trim()) {
    ttContainer.innerHTML = `<blockquote class="tiktok-embed" cite="https://www.tiktok.com/@cornergiovinazzo" data-unique-id="cornergiovinazzo" data-embed-type="creator" style="max-width: 780px; min-width: 288px;"> <section> <a target="_blank" href="https://www.tiktok.com/@cornergiovinazzo?refer=creator_embed"> @cornergiovinazzo </a> </section> </blockquote>`;

    // Script creator manually to ensure execution
    const script = document.createElement('script');
    script.src = "https://www.tiktok.com/embed.js";
    script.async = true;
    ttContainer.appendChild(script);

    if (ttPlaceholder) ttPlaceholder.style.display = 'none';
  }

  // Instagram
  const igContainer = document.getElementById('instagram-embed-container');
  const igPlaceholder = document.getElementById('instagram-placeholder');
  if (igContainer && !igContainer.innerHTML.trim()) {
    igContainer.innerHTML = `<iframe src="https://www.instagram.com/corner_giovinazzo/embed" width="400" height="480" frameborder="0" scrolling="no" allowtransparency="true"></iframe>`;
    if (igPlaceholder) igPlaceholder.style.display = 'none';
  }
}

window.enableSocialConsent = function () {
  localStorage.setItem(STORAGE_KEY_SOCIAL, 'true');
  // Manteniamo il booking setting attuale
  const booking = localStorage.getItem(STORAGE_KEY_BOOKING) === 'true';
  applyConsents(booking, true);

  // Aggiorna anche la checkbox nella modale se riaperta
  const chk = document.getElementById('consentSocial');
  if (chk) chk.checked = true;
};

// Bind button save
const btnSave = document.getElementById('btnSaveConsent');
if (btnSave) {
  btnSave.addEventListener('click', saveConsent);
}
const btnAcceptAll = document.getElementById('btnAcceptAll');
if (btnAcceptAll) {
  btnAcceptAll.addEventListener('click', () => {
    const booking = document.getElementById('consentBooking');
    const social = document.getElementById('consentSocial');
    if (booking) booking.checked = true;
    if (social) social.checked = true;
    saveConsent();
  });
}

const eventBookOverlay = document.getElementById('eventBookOverlay');
document.getElementById('closeEventBookBtn')?.addEventListener('click', closeEventBookModal);
eventBookOverlay?.addEventListener('click', (e) => {
  if (e.target === eventBookOverlay) closeEventBookModal();
});
const dishOverlay = document.getElementById('dishOverlay');
document.getElementById('closeDishBtn')?.addEventListener('click', closeDishSheet);
document.getElementById('dishSheetDone')?.addEventListener('click', closeDishSheet);
dishOverlay?.addEventListener('click', (e) => {
  if (e.target === dishOverlay) closeDishSheet();
});
document.addEventListener('keydown', (e) => {
  if (e.key !== 'Escape') return;
    if (eventBookOverlay?.classList.contains('is-open')) {
    closeEventBookModal();
    return;
  }
  if (document.getElementById('dishOverlay')?.classList.contains('is-open')) {
    closeDishSheet();
    return;
  }
  document.getElementById('closeEventsPopupBtn')?.click();
});

// Avvio
document.addEventListener('DOMContentLoaded', () => {
  // 1. Force Scroll to Top (User Preference)
  if ('scrollRestoration' in history) {
    history.scrollRestoration = 'manual';
  }
  window.scrollTo(0, 0);

  // 2. Init GDPR
  initGDPR();

  // 3. Bind Footer Privacy Settings Link
  const openPrivacyBtn = document.getElementById('openPrivacySettings');
  if (openPrivacyBtn) {
    openPrivacyBtn.addEventListener('click', (e) => {
      e.preventDefault();
      showConsentModal();
    });
  }
});

// =========================
// 🥗 GESTIONE ALLERGENI
// =========================
function setupAllergenLogic(inputId, consentContainerId, consentCheckboxId) {
  const input = document.getElementById(inputId);
  const container = document.getElementById(consentContainerId);
  const checkbox = document.getElementById(consentCheckboxId);

  if (!input || !container || !checkbox) return;

  input.addEventListener('input', () => {
    if (isRealAllergenText(input.value)) {
      container.classList.remove('d-none');
    } else {
      container.classList.add('d-none');
      checkbox.checked = false;
    }
  });
}

document.addEventListener('DOMContentLoaded', () => {
  // Setup per Prenotazione Tavolo
  setupAllergenLogic('resAllergens', 'resAllergenConsentContainer', 'resAllergenConsent');

  // Setup per Registrazione Evento
  setupAllergenLogic('eventAllergens', 'eventAllergenConsentContainer', 'eventAllergenConsent');
});
