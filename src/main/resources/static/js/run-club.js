const state = { events: [], pass: null };

const eventsEl = document.getElementById("events");
const formPanel = document.getElementById("formPanel");
const form = document.getElementById("joinForm");
const formTitle = document.getElementById("formTitle");
const formStatus = document.getElementById("formStatus");
const passPanel = document.getElementById("passPanel");
const recoverForm = document.getElementById("recoverForm");
const recoverEvent = document.getElementById("recoverEvent");
const recoverStatus = document.getElementById("recoverStatus");

function esc(value) {
  return String(value ?? "").replace(/[&<>"']/g, (ch) => ({
    "&": "&amp;",
    "<": "&lt;",
    ">": "&gt;",
    '"': "&quot;",
    "'": "&#39;"
  }[ch]));
}

function fmt(iso) {
  if (!iso) return "";
  const date = new Date(iso);
  if (Number.isNaN(date.getTime())) return String(iso).replace("T", " ");
  return date.toLocaleString("it-IT", {
    weekday: "long",
    day: "numeric",
    month: "long",
    year: "numeric",
    hour: "2-digit",
    minute: "2-digit"
  });
}

async function readBody(res) {
  const text = await res.text();
  if (!text) return {};
  try { return JSON.parse(text); } catch (e) { return { error: text.slice(0, 180) }; }
}

function eventById(id) {
  return state.events.find((event) => String(event.id) === String(id));
}

function renderEvents() {
  if (!state.events.length) {
    eventsEl.innerHTML = "<article class='card'><strong>Nessuna uscita in programma</strong><p class='meta'>Quando il Corner pubblica la prossima corsa, la trovi qui. Nel frattempo passa dal pub in Piazza Duomo 58.</p></article>";
    recoverEvent.innerHTML = "<option value=''>Nessuna uscita</option>";
    return;
  }
  eventsEl.innerHTML = state.events.map((event) => {
    const left = event.postiDisponibili;
    const full = left === 0;
    const bits = [fmt(event.data), event.luogo, event.distanza, event.ritmo].filter(Boolean);
    return `<article class="event-card">
      <div>
        <h3>${esc(event.titolo)}</h3>
        <p class="meta">${esc(bits.join(" · "))}</p>
        ${event.descrizione ? `<p>${esc(event.descrizione)}</p>` : ""}
        <span class="badge ${full ? "full" : "ok"}">${full ? "Posti esauriti" : esc(left + " posti liberi")}</span>
      </div>
      <div class="row-actions">
        <button class="btn" type="button" data-join="${event.id}" ${full ? "disabled" : ""}>${full ? "Posti esauriti" : "Prenotati"}</button>
      </div>
    </article>`;
  }).join("");
  recoverEvent.innerHTML = state.events.map((event) =>
    `<option value="${event.id}">${esc(event.titolo)} · ${esc(fmt(event.data))}</option>`
  ).join("");
}

function openForm(event) {
  form.dataset.eventId = event.id;
  formTitle.textContent = event.titolo;
  formStatus.textContent = fmt(event.data) + " · " + (event.luogo || "");
  formStatus.className = "status";
  form.hidden = false;
  passPanel.hidden = true;
  formPanel.hidden = false;
  form.querySelectorAll("input[type=checkbox]").forEach((box) => { box.checked = false; });
  formPanel.scrollIntoView({ behavior: "smooth", block: "start" });
}

function showPass(pass) {
  state.pass = pass;
  sessionStorage.setItem("crc-pass", JSON.stringify(pass));
  document.getElementById("passName").textContent = pass.name + " " + pass.surname;
  document.getElementById("passEvent").textContent = pass.eventTitle || "";
  document.getElementById("passWhen").textContent = pass.when || "";
  document.getElementById("passCode").textContent = pass.ticketCode;
  document.getElementById("passPdf").href = "/api/run-club/tickets/" + encodeURIComponent(pass.ticketCode) + "/pdf";
  form.hidden = true;
  passPanel.hidden = false;
  formPanel.hidden = false;
  passPanel.scrollIntoView({ behavior: "smooth", block: "start" });
}

eventsEl.addEventListener("click", (event) => {
  const button = event.target.closest("[data-join]");
  if (!button) return;
  const selected = eventById(button.dataset.join);
  if (selected) openForm(selected);
});

form.addEventListener("submit", async (event) => {
  event.preventDefault();
  const data = new FormData(form);
  const phone = String(data.get("phone") || "").replace(/\D/g, "");
  const payload = {
    name: String(data.get("name") || "").trim(),
    surname: String(data.get("surname") || "").trim(),
    phone,
    note: String(data.get("note") || "").trim(),
    partecipanti: 1,
    privacyAccepted: form.privacy.checked,
    rulesAccepted: form.rules.checked,
    fitnessDeclared: form.fitness.checked,
    photoConsent: form.photo.checked
  };
  if (!payload.privacyAccepted || !payload.rulesAccepted || !payload.fitnessDeclared) {
    formStatus.textContent = "Per prenotarti servono informativa privacy, regolamento e dichiarazione di idoneità.";
    formStatus.className = "status error";
    return;
  }
  const button = form.querySelector("button[type=submit]");
  button.disabled = true;
  formStatus.textContent = "Iscrizione in corso…";
  formStatus.className = "status";
  try {
    const res = await fetch("/api/run-club/events/" + form.dataset.eventId + "/register", {
      method: "POST",
      headers: { "Content-Type": "application/json", "Accept": "application/json" },
      body: JSON.stringify(payload)
    });
    const body = await readBody(res);
    if (!res.ok) throw new Error(body.error || "Iscrizione non riuscita");
    const selected = eventById(form.dataset.eventId);
    showPass({
      ticketCode: body.ticketCode,
      name: payload.name,
      surname: payload.surname,
      eventId: Number(form.dataset.eventId),
      eventTitle: selected ? selected.titolo : "",
      when: selected ? fmt(selected.data) : "",
      phone
    });
    form.reset();
    loadEvents();
  } catch (err) {
    formStatus.textContent = err.message;
    formStatus.className = "status error";
  } finally {
    button.disabled = false;
  }
});

document.getElementById("cancelPass").addEventListener("click", async () => {
  if (!state.pass) return;
  const yes = window.confirm("Annullare l'iscrizione di " + state.pass.name + "? Il pass non sarà più valido.");
  if (!yes) return;
  const res = await fetch("/api/events/" + state.pass.eventId + "/unregister/" + encodeURIComponent(state.pass.phone), {
    method: "DELETE",
    headers: { "Accept": "application/json" }
  });
  const body = await readBody(res);
  if (!res.ok) {
    formStatus.textContent = body.error || "Annullamento non riuscito";
    formStatus.className = "status error";
    form.hidden = false;
    return;
  }
  sessionStorage.removeItem("crc-pass");
  state.pass = null;
  passPanel.hidden = true;
  formPanel.hidden = true;
  loadEvents();
});

recoverForm.addEventListener("submit", async (event) => {
  event.preventDefault();
  const data = new FormData(recoverForm);
  recoverStatus.textContent = "Cerco il pass…";
  recoverStatus.className = "status";
  const res = await fetch("/api/run-club/tickets/recover", {
    method: "POST",
    headers: { "Content-Type": "application/json", "Accept": "application/json" },
    body: JSON.stringify({
      eventId: Number(data.get("eventId")),
      phone: String(data.get("phone") || "")
    })
  });
  const body = await readBody(res);
  if (!res.ok) {
    recoverStatus.textContent = body.error || "Pass non trovato";
    recoverStatus.className = "status error";
    return;
  }
  const selected = eventById(body.eventId);
  recoverStatus.textContent = "Pass trovato.";
  recoverStatus.className = "status ok";
  showPass({
    ticketCode: body.ticketCode,
    name: body.name,
    surname: body.surname,
    eventId: body.eventId,
    eventTitle: body.eventTitle,
    when: selected ? fmt(selected.data) : "",
    phone: String(data.get("phone") || "").replace(/\D/g, "")
  });
});

async function loadEvents() {
  eventsEl.innerHTML = "<p class='meta'>Carico le uscite…</p>";
  try {
    const res = await fetch("/api/run-club/events", { headers: { "Accept": "application/json" } });
    if (!res.ok) throw new Error("Impossibile caricare le uscite");
    state.events = await res.json();
    renderEvents();
  } catch (err) {
    eventsEl.innerHTML = "<article class='card'><strong>Le uscite non sono disponibili</strong><p class='meta'>Ricarica la pagina tra poco. Se il problema resta, scrivi a cornersnc@gmail.com.</p></article>";
  }
}

const saved = sessionStorage.getItem("crc-pass");
if (saved) {
  try { showPass(JSON.parse(saved)); } catch (e) { sessionStorage.removeItem("crc-pass"); }
}
loadEvents();
