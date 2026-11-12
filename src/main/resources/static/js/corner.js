'use strict';

/* Corner Pub — logica del sito pubblico (menù, prenotazioni, eventi, consensi) */

const API = {
  menu: '/api/menu',
  highlights: '/api/in_evidenza',
  promotions: '/api/promotions/attive',
  speciali: '/api/speciali/attivi',
  events: '/api/events',
  reservations: '/api/reservations',
  bookingSettings: '/api/booking-settings',
};

const STORAGE = {
  booking: 'consent.booking',
  social: 'consent.social',
  eventsPopup: 'eventsPopupShown',
};

const DEFAULT_PHOTO = 'images/placeholder.svg';
const FEATURED = 'In Evidenza';
// Stesso limite applicato dal backend alle prenotazioni tavolo.
const MAX_PEOPLE = 12;
const EVENT_FIRST_SLOT = 20 * 60;
const EVENT_LAST_SLOT = 23 * 60;

const MENU_CATEGORY_ORDER = [
  'panini', 'bevande', 'wrap', 'sfizi', 'dolci', 'birre', 'fritti', 'starter',
  'insalat', 'combo', 'bombette', 'polpette', 'carne', 'pinse', 'vino', 'toast',
];

const reduceMotion = window.matchMedia('(prefers-reduced-motion: reduce)');

/* ---------- Utilità ---------- */

const $ = (selector, root = document) => root.querySelector(selector);
const $$ = (selector, root = document) => [...root.querySelectorAll(selector)];

function esc(value) {
  return String(value ?? '').replace(/[&<>"']/g, char => ({
    '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;',
  }[char]));
}

const euro = new Intl.NumberFormat('it-IT', { style: 'currency', currency: 'EUR' });
const money = value => euro.format(Number(value) || 0);

function icon(name) {
  return `<svg class="icon" aria-hidden="true"><use href="#i-${name}"/></svg>`;
}

// L'accesso allo storage può lanciare (navigazione privata, cookie bloccati): mai fuori dal try.
const store = {
  get(key, area = 'localStorage') {
    try { return window[area].getItem(key); } catch { return null; }
  },
  set(key, value, area = 'localStorage') {
    try { window[area].setItem(key, String(value)); } catch { /* storage non disponibile */ }
  },
};

async function getJson(url) {
  const res = await fetch(url, { headers: { Accept: 'application/json' } });
  if (!res.ok) throw new Error(`${url}: ${res.status}`);
  return res.json();
}

async function readError(res, fallback) {
  const body = await res.json().catch(() => null);
  return {
    code: body?.code || '',
    message: body?.error || body?.message || fallback,
  };
}

function photoUrl(url) {
  const value = String(url ?? '').trim();
  return !value || value === 'null' || value === 'undefined' ? DEFAULT_PHOTO : value;
}

function cssUrl(url) {
  return encodeURI(url).replace(/['()]/g, char => `%${char.charCodeAt(0).toString(16)}`);
}

// Solo link interni o http(s): i link arrivano da contenuti inseriti a mano nel pannello admin.
function safeHref(value, fallback) {
  const href = String(value ?? '').trim();
  return /^(#|\/(?!\/)|https?:\/\/|tel:|mailto:)/i.test(href) ? href : fallback;
}

function parseIsoDate(value) {
  const match = /^(\d{4})-(\d{2})-(\d{2})/.exec(String(value ?? ''));
  return match ? new Date(Number(match[1]), Number(match[2]) - 1, Number(match[3])) : null;
}

function capitalize(text) {
  return text ? text.charAt(0).toUpperCase() + text.slice(1) : '';
}

function formatDay(value, options = { weekday: 'long', day: 'numeric', month: 'long' }) {
  const date = parseIsoDate(value);
  return date ? capitalize(date.toLocaleDateString('it-IT', options)) : String(value ?? '');
}

function formatTime(value) {
  return String(value ?? '').slice(0, 5);
}

// Data e minuti correnti a Giovinazzo, indipendentemente dal fuso del dispositivo.
function romeNow() {
  const parts = new Intl.DateTimeFormat('en-CA', {
    timeZone: 'Europe/Rome',
    year: 'numeric', month: '2-digit', day: '2-digit',
    hour: '2-digit', minute: '2-digit', hourCycle: 'h23',
  }).formatToParts(new Date());
  const part = type => parts.find(entry => entry.type === type)?.value ?? '00';
  return {
    date: `${part('year')}-${part('month')}-${part('day')}`,
    minutes: Number(part('hour')) * 60 + Number(part('minute')),
  };
}

function slotToMinutes(slot) {
  const [hours, minutes] = String(slot).split(':').map(Number);
  return hours * 60 + (minutes || 0);
}

function phoneDigits(value) {
  return String(value ?? '').replace(/\D/g, '');
}

// Forma compatta inviata al server: così "333 123 4567" e "3331234567" sono lo stesso numero.
function compactPhone(value) {
  const text = String(value ?? '').trim();
  return (text.startsWith('+') ? '+' : '') + phoneDigits(text);
}

function isValidPhone(value) {
  const digits = phoneDigits(value);
  return digits.length >= 6 && digits.length <= 15;
}

function setBusy(button, busy, busyLabel) {
  if (!button) return;
  button.disabled = busy;
  button.textContent = busy ? busyLabel : button.dataset.label;
}

/* ---------- Dialog ---------- */

function openDialog(dialog) {
  if (!dialog) return;
  if (dialog.closing) {
    dialog.closing.then(() => openDialog(dialog));
    return;
  }
  if (dialog.open) return;
  if (typeof dialog.showModal === 'function') {
    dialog.showModal();
  } else {
    dialog.classList.add('no-modal');
    dialog.setAttribute('open', '');
  }
  document.documentElement.classList.add('is-locked');
}

function closeDialog(dialog) {
  if (!dialog || !dialog.open) return Promise.resolve();
  if (dialog.closing) return dialog.closing;
  dialog.closing = new Promise(resolve => {
    dialog.classList.add('is-closing');
    setTimeout(() => {
      dialog.classList.remove('is-closing');
      if (typeof dialog.close === 'function') dialog.close();
      else dialog.removeAttribute('open');
      dialog.closing = null;
      if (!$('dialog[open]')) document.documentElement.classList.remove('is-locked');
      resolve();
    }, reduceMotion.matches ? 0 : 190);
  });
  return dialog.closing;
}

function dismissDialog(dialog) {
  if (dialog.dataset.locked === 'true') return;
  dialog.dispatchEvent(new CustomEvent('sheet:dismiss'));
  closeDialog(dialog);
}

function initDialogs() {
  $$('dialog.sheet').forEach(dialog => {
    let pressedOnBackdrop = false;
    dialog.addEventListener('cancel', event => {
      event.preventDefault();
      dismissDialog(dialog);
    });
    dialog.addEventListener('pointerdown', event => {
      pressedOnBackdrop = event.target === dialog;
    });
    dialog.addEventListener('click', event => {
      if (event.target === dialog) {
        if (pressedOnBackdrop) dismissDialog(dialog);
      } else if (event.target.closest('[data-close]')) {
        dismissDialog(dialog);
      }
    });
  });
}

let settleAlert = null;

async function notify({ title, text = '', html = '', tone = 'info', confirmLabel = 'Ok', cancelLabel = '' }) {
  const dialog = $('#alertDialog');
  // Un solo messaggio alla volta: quello precedente si chiude prima di mostrare il nuovo.
  if (settleAlert) await settleAlert(false);

  const icons = { success: 'check', error: 'alert', info: 'info' };
  const badge = $('#alertIcon');
  badge.dataset.tone = tone;
  badge.innerHTML = icon(icons[tone] || 'info');
  $('#alertTitle').textContent = title;
  $('#alertContent').innerHTML = html || esc(text);

  const confirm = $('#alertConfirm');
  const cancel = $('#alertCancel');
  confirm.textContent = confirmLabel;
  cancel.textContent = cancelLabel;
  cancel.hidden = !cancelLabel;

  return new Promise(resolve => {
    const finish = async value => {
      if (settleAlert !== finish) return;
      settleAlert = null;
      confirm.onclick = null;
      cancel.onclick = null;
      dialog.removeEventListener('sheet:dismiss', onDismiss);
      await closeDialog(dialog);
      resolve(value);
    };
    const onDismiss = () => finish(false);
    settleAlert = finish;
    confirm.onclick = () => finish(true);
    cancel.onclick = () => finish(false);
    dialog.addEventListener('sheet:dismiss', onDismiss);
    openDialog(dialog);
  });
}

function summaryHtml(rows) {
  const items = rows
    .filter(([, value]) => value !== null && value !== undefined && String(value).trim() !== '')
    .map(([label, value]) => `<div><dt>${esc(label)}</dt><dd>${esc(value)}</dd></div>`)
    .join('');
  return `<dl class="summary">${items}</dl><p>Ti aspettiamo!</p>`;
}

let toastTimer = 0;

function toast(message, tone = 'info') {
  const el = $('#toast');
  if (!el) return;
  el.textContent = message;
  el.dataset.tone = tone;
  el.classList.add('is-visible');
  clearTimeout(toastTimer);
  toastTimer = setTimeout(() => el.classList.remove('is-visible'), 3200);
}

/* ---------- Errori di campo ---------- */

function setFieldError(control, message) {
  const holder = control.closest('.field, .check');
  if (!holder) return;
  const isCheck = holder.classList.contains('check');
  let error = isCheck ? holder.nextElementSibling : $('.field-error', holder);
  if (!error || !error.classList.contains('field-error')) {
    error = document.createElement('p');
    error.className = isCheck ? 'field-error field-wide' : 'field-error';
    error.id = `${control.id}Error`;
    if (isCheck) holder.after(error);
    else holder.append(error);
  }
  error.textContent = message;
  control.setAttribute('aria-invalid', 'true');
  control.setAttribute('aria-describedby', error.id);
  holder.classList.toggle('is-invalid', isCheck);
}

function clearFieldError(control) {
  const holder = control.closest('.field, .check');
  if (!holder) return;
  const error = holder.classList.contains('check') ? holder.nextElementSibling : $('.field-error', holder);
  if (error?.classList.contains('field-error')) error.remove();
  control.removeAttribute('aria-invalid');
  control.removeAttribute('aria-describedby');
  holder.classList.remove('is-invalid');
}

function clearFormErrors(form) {
  $$('[aria-invalid="true"]', form).forEach(clearFieldError);
}

// Mostra tutti gli errori e porta il focus sul primo; true se il form è valido.
function reportErrors(form, errors) {
  clearFormErrors(form);
  errors.forEach(([control, message]) => setFieldError(control, message));
  if (errors.length) errors[0][0].focus({ preventScroll: false });
  return errors.length === 0;
}

function bindLiveValidation(form) {
  // Solo `input`: su `change` cancelleremmo errori appena impostati da altri handler
  // (es. loadSlots sulla data), perché il form riceve l'evento in bubbling dopo il target.
  form.addEventListener('input', event => {
    if (event.target.getAttribute?.('aria-invalid') === 'true') clearFieldError(event.target);
  });
}

function bindPhoneSanitizer(input) {
  input?.addEventListener('input', () => {
    const cleaned = input.value.replace(/[^\d\s()+-]/g, '');
    if (cleaned !== input.value) input.value = cleaned;
  });
}

function bindAllergenConsent(textarea, container, checkbox) {
  if (!textarea || !container || !checkbox) return;
  textarea.addEventListener('input', () => {
    const filled = textarea.value.trim().length > 0;
    container.hidden = !filled;
    if (!filled) {
      checkbox.checked = false;
      clearFieldError(checkbox);
    }
  });
}

/* ---------- Stato condiviso ---------- */

const state = {
  gate: { tableBookingsBlocked: false, eventBookingsBlocked: false },
  events: [],
  eventsLoaded: false,
  eventsPromise: null,
  specialiPromise: null,
  posterByEvent: {},
  posterBySpeciale: {},
};

/* ---------- Menù ---------- */

const menu = {
  items: [],
  featured: new Set(),
  filter: FEATURED,
  query: '',
  pendingCategory: null,
};

const menuEls = {
  filters: $('#categoryFilters'),
  grid: $('#menuItemsContainer'),
  search: $('#menuSearch'),
  count: $('#menuCount'),
};

function categoryRank(name) {
  const value = String(name ?? '').toLowerCase();
  const index = MENU_CATEGORY_ORDER.findIndex(key => value.includes(key));
  return index === -1 ? 999 : index;
}

function sortCategories(names) {
  return [...new Set(names.filter(Boolean))].sort((a, b) =>
    categoryRank(a) - categoryRank(b) || String(a).localeCompare(String(b), 'it'));
}

function renderFilters(categories) {
  const names = menu.featured.size ? [FEATURED, ...categories] : categories;
  menuEls.filters.innerHTML = names.map(name =>
    `<button type="button" class="chip" role="tab" aria-selected="false" data-filter="${esc(name)}">${esc(name)}</button>`
  ).join('');
}

function syncFilterChips() {
  const searching = menu.query.trim().length > 0;
  $$('.chip', menuEls.filters).forEach(chip => {
    chip.setAttribute('aria-selected', String(!searching && chip.dataset.filter === menu.filter));
  });
}

function selectCategory(name, { scrollChip = true } = {}) {
  menu.filter = name;
  if (menu.query) {
    menu.query = '';
    menuEls.search.value = '';
  }
  renderMenu();
  if (scrollChip) {
    $$('.chip', menuEls.filters)
      .find(chip => chip.dataset.filter === name)
      ?.scrollIntoView({ behavior: 'smooth', inline: 'center', block: 'nearest' });
  }
}

function allergensHtml(allergens) {
  if (!Array.isArray(allergens) || !allergens.length) return '';
  const chips = allergens.map(entry => {
    const trace = entry?.status === 'MAY_CONTAIN';
    const label = String(entry?.label ?? '').toLowerCase();
    return `<li class="allergen-chip${trace ? ' is-trace' : ''}">${trace ? 'Può contenere' : 'Contiene'} ${esc(label)}</li>`;
  }).join('');
  return `<div class="allergens"><p class="allergens-title">Allergeni</p><ul class="allergen-list">${chips}</ul></div>`;
}

function dishHtml(item, index, searching) {
  const allergens = Array.isArray(item.allergens) ? item.allergens.length : 0;
  const featured = menu.featured.has(item.id);
  return `
    <li>
      <button type="button" class="dish" data-item-id="${esc(item.id)}" style="--i:${Math.min(index, 10)}">
        <span class="dish-media">
          <img src="${esc(photoUrl(item.imageUrl))}" alt="" width="400" height="400"
               loading="${index < 4 ? 'eager' : 'lazy'}" decoding="async" data-fallback="">
          ${featured ? `<span class="dish-badge">${icon('star')}<span>Top</span></span>` : ''}
        </span>
        <span class="dish-body">
          <span class="dish-head">
            <span class="dish-title">${esc(item.titolo)}</span>
            <span class="price">${money(item.prezzo)}</span>
          </span>
          ${searching && item.categoryName ? `<span class="dish-cat">${esc(item.categoryName)}</span>` : ''}
          ${item.descrizione ? `<span class="dish-desc">${esc(item.descrizione)}</span>` : ''}
          ${allergens ? `<span class="dish-foot">${allergens === 1 ? '1 allergene segnalato' : `${allergens} allergeni segnalati`}</span>` : ''}
        </span>
      </button>
    </li>`;
}

function renderMenu() {
  if (!menuEls.grid) return;
  const query = menu.query.trim().toLowerCase();
  let list;
  if (query) {
    list = menu.items.filter(item =>
      `${item.titolo ?? ''} ${item.descrizione ?? ''} ${item.categoryName ?? ''}`.toLowerCase().includes(query));
  } else if (menu.filter === FEATURED) {
    list = menu.items.filter(item => menu.featured.has(item.id));
  } else {
    list = menu.items.filter(item => item.categoryName === menu.filter);
  }

  syncFilterChips();

  if (query) {
    menuEls.count.textContent = list.length === 1 ? '1 risultato' : `${list.length} risultati`;
  } else if (menu.filter === FEATURED) {
    menuEls.count.textContent = list.length === 1 ? '1 piatto in evidenza' : `${list.length} piatti in evidenza`;
  } else {
    menuEls.count.textContent = list.length === 1 ? '1 proposta' : `${list.length} proposte`;
  }

  menuEls.grid.setAttribute('aria-busy', 'false');
  menuEls.grid.innerHTML = list.length
    ? list.map((item, index) => dishHtml(item, index, Boolean(query))).join('')
    : '<li class="empty">Nessun piatto trovato. Prova un’altra ricerca o categoria.</li>';
}

function applyPendingCategory() {
  if (!menu.pendingCategory || !menu.items.length) return false;
  const wanted = String(menu.pendingCategory).trim().toLowerCase();
  const chip = $$('.chip', menuEls.filters).find(entry => entry.dataset.filter.trim().toLowerCase() === wanted);
  menu.pendingCategory = null;
  if (!chip) return false;
  selectCategory(chip.dataset.filter);
  return true;
}

function openMenuCategory(name) {
  menu.pendingCategory = name;
  applyPendingCategory();
  $('#menu')?.scrollIntoView({ behavior: 'smooth', block: 'start' });
}

function openDish(item) {
  const img = $('#dishSheetImg');
  img.dataset.fallback = '';
  img.src = photoUrl(item.imageUrl);
  img.alt = item.titolo || '';
  $('#dishSheetCat').textContent = item.categoryName || '';
  $('#dishSheetTitle').textContent = item.titolo || 'Piatto';
  $('#dishSheetPrice').textContent = money(item.prezzo);
  const desc = $('#dishSheetDesc');
  desc.textContent = item.descrizione || '';
  desc.hidden = !item.descrizione;
  $('#dishSheetAllergens').innerHTML = allergensHtml(item.allergens);
  openDialog($('#dishDialog'));
}

async function loadMenu() {
  if (!menuEls.grid) return;
  try {
    const [items, highlights] = await (window.__cornerBoot
      || Promise.all([getJson(API.menu), getJson(API.highlights)]));
    menu.items = Array.isArray(items) ? items : [];
    menu.featured = new Set((Array.isArray(highlights) ? highlights : []).map(entry => entry.itemId));

    const categories = sortCategories(menu.items.map(item => item.categoryName));
    renderFilters(categories);

    if (!menu.items.length) {
      menuEls.grid.setAttribute('aria-busy', 'false');
      menuEls.grid.innerHTML = '<li class="empty">Il menù verrà pubblicato a breve.</li>';
      return;
    }
    if (!applyPendingCategory()) {
      menu.filter = menu.featured.size ? FEATURED : categories[0];
      renderMenu();
    }
  } catch (err) {
    console.error(err);
    menuEls.grid.setAttribute('aria-busy', 'false');
    menuEls.grid.innerHTML = '<li class="empty">Non riusciamo a caricare il menù. Ricarica la pagina tra qualche istante.</li>';
  }
}

function initMenu() {
  if (!menuEls.grid) return;

  menuEls.filters.addEventListener('click', event => {
    const chip = event.target.closest('.chip');
    if (chip) selectCategory(chip.dataset.filter);
  });

  menuEls.grid.addEventListener('click', event => {
    const card = event.target.closest('.dish');
    if (!card) return;
    const item = menu.items.find(entry => String(entry.id) === card.dataset.itemId);
    if (item) openDish(item);
  });

  let searchTimer = 0;
  menuEls.search.addEventListener('input', () => {
    clearTimeout(searchTimer);
    searchTimer = setTimeout(() => {
      menu.query = menuEls.search.value;
      renderMenu();
    }, 120);
  });
}

/* ---------- Speciali ---------- */

function loadSpecialiData() {
  if (!state.specialiPromise) {
    state.specialiPromise = getJson(API.speciali)
      .then(list => (Array.isArray(list) ? list : []))
      .catch(err => {
        console.error(err);
        return [];
      })
      .then(list => {
        list.forEach(entry => {
          const img = photoUrl(entry?.immagineUrl || entry?.immagine_url);
          if (img === DEFAULT_PHOTO) return;
          if (entry?.eventoId != null) state.posterByEvent[String(entry.eventoId)] = img;
          if (entry?.id != null) state.posterBySpeciale[String(entry.id)] = img;
        });
        return list;
      });
  }
  return state.specialiPromise;
}

function specialeDates(start, end) {
  const short = value => formatDay(value, { day: 'numeric', month: 'short' }).toLowerCase();
  if (start && end) return `${short(start)} – ${short(end)}`;
  if (start) return `Dal ${short(start)}`;
  return end ? `Fino al ${short(end)}` : '';
}

function specialeHtml(item) {
  const dateLabel = item.periodoLabel || specialeDates(item.dataInizio, item.dataFine);
  const category = String(item.ctaCategoria ?? '').trim();
  const url = String(item.ctaUrl ?? '').trim();
  const canBook = Boolean(item.eventoId) && item.mostraPrenotati !== false;
  const hasCustomCta = Boolean(item.ctaTesto || category || url);
  const actions = [];

  if (canBook) {
    actions.push(`<a class="btn btn-gold" href="#book" data-speciale-book="${esc(item.eventoId)}">Prenotati ora</a>`);
  }
  if (hasCustomCta && !(canBook && !category && !url && /prenotati/i.test(item.ctaTesto || ''))) {
    const href = category ? '#menu' : safeHref(url, '#book');
    const external = /^https?:/i.test(href);
    actions.push(`<a class="btn ${canBook ? 'btn-ghost' : 'btn-gold'}" href="${esc(href)}"
      ${category ? `data-speciale-category="${esc(category)}"` : ''}
      ${external ? 'target="_blank" rel="noopener"' : ''}>${esc(item.ctaTesto || 'Scopri di più')}</a>`);
  }

  return `
    <article class="speciale-card" data-reveal>
      <div class="speciale-inner">
        <div class="speciale-media">
          <img src="${esc(photoUrl(item.immagineUrl))}" alt="${esc(item.titolo || 'Speciale')}" loading="lazy" decoding="async" data-fallback="">
        </div>
        <div class="speciale-body">
          <p class="eyebrow">${esc(item.kicker || dateLabel || 'In corso')}</p>
          <h3>${esc(item.titolo || 'Speciale')}</h3>
          ${item.descrizione ? `<p>${esc(item.descrizione)}</p>` : ''}
          ${dateLabel && item.kicker ? `<p class="speciale-dates">${icon('calendar')}${esc(dateLabel)}</p>` : ''}
          ${actions.length ? `<div class="speciale-actions">${actions.join('')}</div>` : ''}
        </div>
      </div>
    </article>`;
}

function toggleSection(sectionId, navIds, visible) {
  const section = document.getElementById(sectionId);
  if (section) section.hidden = !visible;
  navIds.forEach(id => {
    const link = document.getElementById(id);
    if (link) link.hidden = !visible;
  });
}

async function loadSpeciali() {
  const container = $('#specialiContainer');
  if (!container) return;
  const list = await loadSpecialiData();
  toggleSection('specialiSection', ['navSpeciali', 'mobileNavSpeciali'], list.length > 0);
  if (!list.length) return;
  container.innerHTML = list.map(specialeHtml).join('');
  observeReveal(container);
}

function initSpeciali() {
  $('#specialiContainer')?.addEventListener('click', event => {
    const book = event.target.closest('[data-speciale-book]');
    if (book) {
      event.preventDefault();
      goToEventBooking(book.dataset.specialeBook);
      return;
    }
    const category = event.target.closest('[data-speciale-category]');
    if (category) {
      event.preventDefault();
      openMenuCategory(category.dataset.specialeCategory);
    }
  });
}

/* ---------- Promozioni ---------- */

let promotions = [];

function promoHtml(promo) {
  const products = Array.isArray(promo.items) ? promo.items : [];
  if (!products.length) return '<p class="empty">Nessun prodotto in questa promozione.</p>';

  let fullTotal = 0;
  let discountedTotal = 0;
  let maxDiscount = 0;

  const tiles = products.map(item => {
    const full = Number(item.prezzoOriginale) || 0;
    const discount = Number(item.scontoPercentuale) || 0;
    const final = item.prezzoScontato != null ? Number(item.prezzoScontato) : full * (1 - discount / 100);
    fullTotal += full;
    discountedTotal += final;
    maxDiscount = Math.max(maxDiscount, discount);
    return `
      <article class="promo-tile">
        <div class="promo-tile-media">
          <img src="${esc(photoUrl(item.imageUrl))}" alt="" loading="lazy" decoding="async" data-fallback="">
          ${discount ? `<span class="promo-tile-badge">−${esc(discount)}%</span>` : ''}
        </div>
        <div class="promo-tile-body">
          <h4>${esc(item.nome)}</h4>
          ${item.categoryName ? `<small>${esc(item.categoryName)}</small>` : ''}
          <p class="promo-prices">
            ${full ? `<s>${money(full)}</s>` : ''}
            <span class="price">${money(final)}</span>
          </p>
        </div>
      </article>`;
  }).join('');

  const description = String(promo.descrizione ?? '').trim();
  return `
    <div class="promo-showcase">
      <header class="promo-head">
        <div>
          <p class="eyebrow">${maxDiscount ? `Fino a −${esc(maxDiscount)}%` : 'Offerta'}</p>
          <h3>${esc(promo.nome || 'Promozione')}</h3>
          ${description ? `<p>${esc(description)}</p>` : ''}
        </div>
        <div class="promo-save">
          <small>Risparmi</small>
          <strong>${money(Math.max(0, fullTotal - discountedTotal))}</strong>
          <small>${products.length === 1 ? '1 prodotto' : `${products.length} prodotti`}</small>
        </div>
      </header>
      <div class="promo-grid">${tiles}</div>
      <footer class="promo-foot">
        <span>Prima <s>${money(fullTotal)}</s></span>
        <span>Con lo sconto <strong class="price">${money(discountedTotal)}</strong></span>
      </footer>
    </div>`;
}

function showPromo(index) {
  $$('.chip', $('#promoTitlesList')).forEach((chip, position) => {
    chip.setAttribute('aria-selected', String(position === index));
  });
  $('#promoItemsContainer').innerHTML = promoHtml(promotions[index]);
}

async function loadPromotions() {
  const tabs = $('#promoTitlesList');
  if (!tabs) return;
  try {
    const list = await getJson(API.promotions);
    promotions = Array.isArray(list) ? list : [];
  } catch (err) {
    console.error(err);
    promotions = [];
  }
  toggleSection('promotionsSection', ['navPromozioni', 'mobileNavPromozioni'], promotions.length > 0);
  if (!promotions.length) return;

  tabs.hidden = promotions.length < 2;
  tabs.innerHTML = promotions.map((promo, index) =>
    `<button type="button" class="chip" role="tab" aria-selected="false" data-promo="${index}">${esc(promo.nome || 'Promozione')}</button>`
  ).join('');
  showPromo(0);
}

function initPromotions() {
  $('#promoTitlesList')?.addEventListener('click', event => {
    const chip = event.target.closest('.chip');
    if (chip) showPromo(Number(chip.dataset.promo));
  });
}

/* ---------- Eventi ---------- */

function eventDates(event) {
  const list = Array.isArray(event?.prossimeDate) ? event.prossimeDate.filter(Boolean) : [];
  if (list.length) return list;
  const raw = String(event?.data ?? '');
  return raw.length >= 10 ? [raw.slice(0, 10)] : [];
}

function eventSlots(isoDate) {
  const now = romeNow();
  if (isoDate < now.date) return [];
  const slots = [];
  for (let minutes = EVENT_FIRST_SLOT; minutes <= EVENT_LAST_SLOT; minutes += 30) {
    if (isoDate === now.date && minutes <= now.minutes) continue;
    slots.push(`${String(Math.floor(minutes / 60)).padStart(2, '0')}:${String(minutes % 60).padStart(2, '0')}`);
  }
  return slots;
}

// Prima data dell'evento per cui esiste ancora almeno un orario prenotabile.
function nextBookable(event) {
  for (const date of eventDates(event)) {
    const slots = eventSlots(date);
    if (slots.length) return { date, slots };
  }
  return null;
}

function eventWhen(event) {
  if (event?.ricorrente) {
    const date = nextBookable(event)?.date || eventDates(event)[0];
    return date ? formatDay(date) : (event.periodoLabel || '');
  }
  const date = new Date(event?.data);
  if (Number.isNaN(date.getTime())) return '';
  return capitalize(date.toLocaleString('it-IT', {
    weekday: 'long', day: 'numeric', month: 'long', hour: '2-digit', minute: '2-digit',
  }));
}

function eventPosterFallback(event) {
  return state.posterByEvent[String(event?.id)] || state.posterBySpeciale[String(event?.specialeId)] || '';
}

function eventPoster(event) {
  const own = photoUrl(event?.posterUrl || event?.poster_url);
  return own !== DEFAULT_PHOTO ? own : (eventPosterFallback(event) || DEFAULT_PHOTO);
}

function eventTileHtml(event) {
  const poster = eventPoster(event);
  const fallback = eventPosterFallback(event);
  const blocked = state.gate.eventBookingsBlocked;
  const soldOut = event.postiDisponibili != null && event.postiDisponibili <= 0;
  const meta = [];
  if (event.ricorrente) meta.push(event.periodoLabel || 'Formula ricorrente');
  if (event.postiDisponibili != null) {
    meta.push(soldOut ? 'Posti esauriti' : `${event.postiDisponibili} posti disponibili`);
  }
  return `
    <button type="button" class="event-tile${blocked || soldOut ? ' is-blocked' : ''}" data-event-id="${esc(event.id)}">
      <span class="event-tile-media" style="--poster:url('${esc(cssUrl(poster))}')">
        <img src="${esc(poster)}" alt="" loading="lazy" decoding="async" data-fallback="${esc(fallback === poster ? '' : fallback)}">
      </span>
      <span class="event-tile-body">
        <span class="event-tile-date">${esc(eventWhen(event))}</span>
        <span class="event-tile-title">${esc(event.titolo || 'Evento')}</span>
        ${meta.length ? `<span class="event-tile-meta">${esc(meta.join(' · '))}</span>` : ''}
      </span>
      <span class="event-tile-cta">${blocked ? 'Al completo' : soldOut ? 'Esaurito' : 'Prenota ora'}${icon('arrow')}</span>
    </button>`;
}

function renderEventGrids() {
  if (!state.eventsLoaded) return;
  const html = state.events.map(eventTileHtml).join('');
  const inline = $('#eventCardsGrid');
  const empty = $('#noEventsMessage');
  if (inline) inline.innerHTML = html;
  if (empty) {
    empty.hidden = state.events.length > 0;
    empty.textContent = 'Non ci sono eventi in programma al momento.';
  }
  const popup = $('#eventsDialogGrid');
  if (popup) popup.innerHTML = html;
}

function loadEvents({ force = false } = {}) {
  if (!state.eventsPromise || force) {
    state.eventsPromise = Promise.all([getJson(API.events), loadSpecialiData()])
      .then(([events]) => {
        state.events = Array.isArray(events) ? events : [];
        return state.events;
      })
      .catch(err => {
        console.error(err);
        state.events = [];
        return state.events;
      })
      .then(events => {
        state.eventsLoaded = true;
        renderEventGrids();
        return events;
      });
  }
  return state.eventsPromise;
}

const eventEls = {
  dialog: $('#eventBookDialog'),
  form: $('#eventForm'),
  id: $('#eventSelect'),
  occurrence: $('#eventOccurrence'),
  time: $('#eventTime'),
  timeWrap: $('#eventOccurrenceWrap'),
  name: $('#eventName'),
  surname: $('#eventSurname'),
  phone: $('#eventPhone'),
  people: $('#eventPartecipanti'),
  note: $('#eventNote'),
  allergens: $('#eventAllergens'),
  allergenBox: $('#eventAllergenConsentContainer'),
  allergenConsent: $('#eventAllergenConsent'),
  privacy: $('#eventPrivacyCheck'),
  notice: $('#eventBookNotice'),
  noticeText: $('#eventBookNoticeText'),
};

async function openEventBooking(event) {
  if (!event || !eventEls.dialog) return;
  if (state.gate.eventBookingsBlocked) {
    notify({ title: 'Siamo al completo', text: 'Le iscrizioni agli eventi sono momentaneamente chiuse.' });
    return;
  }
  await closeDialog($('#eventsDialog'));

  const bookable = nextBookable(event);
  const soldOut = event.postiDisponibili != null && event.postiDisponibili <= 0;
  const closedReason = soldOut
    ? 'I posti per questo evento sono esauriti.'
    : bookable ? '' : 'Le prenotazioni online per questo evento sono chiuse.';

  clearFormErrors(eventEls.form);
  eventEls.id.value = String(event.id);
  eventEls.occurrence.value = bookable?.date || '';

  const poster = $('#eventBookPoster');
  poster.dataset.fallback = eventPosterFallback(event);
  poster.src = eventPoster(event);
  poster.alt = event.titolo ? `Locandina: ${event.titolo}` : '';

  $('#eventBookTitle').textContent = event.titolo || 'Evento';
  $('#eventBookWhen').textContent = bookable ? formatDay(bookable.date) : eventWhen(event);
  $('#eventBookMeta').textContent = String(event.descrizione ?? '').trim();
  $('#postiDisponibili').textContent = event.postiDisponibili != null && !soldOut
    ? `${event.postiDisponibili} posti disponibili` : '';

  eventEls.time.innerHTML = '<option value="">Scegli l\'orario</option>'
    + (bookable?.slots ?? []).map(slot => `<option value="${slot}">${slot}</option>`).join('');
  eventEls.timeWrap.hidden = !bookable;

  if (event.postiDisponibili != null && !soldOut) eventEls.people.max = String(event.postiDisponibili);
  else eventEls.people.removeAttribute('max');

  eventEls.notice.hidden = !closedReason;
  eventEls.noticeText.textContent = closedReason;
  $$('input:not([type="hidden"]), select, textarea, button', eventEls.form)
    .forEach(control => { control.disabled = Boolean(closedReason); });

  openDialog(eventEls.dialog);
}

async function goToEventBooking(eventId) {
  const events = await loadEvents();
  let event = events.find(entry => String(entry.id) === String(eventId));
  if (!event) {
    event = await getJson(`${API.events}/${encodeURIComponent(eventId)}`).catch(() => null);
  }
  if (event) {
    openEventBooking(event);
    return;
  }
  activateTab('eventoForm');
  $('#book')?.scrollIntoView({ behavior: 'smooth', block: 'start' });
}

function validateEventForm() {
  const errors = [];
  const people = Number(eventEls.people.value);
  const seats = Number(eventEls.people.max) || Infinity;

  if (!eventEls.timeWrap.hidden && !eventEls.time.value) errors.push([eventEls.time, 'Scegli a che ora arrivi.']);
  if (!eventEls.name.value.trim()) errors.push([eventEls.name, 'Inserisci il tuo nome.']);
  if (!eventEls.surname.value.trim()) errors.push([eventEls.surname, 'Inserisci il tuo cognome.']);
  if (!isValidPhone(eventEls.phone.value)) errors.push([eventEls.phone, 'Inserisci un numero di telefono valido.']);
  if (!Number.isInteger(people) || people < 1) {
    errors.push([eventEls.people, 'Indica quante persone partecipano.']);
  } else if (people > seats) {
    errors.push([eventEls.people, seats === 1 ? 'È rimasto un solo posto.' : `Sono rimasti solo ${seats} posti.`]);
  }
  if (eventEls.allergens.value.trim() && !eventEls.allergenConsent.checked) {
    errors.push([eventEls.allergenConsent, 'Per comunicarci gli allergeni serve il tuo consenso.']);
  }
  if (!eventEls.privacy.checked) errors.push([eventEls.privacy, 'Devi accettare la Privacy Policy per prenotarti.']);
  return reportErrors(eventEls.form, errors);
}

let eventBusy = false;

async function submitEventForm(event) {
  event.preventDefault();
  if (eventBusy) return;
  if (state.gate.eventBookingsBlocked) {
    notify({ title: 'Siamo al completo', text: 'Le iscrizioni agli eventi sono momentaneamente chiuse.' });
    return;
  }
  if (!eventEls.id.value || !validateEventForm()) return;

  const allergens = eventEls.allergens.value.trim();
  const payload = {
    name: eventEls.name.value.trim(),
    surname: eventEls.surname.value.trim(),
    phone: compactPhone(eventEls.phone.value),
    partecipanti: Number(eventEls.people.value),
    note: eventEls.note.value.trim(),
    privacyAccepted: true,
    allergensNote: allergens || null,
    allergensConsent: Boolean(allergens) && eventEls.allergenConsent.checked,
  };
  if (eventEls.occurrence.value) payload.occurrenceDate = eventEls.occurrence.value;
  if (eventEls.time.value) payload.occurrenceTime = eventEls.time.value;

  const button = $('button[type="submit"]', eventEls.form);
  eventBusy = true;
  setBusy(button, true, 'Invio in corso…');
  try {
    const res = await fetch(`${API.events}/${encodeURIComponent(eventEls.id.value)}/register`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(payload),
    });
    if (!res.ok) {
      const error = await readError(res, 'Non è stato possibile completare l\'iscrizione.');
      notify({ title: 'Iscrizione non riuscita', text: error.message, tone: 'error' });
      return;
    }
    const data = await res.json().catch(() => ({}));
    const date = data.occurrenceDate || payload.occurrenceDate;
    const time = formatTime(data.occurrenceTime || payload.occurrenceTime);

    eventEls.form.reset();
    eventEls.allergenBox.hidden = true;
    syncPrivacyChecks();
    await closeDialog(eventEls.dialog);
    notify({
      title: 'Prenotazione confermata!',
      tone: 'success',
      confirmLabel: 'Ottimo',
      html: summaryHtml([
        ['Evento', data.event?.titolo || $('#eventBookTitle').textContent],
        ['Quando', [date ? formatDay(date) : '', time].filter(Boolean).join(' · ')],
        ['Nome', `${data.name || payload.name} ${data.surname || payload.surname}`],
        ['Persone', data.partecipanti ?? payload.partecipanti],
        ['Note', data.note ?? payload.note],
        ['Allergeni', data.allergensNote ?? payload.allergensNote],
      ]),
    });
    loadEvents({ force: true });
  } catch (err) {
    console.error(err);
    notify({ title: 'Connessione assente', text: 'Non riusciamo a contattare il server. Riprova tra poco.', tone: 'error' });
  } finally {
    eventBusy = false;
    setBusy(button, false);
  }
}

async function maybeShowEventsPopup() {
  // Chi arriva con un link diretto a una sezione non viene interrotto dal popup.
  const deepLink = hashTarget().id;
  if (deepLink && deepLink !== 'top') return;
  if (store.get(STORAGE.booking) === null) return;
  if (store.get(STORAGE.eventsPopup, 'sessionStorage')) return;
  const events = await loadEvents();
  if ($('dialog[open]')) return;
  store.set(STORAGE.eventsPopup, 'true', 'sessionStorage');
  if (events.length) openDialog($('#eventsDialog'));
}

function initEvents() {
  const onTileClick = event => {
    const tile = event.target.closest('.event-tile');
    if (!tile) return;
    const selected = state.events.find(entry => String(entry.id) === tile.dataset.eventId);
    if (selected) openEventBooking(selected);
  };
  $('#eventCardsGrid')?.addEventListener('click', onTileClick);
  $('#eventsDialogGrid')?.addEventListener('click', onTileClick);

  if (!eventEls.form) return;
  bindLiveValidation(eventEls.form);
  bindPhoneSanitizer(eventEls.phone);
  bindAllergenConsent(eventEls.allergens, eventEls.allergenBox, eventEls.allergenConsent);
  eventEls.form.addEventListener('submit', submitEventForm);
}

/* ---------- Prenotazione tavolo ---------- */

const resEls = {
  form: $('#reservationForm'),
  name: $('#resName'),
  surname: $('#resSurname'),
  phone: $('#resPhone'),
  people: $('#resPeople'),
  date: $('#resDate'),
  time: $('#resTime'),
  note: $('#resNote'),
  allergens: $('#resAllergens'),
  allergenBox: $('#resAllergenConsentContainer'),
  allergenConsent: $('#resAllergenConsent'),
  privacy: $('#privacyCheck'),
};

let slotsRequest = 0;

function setTimeOptions(placeholder, slots = []) {
  resEls.time.innerHTML = `<option value="">${esc(placeholder)}</option>`
    + slots.map(slot => `<option value="${esc(slot)}">${esc(formatTime(slot))}</option>`).join('');
  resEls.time.disabled = slots.length === 0 || state.gate.tableBookingsBlocked;
}

async function loadSlots() {
  const date = resEls.date.value;
  const request = ++slotsRequest;
  clearFieldError(resEls.time);
  clearFieldError(resEls.date);

  if (!date) {
    setTimeOptions('Scegli prima la data');
    return;
  }
  const now = romeNow();
  if (date < now.date) {
    setTimeOptions('Scegli prima la data');
    setFieldError(resEls.date, 'Scegli una data da oggi in poi.');
    return;
  }

  setTimeOptions('Carico gli orari…');
  try {
    const all = await getJson(`${API.reservations}/available/${encodeURIComponent(date)}`);
    if (request !== slotsRequest) return;
    const slots = (Array.isArray(all) ? all : [])
      .filter(slot => date !== now.date || slotToMinutes(slot) > now.minutes);
    if (!slots.length) {
      setTimeOptions('Nessun orario disponibile');
      setFieldError(resEls.time, 'Per questa data non ci sono orari liberi: prova un altro giorno.');
      return;
    }
    setTimeOptions('Scegli l\'orario', slots);
  } catch (err) {
    if (request !== slotsRequest) return;
    console.error(err);
    setTimeOptions('Orari non disponibili');
    setFieldError(resEls.time, 'Non riusciamo a caricare gli orari. Riprova tra poco.');
  }
}

function validateReservation() {
  const errors = [];
  const people = Number(resEls.people.value);

  if (!resEls.name.value.trim()) errors.push([resEls.name, 'Inserisci il tuo nome.']);
  if (!resEls.surname.value.trim()) errors.push([resEls.surname, 'Inserisci il tuo cognome.']);
  if (!isValidPhone(resEls.phone.value)) errors.push([resEls.phone, 'Inserisci un numero di telefono valido.']);
  if (!Number.isInteger(people) || people < 1) {
    errors.push([resEls.people, 'Indica per quante persone.']);
  } else if (people > MAX_PEOPLE) {
    errors.push([resEls.people, `Online prenotiamo fino a ${MAX_PEOPLE} persone: per gruppi più numerosi chiamaci.`]);
  }
  if (!resEls.date.value) errors.push([resEls.date, 'Scegli una data.']);
  else if (resEls.date.value < romeNow().date) errors.push([resEls.date, 'Scegli una data da oggi in poi.']);
  if (resEls.date.value && !resEls.time.value && !errors.some(([control]) => control === resEls.date)) {
    if (resEls.time.options.length > 1) errors.push([resEls.time, 'Scegli un orario.']);
    else errors.push([resEls.date, 'Per questa data non ci sono orari liberi: prova un altro giorno.']);
  }
  if (resEls.allergens.value.trim() && !resEls.allergenConsent.checked) {
    errors.push([resEls.allergenConsent, 'Per comunicarci gli allergeni serve il tuo consenso.']);
  }
  if (!resEls.privacy.checked) errors.push([resEls.privacy, 'Devi accettare la Privacy Policy per prenotare.']);
  return reportErrors(resEls.form, errors);
}

let reservationBusy = false;

async function submitReservation(event) {
  event.preventDefault();
  if (reservationBusy) return;
  if (state.gate.tableBookingsBlocked) {
    notify({ title: 'Siamo al completo', text: 'Le prenotazioni tavolo sono momentaneamente chiuse.' });
    return;
  }
  if (!validateReservation()) return;

  const allergens = resEls.allergens.value.trim();
  const payload = {
    name: resEls.name.value.trim(),
    surname: resEls.surname.value.trim(),
    phone: compactPhone(resEls.phone.value),
    date: resEls.date.value,
    time: resEls.time.value,
    people: Number(resEls.people.value),
    note: resEls.note.value.trim(),
    privacyAccepted: true,
    allergensNote: allergens || null,
    allergensConsent: Boolean(allergens) && resEls.allergenConsent.checked,
  };

  const button = $('button[type="submit"]', resEls.form);
  reservationBusy = true;
  setBusy(button, true, 'Invio in corso…');
  try {
    const res = await fetch(API.reservations, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(payload),
    });
    if (!res.ok) {
      const error = await readError(res, 'Non è stato possibile registrare la prenotazione.');
      if (res.status === 422 && error.code === 'PRIVACY_NOT_ACCEPTED') {
        setFieldError(resEls.privacy, 'Devi accettare la Privacy Policy per prenotare.');
        resEls.privacy.focus();
      } else {
        notify({ title: 'Prenotazione non riuscita', text: error.message, tone: 'error' });
      }
      return;
    }
    const dto = await res.json().catch(() => ({}));

    resEls.form.reset();
    resEls.allergenBox.hidden = true;
    setTimeOptions('Scegli prima la data');
    syncPrivacyChecks();
    notify({
      title: 'Prenotazione confermata!',
      tone: 'success',
      confirmLabel: 'Ottimo',
      html: summaryHtml([
        ['Giorno', formatDay(dto.date || payload.date, { weekday: 'long', day: 'numeric', month: 'long', year: 'numeric' })],
        ['Ora', formatTime(dto.time || payload.time)],
        ['Nome', `${payload.name} ${payload.surname}`],
        ['Persone', payload.people],
        ['Note', payload.note],
        ['Allergeni', payload.allergensNote],
      ]),
    });
  } catch (err) {
    console.error(err);
    notify({ title: 'Connessione assente', text: 'Non riusciamo a contattare il server. Riprova tra poco.', tone: 'error' });
  } finally {
    reservationBusy = false;
    setBusy(button, false);
    applyGate();
  }
}

function initReservation() {
  if (!resEls.form) return;
  resEls.date.min = romeNow().date;
  bindLiveValidation(resEls.form);
  bindPhoneSanitizer(resEls.phone);
  bindAllergenConsent(resEls.allergens, resEls.allergenBox, resEls.allergenConsent);
  resEls.date.addEventListener('change', loadSlots);
  resEls.form.addEventListener('submit', submitReservation);
}

/* ---------- Le mie prenotazioni ---------- */

const lookupEls = {
  form: $('#lookupForm'),
  phone: $('#lookupPhone'),
  list: $('#reservationsList'),
};

function isEventBooking(entry) {
  return Boolean(entry.isEventRegistration || entry.eventRegistration || entry.eventId);
}

function reservationHtml(entry, phone) {
  const date = parseIsoDate(entry.date);
  const isEvent = isEventBooking(entry);
  const people = Number(entry.people) || 0;
  const details = [
    isEvent ? `Evento${entry.event?.titolo ? `: ${entry.event.titolo}` : ''}` : 'Tavolo',
    people ? (people === 1 ? '1 persona' : `${people} persone`) : '',
  ].filter(Boolean).join(' · ');
  const time = formatTime(entry.time);
  return `
    <li class="res-item">
      <div class="res-date" aria-hidden="true">
        <b>${date ? date.getDate() : '–'}</b>
        <small>${date ? esc(date.toLocaleDateString('it-IT', { month: 'short' }).replace('.', '')) : ''}</small>
      </div>
      <div class="res-info">
        <strong>${esc(formatDay(entry.date))}${time ? ` · ${esc(time)}` : ''}</strong>
        <span>${esc(details)}</span>
        ${entry.note ? `<span>Note: ${esc(entry.note)}</span>` : ''}
      </div>
      <button type="button" class="btn btn-ghost btn-sm" data-cancel
        data-phone="${esc(entry.phone || phone)}" data-date="${esc(entry.date)}"
        data-event-id="${isEvent ? esc(entry.eventId ?? '') : ''}">Annulla</button>
    </li>`;
}

function lookupMessage(text, isError = false) {
  lookupEls.list.innerHTML = `<li class="res-message${isError ? ' is-error' : ''}">${esc(text)}</li>`;
}

async function submitLookup(event) {
  event.preventDefault();
  const phone = lookupEls.phone.value.trim();
  if (!reportErrors(lookupEls.form, isValidPhone(phone) ? [] : [[lookupEls.phone, 'Inserisci un numero di telefono valido.']])) {
    lookupEls.list.innerHTML = '';
    return;
  }

  const button = $('button[type="submit"]', lookupEls.form);
  setBusy(button, true, 'Cerco…');
  try {
    // Le prenotazioni più vecchie sono salvate con il numero così come era stato digitato.
    const variants = [...new Set([phone, compactPhone(phone)])];
    const results = await Promise.all(variants.map(variant =>
      getJson(`${API.reservations}/user/${encodeURIComponent(variant)}`)
        .then(found => (Array.isArray(found) ? found : []))));
    const seen = new Set();
    const list = results.flat()
      .filter(entry => {
        const key = `${entry.id ?? ''}|${entry.eventId ?? ''}|${entry.date}|${entry.time}|${entry.phone}`;
        if (seen.has(key)) return false;
        seen.add(key);
        return true;
      })
      .sort((a, b) => `${a.date}${a.time}`.localeCompare(`${b.date}${b.time}`));
    if (!list.length) {
      lookupMessage(`Nessuna prenotazione trovata per ${phone}.`);
    } else {
      lookupEls.list.innerHTML = list.map(entry => reservationHtml(entry, phone)).join('');
    }
  } catch (err) {
    console.error(err);
    lookupMessage('Non riusciamo a recuperare le prenotazioni. Riprova tra poco.', true);
  } finally {
    setBusy(button, false);
  }
}

async function cancelReservation(button) {
  const { phone, date, eventId } = button.dataset;
  const confirmed = await notify({
    title: 'Annullare la prenotazione?',
    text: `Stai per annullare la prenotazione di ${formatDay(date).toLowerCase()}.`,
    confirmLabel: 'Sì, annulla',
    cancelLabel: 'Tienila',
  });
  if (!confirmed) return;

  const url = eventId
    ? `${API.events}/${encodeURIComponent(eventId)}/unregister/${encodeURIComponent(phone)}`
    : `${API.reservations}/${encodeURIComponent(phone)}/${encodeURIComponent(date)}`;

  button.disabled = true;
  try {
    const res = await fetch(url, { method: 'DELETE' });
    if (!res.ok) throw new Error(`${url}: ${res.status}`);
    button.closest('.res-item').remove();
    if (!$('.res-item', lookupEls.list)) lookupMessage('Non hai altre prenotazioni attive.');
    toast('Prenotazione annullata');
    if (eventId) loadEvents({ force: true });
  } catch (err) {
    console.error(err);
    button.disabled = false;
    notify({ title: 'Annullamento non riuscito', text: 'Riprova tra poco oppure chiamaci.', tone: 'error' });
  }
}

function initLookup() {
  if (!lookupEls.form) return;
  bindLiveValidation(lookupEls.form);
  bindPhoneSanitizer(lookupEls.phone);
  lookupEls.form.addEventListener('submit', submitLookup);
  lookupEls.list.addEventListener('click', event => {
    const button = event.target.closest('[data-cancel]');
    if (button) cancelReservation(button);
  });
}

/* ---------- Tab prenotazioni ---------- */

function activateTab(panelId, { focus = false } = {}) {
  const tabs = $$('.tab[data-tab]');
  if (!tabs.some(tab => tab.dataset.tab === panelId)) return;
  tabs.forEach(tab => {
    const active = tab.dataset.tab === panelId;
    tab.classList.toggle('is-active', active);
    tab.setAttribute('aria-selected', String(active));
    tab.tabIndex = active ? 0 : -1;
    const panel = document.getElementById(tab.dataset.tab);
    if (panel) panel.hidden = !active;
    if (active && focus) tab.focus();
  });
  if (panelId === 'eventoForm') loadEvents({ force: true });
}

function initTabs() {
  const tabs = $$('.tab[data-tab]');
  tabs.forEach((tab, index) => {
    tab.addEventListener('click', () => activateTab(tab.dataset.tab));
    tab.addEventListener('keydown', event => {
      const step = { ArrowRight: 1, ArrowLeft: -1 }[event.key];
      if (!step) return;
      event.preventDefault();
      activateTab(tabs[(index + step + tabs.length) % tabs.length].dataset.tab, { focus: true });
    });
  });

  document.addEventListener('click', event => {
    const link = event.target.closest('[data-tab-link]');
    if (link) activateTab(link.dataset.tabLink);
  });
}

/* ---------- Blocco prenotazioni ("siamo pieni") ---------- */

function applyGate() {
  const tableBlocked = Boolean(state.gate.tableBookingsBlocked);
  const eventBlocked = Boolean(state.gate.eventBookingsBlocked);

  const tableBanner = $('#tableFullBanner');
  const eventBanner = $('#eventFullBanner');
  if (tableBanner) tableBanner.hidden = !tableBlocked;
  if (eventBanner) eventBanner.hidden = !eventBlocked;

  if (resEls.form) {
    $$('input, select, textarea, button', resEls.form).forEach(control => {
      control.disabled = tableBlocked || (control === resEls.time && resEls.time.options.length <= 1);
    });
  }
  renderEventGrids();
}

async function loadGate() {
  try {
    state.gate = await getJson(API.bookingSettings);
  } catch (err) {
    console.warn('Stato prenotazioni non disponibile:', err);
  }
  applyGate();
}

/* ---------- Consensi ---------- */

const consentEls = {
  dialog: $('#consentDialog'),
  booking: $('#consentBooking'),
  social: $('#consentSocial'),
};

function syncPrivacyChecks() {
  if (store.get(STORAGE.booking) !== 'true') return;
  if (resEls.privacy) resEls.privacy.checked = true;
  if (eventEls.privacy) eventEls.privacy.checked = true;
}

function setSocialEmbeds(enabled) {
  const instagram = $('#instagram-embed-container');
  const tiktok = $('#tiktok-embed-container');
  const instagramPlaceholder = $('#instagram-placeholder');
  const tiktokPlaceholder = $('#tiktok-placeholder');
  if (!instagram || !tiktok) return;

  if (!enabled) {
    instagram.innerHTML = '';
    tiktok.innerHTML = '';
    instagramPlaceholder.hidden = false;
    tiktokPlaceholder.hidden = false;
    return;
  }

  // Solo iframe isolati: nessuno script di terze parti viene eseguito nella pagina.
  const sandbox = 'allow-scripts allow-same-origin allow-popups allow-popups-to-escape-sandbox';
  if (!instagram.childElementCount) {
    instagram.innerHTML = `<iframe src="https://www.instagram.com/corner_giovinazzo/embed" title="Profilo Instagram di Corner Pub" loading="lazy" scrolling="no" sandbox="${sandbox}"></iframe>`;
    instagramPlaceholder.hidden = true;
  }
  if (!tiktok.childElementCount) {
    tiktok.innerHTML = `<iframe src="https://www.tiktok.com/embed/@cornergiovinazzo" title="Profilo TikTok di Corner Pub" loading="lazy" scrolling="no" sandbox="${sandbox}"></iframe>`;
    tiktokPlaceholder.hidden = true;
  }
}

function openConsent({ required }) {
  if (!consentEls.dialog) return;
  consentEls.booking.checked = store.get(STORAGE.booking) === 'true';
  consentEls.social.checked = store.get(STORAGE.social) === 'true';
  consentEls.dialog.dataset.locked = String(required);
  openDialog(consentEls.dialog);
}

async function saveConsent() {
  store.set(STORAGE.booking, consentEls.booking.checked);
  store.set(STORAGE.social, consentEls.social.checked);
  setSocialEmbeds(consentEls.social.checked);
  syncPrivacyChecks();
  await closeDialog(consentEls.dialog);
  toast('Preferenze salvate');
  maybeShowEventsPopup();
}

function initConsent() {
  if (!consentEls.dialog) return;
  const booking = store.get(STORAGE.booking);
  const social = store.get(STORAGE.social);

  if (booking === null || social === null) {
    openConsent({ required: true });
  } else {
    setSocialEmbeds(social === 'true');
    syncPrivacyChecks();
  }

  $('#btnSaveConsent').addEventListener('click', saveConsent);
  $('#btnAcceptAll').addEventListener('click', () => {
    consentEls.booking.checked = true;
    consentEls.social.checked = true;
    saveConsent();
  });
  $('#openPrivacySettings')?.addEventListener('click', () => openConsent({ required: false }));

  $$('[data-enable-social]').forEach(button => {
    button.addEventListener('click', () => {
      store.set(STORAGE.social, true);
      setSocialEmbeds(true);
    });
  });
}

/* ---------- Navigazione ---------- */

function initNav() {
  const header = $('#siteHeader');
  const toggle = $('#navToggle');
  const panel = $('#mobileNav');
  const root = document.documentElement;

  const setOpen = open => {
    if (!toggle || !panel) return;
    panel.hidden = !open;
    toggle.setAttribute('aria-expanded', String(open));
    toggle.setAttribute('aria-label', open ? 'Chiudi il menu di navigazione' : 'Apri il menu di navigazione');
    toggle.innerHTML = icon(open ? 'x' : 'menu');
    root.classList.toggle('nav-open', open);
    if (!$('dialog[open]')) root.classList.toggle('is-locked', open);
  };

  toggle?.addEventListener('click', () => setOpen(panel.hidden));
  panel?.addEventListener('click', event => {
    if (event.target.closest('a')) setOpen(false);
  });
  document.addEventListener('keydown', event => {
    if (event.key === 'Escape' && panel && !panel.hidden) {
      setOpen(false);
      toggle.focus();
    }
  });
  window.matchMedia('(min-width: 1024px)').addEventListener('change', event => {
    if (event.matches) setOpen(false);
  });

  const navLinks = $$('.site-nav a[data-nav]');
  const dockLinks = $$('.app-dock a[data-dock]');
  const sections = ['top', 'specialiSection', 'promotionsSection', 'menu', 'noi', 'book', 'social', 'dove']
    .map(id => document.getElementById(id))
    .filter(Boolean);
  const order = id => sections.findIndex(section => section.id === id);

  let ticking = false;
  const update = () => {
    ticking = false;
    header?.classList.toggle('is-scrolled', window.scrollY > 12);
    const line = window.scrollY + window.innerHeight * 0.35;
    let current = 'top';
    sections.forEach(section => {
      if (!section.hidden && section.offsetTop <= line) current = section.id;
    });
    navLinks.forEach(link => {
      if (link.dataset.nav === current) link.setAttribute('aria-current', 'true');
      else link.removeAttribute('aria-current');
    });
    const dockKey = order(current) >= order('book') ? 'book' : order(current) >= order('menu') ? 'menu' : 'top';
    dockLinks.forEach(link => link.classList.toggle('is-active', link.dataset.dock === dockKey));
  };
  const onScroll = () => {
    if (ticking) return;
    ticking = true;
    requestAnimationFrame(update);
  };
  window.addEventListener('scroll', onScroll, { passive: true });
  window.addEventListener('resize', onScroll, { passive: true });
  update();
}

/* ---------- Hero, animazioni, immagini ---------- */

function initHeroVideo() {
  const video = $('#heroVideo');
  if (!video || reduceMotion.matches || navigator.connection?.saveData) return;

  const start = () => {
    video.addEventListener('playing', () => video.classList.add('is-playing'), { once: true });
    video.src = video.dataset.src;
    video.play().catch(() => {});
    if (!('IntersectionObserver' in window)) return;
    new IntersectionObserver(([entry]) => {
      if (entry.isIntersecting) video.play().catch(() => {});
      else video.pause();
    }, { threshold: 0.05 }).observe(video);
  };
  if (document.readyState === 'complete') start();
  else window.addEventListener('load', start, { once: true });
}

let revealObserver = null;

function observeReveal(root = document) {
  const targets = $$('[data-reveal]:not(.is-in)', root);
  if (!revealObserver) {
    targets.forEach(el => el.classList.add('is-in'));
    return;
  }
  targets.forEach(el => revealObserver.observe(el));
}

function initReveal() {
  if ('IntersectionObserver' in window && !reduceMotion.matches) {
    revealObserver = new IntersectionObserver(entries => {
      entries.forEach(entry => {
        if (!entry.isIntersecting) return;
        entry.target.classList.add('is-in');
        revealObserver.unobserve(entry.target);
      });
    }, { threshold: 0.12, rootMargin: '0px 0px -6% 0px' });
  }
  observeReveal();
}

function initImageFallback() {
  document.addEventListener('error', event => {
    const img = event.target;
    if (!(img instanceof HTMLImageElement) || img.dataset.fallback === undefined) return;
    const next = img.dataset.fallback;
    if (next) {
      img.dataset.fallback = '';
      img.src = next;
    } else if (!img.src.endsWith(DEFAULT_PHOTO)) {
      img.src = DEFAULT_PHOTO;
    }
  }, true);
}

/* ---------- Deep link ---------- */

function hashTarget() {
  const raw = window.location.hash.slice(1);
  if (!raw) return { id: '', params: new URLSearchParams() };
  const [id, query = ''] = raw.split('?');
  let decoded = id;
  try { decoded = decodeURIComponent(id); } catch { /* hash malformato: lo uso così com'è */ }
  return { id: decoded, params: new URLSearchParams(query) };
}

function routeHash({ scroll = false } = {}) {
  const { id, params } = hashTarget();
  if (!id) return;
  if (id === 'menu' && params.get('cat')) {
    menu.pendingCategory = params.get('cat');
    applyPendingCategory();
  }
  let target = document.getElementById(id);
  const isTab = id === 'myReservationsSection' || id === 'eventoForm' || id === 'tavoloForm';
  if (isTab) {
    activateTab(id);
    target = $('#book');
  }
  // I pannelli nascosti non sono raggiungibili dall'ancora nativa: ci porto io la pagina.
  if ((scroll || isTab) && target && !target.hidden) target.scrollIntoView({ behavior: 'auto', block: 'start' });
}

/* ---------- Avvio ---------- */

function init() {
  const year = $('#displayYear');
  if (year) year.textContent = String(new Date().getFullYear());

  initImageFallback();
  initDialogs();
  initReveal();
  initNav();
  initHeroVideo();
  initMenu();
  initSpeciali();
  initPromotions();
  initTabs();
  initReservation();
  initEvents();
  initLookup();

  // Senza un deep link la pagina riparte sempre dall'inizio.
  if ('scrollRestoration' in history) history.scrollRestoration = 'manual';
  const hasDeepLink = Boolean(hashTarget().id) && hashTarget().id !== 'top';
  if (!hasDeepLink) window.scrollTo(0, 0);
  routeHash();
  window.addEventListener('hashchange', () => routeHash());

  // Le sezioni sopra il target si popolano in modo asincrono: riallineo quando hanno finito,
  // a meno che nel frattempo l'utente abbia già iniziato a scorrere.
  let userScrolled = false;
  const markScrolled = () => { userScrolled = true; };
  ['wheel', 'touchmove', 'keydown'].forEach(type =>
    window.addEventListener(type, markScrolled, { once: true, passive: true }));

  const content = Promise.allSettled([loadMenu(), loadSpeciali(), loadPromotions()]);
  if (hasDeepLink) {
    content.then(() => {
      if (!userScrolled) routeHash({ scroll: true });
    });
  }

  loadGate();
  loadEvents();
  initConsent();
  maybeShowEventsPopup();
}

init();
