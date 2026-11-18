const eventsEl = document.getElementById("events");
const form = document.getElementById("eventForm");
const formStatus = document.getElementById("formStatus");
const attendeesEl = document.getElementById("attendees");
const detailTitle = document.getElementById("detailTitle");
const checkinStatus = document.getElementById("checkinStatus");
const scanVideo = document.getElementById("scanVideo");
let events = [];
let selectedId = null;
let scanning = false;
let stream = null;

function esc(value) {
  return String(value ?? "").replace(/[&<>"']/g, (ch) => ({
    "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;"
  }[ch]));
}

function fmt(iso) {
  if (!iso) return "";
  const date = new Date(iso);
  if (Number.isNaN(date.getTime())) return String(iso).replace("T", " ").slice(0, 16);
  return date.toLocaleString("it-IT", { weekday: "short", day: "2-digit", month: "short", hour: "2-digit", minute: "2-digit" });
}

async function api(url, options = {}) {
  const res = await fetch(url, {
    credentials: "same-origin",
    headers: { "Accept": "application/json", ...(options.body ? { "Content-Type": "application/json" } : {}), ...(options.headers || {}) },
    ...options
  });
  if (res.redirected || (res.url && res.url.includes("login.html"))) {
    window.location.href = "login.html";
    throw new Error("Sessione scaduta");
  }
  if (res.status === 401) {
    window.location.href = "login.html";
    throw new Error("Sessione scaduta");
  }
  const text = await res.text();
  let body = {};
  if (text) {
    try { body = JSON.parse(text); } catch (e) { body = { error: text.slice(0, 180) }; }
  }
  if (!res.ok) throw new Error(body.error || "Operazione non riuscita");
  return body;
}

function selectedEvent() {
  return events.find((event) => event.id === selectedId);
}

function renderEvents() {
  if (!events.length) {
    eventsEl.innerHTML = "<p>Nessuna uscita. Creane una dal modulo.</p>";
    return;
  }
  eventsEl.innerHTML = events.map((event) => {
    const left = event.postiDisponibili == null ? "?" : event.postiDisponibili;
    return `<article class="event">
      <strong>${esc(event.titolo)}</strong>
      <p>${esc(fmt(event.data))} · ${esc(event.luogo || "")}<br>${esc(event.distanza || "")} ${event.ritmo ? "· " + esc(event.ritmo) : ""} · ${esc(event.postiOccupati || 0)}/${esc(event.postiTotali || "∞")} · liberi ${esc(left)}</p>
      <span class="badge">${event.annullata ? "Annullata" : "Aperta"}</span>
      <div>
        <button class="btn small" type="button" data-open="${event.id}">Iscritti</button>
        <button class="btn small secondary" type="button" data-edit="${event.id}">Modifica</button>
        <button class="btn small secondary" type="button" data-toggle="${event.id}">${event.annullata ? "Riapri" : "Annulla uscita"}</button>
        <button class="btn small secondary" type="button" data-delete="${event.id}">Elimina</button>
      </div>
    </article>`;
  }).join("");
}

function fillForm(event) {
  form.dataset.id = event ? event.id : "";
  form.titolo.value = event ? event.titolo : "";
  form.descrizione.value = event ? (event.descrizione || "") : "";
  form.data.value = event && event.data ? event.data.slice(0, 16) : "";
  form.postiTotali.value = event ? (event.postiTotali || "") : "20";
  form.luogo.value = event ? (event.luogo || "") : "Corner Pub, Piazza Duomo 58, Giovinazzo";
  form.distanza.value = event ? (event.distanza || "") : "";
  form.ritmo.value = event ? (event.ritmo || "") : "";
  form.ricorrente.checked = false;
  form.ricorrente.disabled = Boolean(event);
  form.giornoSettimana.disabled = Boolean(event);
  form.dataFine.disabled = Boolean(event);
  document.getElementById("seriesWrap").classList.toggle("hidden", !event || !event.parentEventId);
  document.getElementById("recurringWrap").classList.toggle("hidden", Boolean(event));
  document.getElementById("saveEvent").textContent = event ? "Salva modifiche" : "Pubblica uscita";
}

function payloadFromForm() {
  const data = new FormData(form);
  return {
    titolo: String(data.get("titolo") || "").trim(),
    descrizione: String(data.get("descrizione") || "").trim(),
    data: data.get("data") ? String(data.get("data")) + ":00" : null,
    postiTotali: Number(data.get("postiTotali")),
    luogo: String(data.get("luogo") || "").trim(),
    distanza: String(data.get("distanza") || "").trim(),
    ritmo: String(data.get("ritmo") || "").trim(),
    ricorrente: form.ricorrente.checked,
    giornoSettimana: form.ricorrente.checked ? Number(data.get("giornoSettimana")) : null,
    dataFine: data.get("dataFine") || null,
    applicaATuttaLaSerie: form.applicaATuttaLaSerie ? form.applicaATuttaLaSerie.checked : false
  };
}

async function loadEvents() {
  events = await api("/admin/run-club/events");
  renderEvents();
  const walk = document.getElementById("walkEvent");
  walk.innerHTML = events.filter((event) => !event.annullata).map((event) =>
    `<option value="${event.id}">${esc(event.titolo)} · ${esc(fmt(event.data))}</option>`
  ).join("");
}

async function loadAttendees(id) {
  selectedId = id;
  const event = selectedEvent();
  detailTitle.textContent = event ? event.titolo + " · " + fmt(event.data) : "Iscritti";
  const rows = await api("/admin/run-club/events/" + id + "/attendees");
  if (!rows.length) {
    attendeesEl.innerHTML = "<p>Nessun iscritto.</p>";
    return;
  }
  attendeesEl.innerHTML = `<div class="table-wrap"><table>
    <thead><tr><th>Runner</th><th>Telefono</th><th>Policy</th><th>Ingresso</th><th></th></tr></thead>
    <tbody>${rows.map((row) => `<tr>
      <td>${esc(row.name)} ${esc(row.surname)}<br><code>${esc(row.ticketCode || "")}</code></td>
      <td>${esc(row.phone)}</td>
      <td>Privacy ${esc(row.privacyPolicyVersion || "-")}<br>Regolamento ${esc(row.rulesVersion || "-")}<br>Idoneità ${row.fitnessDeclared ? "sì" : "no"} · Foto ${row.photoConsent ? "sì" : "no"}</td>
      <td>${row.checkedInAt ? "Dentro alle " + esc(fmt(row.checkedInAt)) : "Non ancora"}</td>
      <td>
        ${row.ticketCode ? `<a href="/api/run-club/tickets/${encodeURIComponent(row.ticketCode)}/pdf">PDF</a>` : ""}
        <button class="btn small secondary" type="button" data-remove="${row.user && row.user.id}">Rimuovi</button>
      </td>
    </tr>`).join("")}</tbody></table></div>`;
}

eventsEl.addEventListener("click", async (event) => {
  const open = event.target.closest("[data-open]");
  const edit = event.target.closest("[data-edit]");
  const toggle = event.target.closest("[data-toggle]");
  const remove = event.target.closest("[data-delete]");
  try {
    if (open) await loadAttendees(Number(open.dataset.open));
    if (edit) {
      const found = events.find((item) => item.id === Number(edit.dataset.edit));
      if (found) fillForm(found);
    }
    if (toggle) {
      const found = events.find((item) => item.id === Number(toggle.dataset.toggle));
      const path = found && found.annullata ? "riapri" : "annulla";
      await api("/admin/run-club/events/" + toggle.dataset.toggle + "/" + path, { method: "POST" });
      await loadEvents();
    }
    if (remove) {
      if (!window.confirm("Eliminare questa data? Si può fare solo se non ci sono iscritti.")) return;
      await api("/admin/run-club/events/" + remove.dataset.delete, { method: "DELETE" });
      await loadEvents();
    }
  } catch (err) {
    formStatus.textContent = err.message;
    formStatus.className = "status error";
  }
});

form.addEventListener("submit", async (event) => {
  event.preventDefault();
  formStatus.textContent = "Salvataggio…";
  formStatus.className = "status";
  try {
    const id = form.dataset.id;
    await api(id ? "/admin/run-club/events/" + id : "/admin/run-club/events", {
      method: id ? "PUT" : "POST",
      body: JSON.stringify(payloadFromForm())
    });
    formStatus.textContent = id ? "Uscita aggiornata." : "Uscita pubblicata.";
    formStatus.className = "status ok";
    fillForm(null);
    await loadEvents();
  } catch (err) {
    formStatus.textContent = err.message;
    formStatus.className = "status error";
  }
});

document.getElementById("newEvent").addEventListener("click", () => {
  fillForm(null);
  formStatus.textContent = "";
});

document.getElementById("walkForm").addEventListener("submit", async (event) => {
  event.preventDefault();
  const status = document.getElementById("walkStatus");
  if (!document.getElementById("walkConsent").checked) {
    status.textContent = "Conferma che la persona ha accettato privacy, regolamento e idoneità.";
    status.className = "status error";
    return;
  }
  const data = new FormData(event.target);
  status.textContent = "Iscrizione…";
  status.className = "status";
  try {
    const body = await api("/admin/run-club/events/" + data.get("eventId") + "/register", {
      method: "POST",
      body: JSON.stringify({
        name: data.get("name"),
        surname: data.get("surname"),
        phone: data.get("phone"),
        note: data.get("note"),
        partecipanti: 1,
        privacyAccepted: true,
        rulesAccepted: true,
        fitnessDeclared: true,
        photoConsent: document.getElementById("walkPhoto").checked
      })
    });
    status.textContent = "Iscritto. Pass " + (body.ticketCode || "");
    status.className = "status ok";
    event.target.reset();
    document.getElementById("walkConsent").checked = false;
    await loadEvents();
    if (selectedId) await loadAttendees(selectedId);
  } catch (err) {
    status.textContent = err.message;
    status.className = "status error";
  }
});

async function submitCheckin(undo) {
  const code = document.getElementById("checkinCode").value;
  checkinStatus.textContent = "Controllo…";
  checkinStatus.className = "status";
  try {
    const body = await api(undo ? "/admin/run-club/checkin/undo" : "/admin/run-club/checkin", {
      method: "POST",
      body: JSON.stringify({ code })
    });
    const reg = body.registration || {};
    const event = reg.event || {};
    checkinStatus.textContent = (reg.name || "") + " " + (reg.surname || "") + " · " + (event.titolo || "")
      + (undo ? " · ingresso annullato" : body.alreadyCheckedIn ? " · era già dentro" : " · ingresso registrato");
    checkinStatus.className = "status ok";
    if (event.id) await loadAttendees(event.id);
  } catch (err) {
    checkinStatus.textContent = err.message;
    checkinStatus.className = "status error";
  }
}

document.getElementById("checkinForm").addEventListener("submit", (event) => {
  event.preventDefault();
  submitCheckin(false);
});
document.getElementById("undoCheckin").addEventListener("click", () => submitCheckin(true));

attendeesEl.addEventListener("click", async (event) => {
  const button = event.target.closest("[data-remove]");
  if (!button || !selectedId) return;
  if (!window.confirm("Rimuovere questa iscrizione? Il pass non sarà più valido.")) return;
  try {
    await api("/admin/events/" + selectedId + "/unregister/" + button.dataset.remove, { method: "DELETE" });
    await loadAttendees(selectedId);
    await loadEvents();
  } catch (err) {
    checkinStatus.textContent = err.message;
    checkinStatus.className = "status error";
  }
});

async function startScan() {
  if (!("BarcodeDetector" in window)) {
    checkinStatus.textContent = "Questa fotocamera non legge i QR dal browser. Inserisci il codice a mano.";
    checkinStatus.className = "status error";
    return;
  }
  stream = await navigator.mediaDevices.getUserMedia({ video: { facingMode: "environment" } });
  scanVideo.srcObject = stream;
  scanVideo.classList.remove("hidden");
  await scanVideo.play();
  scanning = true;
  const detector = new BarcodeDetector({ formats: ["qr_code"] });
  const tick = async () => {
    if (!scanning) return;
    try {
      const codes = await detector.detect(scanVideo);
      if (codes.length) {
        document.getElementById("checkinCode").value = codes[0].rawValue || "";
        stopScan();
        submitCheckin(false);
        return;
      }
    } catch (e) { /* frame vuoto */ }
    requestAnimationFrame(() => tick());
  };
  tick();
}

function stopScan() {
  scanning = false;
  if (stream) stream.getTracks().forEach((track) => track.stop());
  stream = null;
  scanVideo.classList.add("hidden");
}

document.getElementById("startScan").addEventListener("click", () => startScan().catch((err) => {
  checkinStatus.textContent = err.message || "Fotocamera non disponibile";
  checkinStatus.className = "status error";
}));
document.getElementById("stopScan").addEventListener("click", stopScan);

fillForm(null);
loadEvents().catch((err) => {
  eventsEl.innerHTML = "<p>" + esc(err.message) + "</p>";
});

const incoming = new URLSearchParams(location.search).get("code");
if (incoming) {
  document.getElementById("checkinCode").value = incoming;
  submitCheckin(false);
}
