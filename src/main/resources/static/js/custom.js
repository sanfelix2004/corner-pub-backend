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
    const imageUrl = item.imageUrl || 'img/default-food.jpg';
    const cat = item.categoryName || '';

    totaleOriginale += prezzoOriginale;
    totaleScontato += prezzoFinale;

    return `
      <li class="promo-item">
        <div class="promo-thumb">
          <img src="${imageUrl}" alt="${item.nome}" loading="lazy">
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
  const imageUrl = item.imageUrl || 'img/default-food.jpg';
  const prezzoOriginale = Number(item.prezzoOriginale ?? 0);
  const sconto = Number(item.scontoPercentuale ?? 0);
  const prezzoFinale = Number(item.prezzoScontato ?? (prezzoOriginale * (1 - sconto / 100)));

  return `
    <div class="col-sm-6 col-lg-4 all ${categoriaSlug}">
      <div class="box promo-card">
        <div class="img-box">
          <img src="${imageUrl}" alt="${item.nome}" />
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
function showEventsPopup(events) {
  if (!events || events.length === 0) return;

  // Inject styles for event popup images (only once)
  if (!document.getElementById('eventsPopupStyles')) {
    const styleTag = document.createElement('style');
    styleTag.id = 'eventsPopupStyles';
    styleTag.textContent = `
      /* Popup eventi: layout con riquadro info sopra e locandina grande sotto */
      body.no-scroll{ overflow:hidden; }
      .popup-container{ max-height:94vh; height:auto; }
      .popup-content{ max-height: calc(94vh - 48px); height:auto; overflow:auto; -webkit-overflow-scrolling: touch; }
      .events-list{ overflow:auto; max-height: calc(94vh - 140px); -webkit-overflow-scrolling: touch; }
      .events-list .event-item{ overflow: visible; }

      .popup-container, .popup-content {
        display: flex;
        flex-direction: column;
        align-items: center;
        justify-content: flex-start;
      }
      .events-list .event-item {
        display: flex;
        flex-direction: column;
        align-items: center;
        justify-content: flex-start;
        width: 100%;
      }

      .events-list .event-item{
        padding: 12px;
        border: 1px solid rgba(0,0,0,.06);
        border-radius: 12px;
        background: #fff;
        box-shadow: 0 2px 10px rgba(0,0,0,.03);
        margin-bottom: 16px;
      }
      .events-list .event-info{
        padding: 10px 12px;
        border: 1px solid rgba(0,0,0,.08);
        border-radius: 10px;
        background: #f9fafb;
        margin-bottom: 12px;
      }
      .events-list .event-info h3{
        margin: 0 0 6px 0;
        font-size: 1.15rem;
        line-height: 1.25;
      }
      .events-list .event-info p{
        margin: 0 0 6px 0;
        color: #555;
      }
      .events-list .event-info time{
        color: #6c757d;
        font-size: .92rem;
      }
      /* Bottone Registrati: stesso colore giallo del sito */
.events-list .btn-register-event {
  display: inline-block;
  margin-top: 8px;
  background-color: #ffb400; /* giallo Corner Pub */
  border-color: #ffb400;
  color: #fff;
  font-weight: 600;
  border-radius: 8px;
  padding: 6px 14px;
  transition: all 0.2s ease-in-out;
}
.events-list .btn-register-event:hover,
.events-list .btn-register-event:focus {
  background-color: #e3a100;
  border-color: #e3a100;
  color: #fff;
  transform: scale(1.03);
}
      /* Locandina grande sotto il riquadro, contenuta senza distorsioni. Nessuna scrollbar interna. */
      .events-list .event-poster{
        width: 100%;
        /* altezza impostata via JS per evitare barre di scorrimento */
        border-radius: 10px;
        background: #f1f3f5;
        overflow: hidden; /* mai scroll */
        display: flex;
        align-items: center;
        justify-content: center;
        max-height: 100%;
        aspect-ratio: auto;
        max-width: 100%;
        height: auto;
      }
      .events-list .event-poster img{
        width: 100%;
        height: auto;
        object-fit: cover;
        border-radius: 10px;
        max-width: 100%;
        display: block;
      }
    `;
    document.head.appendChild(styleTag);
  }

  const modalHTML = `
  <div id="eventsPopupOverlay" class="popup-overlay">
    <div class="popup-container">
      <button id="closeEventsPopupBtn" class="close-btn" aria-label="Chiudi popup">&times;</button>
      <div class="popup-content">
        <h2>Eventi in programma</h2>
        <div class="events-list">
          ${events.map(event => {
    const poster = event.posterUrl || event.poster_url || 'images/default-event.jpg';
    const dateLabel = new Date(event.data).toLocaleString('it-IT', {
      weekday: 'long',
      day: 'numeric',
      month: 'long',
      hour: '2-digit',
      minute: '2-digit'
    });
    return `
              <div class="event-item">
                <div class="event-info">
                  <h3>${event.titolo}</h3>
                  <p>${event.descrizione ?? ''}</p>
                  <time datetime="${event.data}">${dateLabel}</time>
                  <button class="btn btn-primary btn-register-event"
                          data-event-id="${event.id}"
                          data-event-date="${new Date(event.data).toISOString().split('T')[0]}">
                    Registrati
                  </button>
                </div>
                <div class="event-poster">
                  <img src="${poster}" alt="Locandina di ${event.titolo}">
                </div>
              </div>
            `;
  }).join('')}
        </div>
      </div>
    </div>
  </div>
  `;

  document.body.insertAdjacentHTML('beforeend', modalHTML);
  document.body.classList.add('no-scroll');

  document.getElementById('closeEventsPopupBtn').addEventListener('click', closeEventsPopup);

  // Gestione click "Registrati"
  document.querySelectorAll('#eventsPopupOverlay .btn-register-event').forEach(btn => {
    btn.addEventListener('click', async (e) => {
      const id = e.currentTarget.getAttribute('data-event-id');
      const dateISO = e.currentTarget.getAttribute('data-event-date');
      // Chiudi popup e porta l'utente alla registrazione evento
      closeEventsPopup();
      await navigateToEventRegistration(id, dateISO);
    });
  });

  // Calibra dinamicamente l'altezza della locandina per evitare barre di scorrimento
  function adjustEventPosters() {
    document.querySelectorAll('#eventsPopupOverlay .event-item').forEach(item => {
      const poster = item.querySelector('.event-poster img');
      const container = item.querySelector('.event-poster');
      if (poster && container) {
        poster.style.height = 'auto';
        poster.style.width = '100%';
        container.style.height = 'auto';
      }
    });
  }

  function closeEventsPopup() {
    const popup = document.getElementById('eventsPopupOverlay');
    if (popup) {
      popup.classList.add('hide');
      setTimeout(() => {
        popup.remove();
      }, 300);
      document.body.classList.remove('no-scroll');
      sessionStorage.setItem('eventsPopupShown', 'true');
    }
  }

  requestAnimationFrame(() => {
    const popup = document.getElementById('eventsPopupOverlay');
    if (popup) popup.classList.add('visible');
  });

  // setta subito l'altezza ottimale e aggiornala su resize/orientamento
  adjustEventPosters();
  window.addEventListener('resize', adjustEventPosters);
  window.addEventListener('orientationchange', adjustEventPosters);
}


/** Naviga alla scheda "Evento", imposta la data evento e pre-seleziona l'evento */
async function navigateToEventRegistration(eventId, dateISO) {
  try {
    // 1) Imposta la data nel datepicker (se disponibile) e scatena il change
    const dateInputEl = document.getElementById('resDate');
    if (dateInputEl && dateISO) {
      dateInputEl.value = dateISO;
      const ev = new Event('change');
      dateInputEl.dispatchEvent(ev);
    }

    // 2) Apri la scheda "Evento"
    const tabBtn = document.querySelector('.tab-btn[data-target="eventoForm"]');
    if (tabBtn) tabBtn.click();

    // 3) Forza il caricamento degli eventi per quella data
    await loadEventsForRegistration(dateISO);

    // 4) Seleziona l'evento nel select (attendi che compaia se necessario)
    const eventSelect = document.getElementById('eventSelect');
    if (eventSelect) {
      const trySelect = (retries = 10) => new Promise(resolve => {
        const opt = Array.from(eventSelect.options).find(o => String(o.value) === String(eventId));
        if (opt) {
          eventSelect.value = String(eventId);
          // eventuale UI plugin
          if (window.$ && $.fn.niceSelect) $('select').niceSelect('update');
          resolve(true);
        } else if (retries > 0) {
          setTimeout(() => resolve(trySelect(retries - 1)), 150);
        } else {
          resolve(false);
        }
      });
      await trySelect();
    }

    // 5) Scroll dolce alla sezione prenotazione
    const book = document.getElementById('book') || document.querySelector('#book');
    if (book) {
      book.scrollIntoView({ behavior: 'smooth', block: 'start' });
    }
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
async function loadEventsForRegistration(selectedDate = null) {
  try {
    let url = EVENTS_API;
    if (selectedDate) {
      url = `${EVENTS_API}?date=${selectedDate}`;
    }


    const res = await fetch(url);
    if (!res.ok) throw new Error(res.statusText);
    const events = await res.json();

    const select = document.getElementById('eventSelect');
    const noEventsMessage = document.getElementById('noEventsMessage');

    if (!select) return;

    // Svuota il select e nascondi il messaggio
    select.innerHTML = '<option value="">Seleziona un evento</option>';
    if (noEventsMessage) noEventsMessage.classList.add('d-none');

    if (events && events.length > 0) {
      events.forEach(event => {
        const option = document.createElement('option');
        option.value = event.id;
        option.textContent = `${event.titolo} - ${new Date(event.data).toLocaleDateString('it-IT')}`;
        select.appendChild(option);
      });

      // Mostra la sezione eventi
      const eventSection = document.getElementById('eventRegistrationSection');
      if (eventSection) {
        eventSection.classList.remove('d-none');
      }
    } else {
      // Mostra messaggio se non ci sono eventi
      if (noEventsMessage) {
        noEventsMessage.classList.remove('d-none');
        noEventsMessage.textContent = selectedDate
          ? `Non ci sono eventi programmati per il ${new Date(selectedDate).toLocaleDateString('it-IT')}`
          : 'Non ci sono eventi programmati al momento';
      }

      // Nascondi la sezione eventi
      const eventSection = document.getElementById('eventRegistrationSection');
      if (eventSection) {
        eventSection.classList.add('d-none');
      }
    }
  } catch (err) {
    console.error('Errore nel caricamento eventi:', err);
    // showToast('Errore nel caricamento degli eventi', true); // Suppressed as per user request
  }
}

// === INIZIO BLOCCO PER IL MENU DINAMICO ===
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
    const imageUrl = escapeHtml(item.imageUrl || '');
    const price = Number(item.prezzo || 0).toFixed(2);
    const category = escapeHtml(item.categoryName || '');
    const hasDetails = Boolean(item.descrizione) || (Array.isArray(item.allergens) && item.allergens.length > 0);
    const imgClass = imageUrl ? 'img-box position-relative' : 'img-box position-relative no-img';

    const card = `
  <div class="col-sm-6 col-lg-4 all menu-item-col">
    <article class="box menu-dish"${hasDetails ? ' tabindex="0"' : ''} style="animation-delay:${Math.min(index, 8) * 40}ms">
      <div class="${imgClass}">
        ${imageUrl ? `<img src="${imageUrl}" alt="${title}" loading="lazy" decoding="async" onerror="this.onerror=null;this.removeAttribute('src');this.parentElement.classList.add('no-img');" />` : ''}
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
        ${hasDetails ? '<span class="dish-more">Dettagli</span>' : ''}
      </div>
    </article>
  </div>`;
    container.insertAdjacentHTML('beforeend', card);
  });

  container.querySelectorAll('.menu-dish').forEach(card => {
    if (!card.hasAttribute('tabindex')) return;
    const toggle = () => card.classList.toggle('is-open');
    card.addEventListener('click', toggle);
    card.addEventListener('keydown', event => {
      if (event.key === 'Enter' || event.key === ' ') {
        event.preventDefault();
        toggle();
      }
    });
  });

  if (window.$grid) {
    $grid.isotope('reloadItems').isotope();
  }
}


async function loadMenu() {
  try {
    const [menuRes, evRes] = await Promise.all([
      fetch(MENU_API),
      fetch(MENU_HIGHLIGHTS)
    ]);

    const [menuData, highlights] = await Promise.all([
      menuRes.json(),
      evRes.json()
    ]);

    allItems = menuData;
    featuredIds = highlights.map(h => h.itemId);

    const cats = [...new Set(menuData.map(i => i.categoryName))];
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

      const [slotsRes, eventsRes] = await Promise.all([
        fetch(`${RES_TIMES_API}/${formattedDate}`),
        fetch(`${EVENTS_API}?date=${formattedDate}`)
      ]);

      const [slots, events] = await Promise.all([
        slotsRes.ok ? slotsRes.json() : [],
        eventsRes.ok ? eventsRes.json() : []
      ]);

      // Popola gli orari
      timeSelect.innerHTML = `<option value="" disabled selected>Seleziona ora</option>`;
      slots.forEach(t => {
        const o = document.createElement('option');
        o.value = t;
        o.innerText = t;
        timeSelect.appendChild(o);
      });

      // Gestione eventi
      const eventSelect = document.getElementById('eventSelect');
      const noEventsMessage = document.getElementById('noEventsMessage');

      if (eventSelect) {
        eventSelect.innerHTML = '<option value="">Seleziona un evento</option>';
        if (noEventsMessage) noEventsMessage.classList.add('d-none');

        if (events && events.length > 0) {
          events.forEach(event => {
            const option = document.createElement('option');
            option.value = event.id;
            option.textContent = `${event.titolo} - ${new Date(event.data).toLocaleDateString('it-IT')}`;
            eventSelect.appendChild(option);
          });

          // Mostra la sezione eventi
          const eventSection = document.getElementById('eventRegistrationSection');
          if (eventSection) {
            eventSection.classList.remove('d-none');
          }
        } else {
          // Mostra messaggio se non ci sono eventi
          if (noEventsMessage) {
            noEventsMessage.classList.remove('d-none');
            noEventsMessage.textContent = `Non ci sono eventi programmati per il ${new Date(dateInput.value).toLocaleDateString('it-IT')}`;
          }

          // Nascondi la sezione eventi
          const eventSection = document.getElementById('eventRegistrationSection');
          if (eventSection) {
            eventSection.classList.add('d-none');
          }
        }
      }
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
    let allergensConsent = false;

    if (allergensVal.length > 0) {
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

    // Allergen handling
    const finalAllergens = (allergensVal === 'Allergeni / Intolleranze alimentari (opzionale) - Inserisci SOLO se necessario per la sicurezza alimentare.') ? '' : allergensVal;

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

        // Registrazione evento se selezionato
        const eventId = document.getElementById('eventSelect')?.value;
        if (eventId) {
          await registerForEvent(
            eventId,
            payload.name,
            payload.surname,
            payload.phone,
            payload.people,
            payload.note
          );
        }

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
            text: err.message,
            icon: 'warning',
            confirmButtonColor: '#D4AF37',
            footer: '<a href="privacy.html" target="_blank">Leggi Informativa</a>'
          });
        } else {
          Swal.fire({
            title: 'Errore',
            text: err.message || res.statusText,
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
              ${r.isEventRegistration ? '<span class="badge bg-info">Evento</span>' : ''}
            </div>
            <button
              class="btn btn-sm btn-danger cancel-btn"
              data-phone="${r.phone}"
              data-date="${r.date}"
              data-event="${r.isEventRegistration}"
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

      if (isEvent) {
        if (isEvent) {
          url = `${EVENTS_API}/${encodeURIComponent(eventId)}/unregister/${encodeURIComponent(phone)}`;
        }
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
    let eventAllergensConsent = false;

    if (eventAllergensVal.length > 0) {
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

    // Allergen handling
    const finalAllergens = (eventAllergensVal === 'Allergeni / Intolleranze alimentari (opzionale) - Inserisci SOLO se necessario per la sicurezza alimentare.') ? '' : eventAllergensVal;

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
      if (!res.ok) throw new Error(await res.text());
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

const heroVideo = document.getElementById('heroVideo');
if (heroVideo) {
  const saveData = navigator.connection && navigator.connection.saveData;
  const reduceMotion = window.matchMedia('(prefers-reduced-motion: reduce)').matches;
  if (saveData || reduceMotion) {
    heroVideo.pause();
    heroVideo.removeAttribute('autoplay');
  }
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
      body: JSON.stringify({ name, surname, phone, partecipanti, note })
    });

    if (!res.ok) {
      const err = await res.json();
      alert(err.message || "Errore iscrizione evento");
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
    document.querySelectorAll('#book .form_container').forEach(f => f.classList.add('d-none'));
    document.getElementById(target).classList.remove('d-none');

    // Se è la scheda eventi → carica eventi
    if (target === "eventoForm") {
      const selectedDate = document.getElementById('resDate')?.value || null;
      await loadEventsForRegistration(selectedDate);
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

document.addEventListener('DOMContentLoaded', () => {
  getYear();

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
    if (input.value.trim().length > 0) {
      // Mostra checkbox
      container.classList.remove('d-none');
    } else {
      // Nascondi checkbox e resetta
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
