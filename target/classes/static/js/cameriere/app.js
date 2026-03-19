import Auth from '../core/auth.js';
import API from '../core/api.js';
import WSClient from '../core/ws.js';

class CameriereApp {
    constructor() {
        this.state = {
            tables: [],
            activeOrders: [],
            categories: [],
            menuItems: [],
            currentTableId: null,
            currentTableData: null,  // Oggetto TableSession completo (include copeRti)
            currentOrderId: null,
            currentOrderStatus: null,
            cartOpenItems: [],
            searchQuery: '',
            selectedCategory: null
        };
        this.wsClient = null;

        this.init();
    }

    async init() {
        if (!Auth.isAuthenticated()) {
            this.showView('login');
        } else {
            await this.loadInitialData();
            this.showView('tables');
            this.connectWebSocket();
            this.startPolling();
        }
        this.bindEvents();
    }

    startPolling() {
        // Intervallo di polling adattivo: 
        // 30s se il WebSocket è connesso e funzionante, 5s se siamo disconnessi (fallback puro).
        const tick = async () => {
            if (Auth.isAuthenticated()) await this.pollUpdates();
            const nextDelay = (this.wsClient && this.wsClient.isConnected) ? 30000 : 5000;
            this._pollTimer = setTimeout(tick, nextDelay);
        };
        this._pollTimer = setTimeout(tick, 5000);
    }

    bindEvents() {
        // Login
        document.getElementById('login-form').addEventListener('submit', async (e) => {
            e.preventDefault();
            const u = document.getElementById('username').value;
            const p = document.getElementById('password').value;
            if (await Auth.login(u, p)) {
                await this.loadInitialData();
                this.showView('tables');
            } else {
                Swal.fire({ icon: 'error', title: 'Errore', text: 'Credenziali non valide' });
            }
        });

        document.getElementById('btn-logout').addEventListener('click', () => {
            if (this.wsClient) this.wsClient.disconnect();
            Auth.logout();
            window.location.reload();
        });

        document.getElementById('btn-tab-tables')?.addEventListener('click', () => {
            document.getElementById('btn-tab-tables').className = 'btn btn-primary';
            document.getElementById('btn-tab-tables').style.background = '';
            document.getElementById('btn-tab-tables').style.color = '';

            document.getElementById('btn-tab-archived').className = 'btn';
            document.getElementById('btn-tab-archived').style.background = 'var(--surface-color)';
            document.getElementById('btn-tab-archived').style.border = '1px solid #ccc';
            document.getElementById('btn-tab-archived').style.color = '#333';

            document.getElementById('tables-list').style.display = 'grid';
            document.getElementById('archived-list').style.display = 'none';
            document.getElementById('btn-new-table').style.display = 'flex';
        });

        document.getElementById('btn-tab-archived')?.addEventListener('click', () => {
            document.getElementById('btn-tab-archived').className = 'btn btn-primary';
            document.getElementById('btn-tab-archived').style.background = '';
            document.getElementById('btn-tab-archived').style.color = '';

            document.getElementById('btn-tab-tables').className = 'btn';
            document.getElementById('btn-tab-tables').style.background = 'var(--surface-color)';
            document.getElementById('btn-tab-tables').style.border = '1px solid #ccc';
            document.getElementById('btn-tab-tables').style.color = '#333';

            document.getElementById('tables-list').style.display = 'none';
            document.getElementById('archived-list').style.display = 'grid';
            document.getElementById('btn-new-table').style.display = 'none';

            this.loadArchivedOrders();
        });

        // Tables
        document.getElementById('btn-new-table').addEventListener('click', () => this.createNewTable());
        document.getElementById('btn-back-tables').addEventListener('click', async () => {
            await this.fetchTables();
            this.showView('tables');
        });

        // Menu Search & Filter
        let searchTimeout;
        document.getElementById('menu-search').addEventListener('input', (e) => {
            clearTimeout(searchTimeout);
            searchTimeout = setTimeout(() => {
                this.state.searchQuery = e.target.value.toLowerCase();
                this.renderMenu();
            }, 100); // Debounce rapido per digitazione fluida
        });

        // Modals
        document.getElementById('btn-close-modal').addEventListener('click', () => {
            document.getElementById('modal-add-item').classList.remove('active');
        });
        document.getElementById('btn-qty-minus').addEventListener('click', () => {
            const el = document.getElementById('modal-qty');
            let q = parseInt(el.innerText);
            if (q > 1) el.innerText = q - 1;
        });
        document.getElementById('btn-qty-plus').addEventListener('click', () => {
            const el = document.getElementById('modal-qty');
            el.innerText = parseInt(el.innerText) + 1;
        });
        document.getElementById('btn-confirm-add').addEventListener('click', () => this.confirmAddItem());

        // Order Actions
        document.getElementById('btn-send-order').addEventListener('click', () => this.sendOrder());
        document.getElementById('btn-view-cart').addEventListener('click', () => this.viewCart());
        document.getElementById('btn-table-history').addEventListener('click', () => this.viewTableHistory());
    }

    showView(viewName) {
        document.getElementById('view-login').style.display = 'none';
        document.getElementById('view-tables').style.display = 'none';
        document.getElementById('view-order').style.display = 'none';
        document.getElementById(`view-${viewName}`).style.display = viewName === 'login' ? 'flex' : 'block';
        window.scrollTo(0, 0);
    }

    connectWebSocket() {
        const endpoint = '/ws-orders';
        this.wsClient = new WSClient(endpoint, {
            '/topic/kitchen/orders': (msg) => this.handleKitchenEvent(msg)
        });
        this.wsClient.connect().catch(e => console.error('WS Cameriere Error', e));
    }

    /**
     * Gestisce eventi WS strutturati.
     */
    async handleKitchenEvent(rawMsg) {
        let payload = null;
        try { payload = JSON.parse(rawMsg); }
        catch (_) { payload = { eventType: 'STATUS_CHANGED' }; }

        // Debounce del pollUpdates per evitare doppi refetch quando arrivano eventi multipli ravvicinati
        clearTimeout(this._wsDebounceTimer);
        this._wsDebounceTimer = setTimeout(async () => {
            await this.pollUpdates();
        }, 50);

        // Se c'è un cambio di stato su un ordine aperto, mostra toast
        if (payload && payload.eventType === 'STATUS_CHANGED' && payload.status) {
            if (this.state.currentOrderId && document.getElementById('view-order').style.display !== 'none') {
                const statusLabels = {
                    IN_PREPARATION: '👨‍🍳 In Preparazione',
                    DONE: '✅ Pronta per il Servizio',
                    ARCHIVED: '🗄️ Archiviata'
                };
                const label = statusLabels[payload.status];
                if (label) {
                    Swal.fire({ toast: true, position: 'top', showConfirmButton: false, timer: 3500, icon: 'info', title: label });
                }
            }
        }
    }

    async pollUpdates() {
        if (document.getElementById('view-tables').style.display !== 'none') {
            try {
                if (document.getElementById('tables-list').style.display !== 'none') {
                    const [tables, activeOrders] = await Promise.all([
                        API.get('/api/cameriere/tables/open'),
                        API.get('/api/cameriere/orders/active')
                    ]);

                    const tJson = JSON.stringify(tables);
                    const oJson = JSON.stringify(activeOrders);

                    if (this.lastTJson !== tJson || this.lastOJson !== oJson) {
                        this.state.tables = tables;
                        this.state.activeOrders = activeOrders;
                        this.lastTJson = tJson;
                        this.lastOJson = oJson;
                        this.renderTables();
                    }
                } else if (document.getElementById('archived-list').style.display !== 'none') {
                    this.loadArchivedOrders();
                }
            } catch (e) { }
        }

        if (this.state.currentTableId && document.getElementById('view-order').style.display !== 'none') {
            try {
                const draftOrder = await API.get(`/api/cameriere/tables/${this.state.currentTableId}/orders/current`);
                const dJson = JSON.stringify(draftOrder);

                if (this.lastDraftJson !== dJson) {
                    if (this.state.currentOrderStatus !== draftOrder.status) {
                        if (draftOrder.status !== 'DRAFT') {
                            const statusLabels = {
                                SENT: '📨 Comanda Inviata',
                                IN_PREPARATION: '👨‍🍳 In Preparazione',
                                DONE: '✅ Pronta per il Servizio'
                            };
                            const label = statusLabels[draftOrder.status] || `Stato: ${draftOrder.status}`;
                            Swal.fire({ toast: true, position: 'top', showConfirmButton: false, timer: 3000, icon: 'info', title: label });
                        }
                    }
                    this.state.cartOpenItems = draftOrder.items || [];
                    this.state.currentOrderStatus = draftOrder.status;
                    this.lastDraftJson = dJson;
                    this.updateBadge();
                    this.updateOrderStatusUI();

                    if (Swal.isVisible() && Swal.getTitle() && Swal.getTitle().innerText === 'Riepilogo') {
                        this.viewCart();
                    }
                }
            } catch (e) { }
        }
    }

    // --- Data Loading --- //

    async loadInitialData() {
        try {
            const [tables, categories, menuItems, activeOrders] = await Promise.all([
                API.get('/api/cameriere/tables/open'),
                API.get('/api/cameriere/menu/categories'),
                API.get('/api/cameriere/menu/items'),
                API.get('/api/cameriere/orders/active')
            ]);
            this.state.tables = tables;
            this.state.categories = categories;
            this.state.menuItems = menuItems;
            this.state.activeOrders = activeOrders;

            this.renderTables();
            this.renderCategories();
        } catch (e) {
            Swal.fire({ icon: 'error', title: 'Errore Rete', text: 'Impossibile caricare i dati' });
        }
    }

    async fetchTables() {
        this.pollUpdates();
    }

    // --- Tables Rendering --- //

    renderTables() {
        const container = document.getElementById('tables-list');
        container.innerHTML = '';
        if (this.state.tables.length === 0) {
            container.innerHTML = `<p class="empty-state">Nessun tavolo aperto.</p>`;
            return;
        }

        this.state.tables.forEach(t => {
            const order = this.state.activeOrders.find(o => o.tableSession.id === t.id && o.status !== 'DRAFT');
            let orderStatusHtml = '';
            if (order) {
                const colors = {
                    DONE: { color: '#059669', bg: '#D1FAE5' },
                    IN_PREPARATION: { color: '#1D4ED8', bg: '#DBEAFE' },
                    SENT: { color: '#D97706', bg: '#FEF3C7' }
                };
                const c = colors[order.status] || { color: '#6B7280', bg: '#F3F4F6' };
                orderStatusHtml = `<div style="margin-top:0.5rem; font-size:0.75rem; background:${c.bg}; color:${c.color}; padding:2px 8px; border-radius:12px; display:inline-block; font-weight:600;">Ordine: ${order.status}</div>`;
            }

            const copeRtiHtml = t.copeRti
                ? `<div style="margin-top:0.3rem; font-size:0.8rem; color:var(--text-secondary);">👥 ${t.copeRti} coperti</div>`
                : '';

            const div = document.createElement('div');
            div.className = 'table-card';
            div.innerHTML = `
                <div class="table-card-top">
                    <span class="table-num">Tavolo ${t.tableNumber}</span>
                    <span class="table-status status-${t.status.toLowerCase()}">${t.status}</span>
                </div>
                ${copeRtiHtml}
                <div style="color:var(--text-secondary); font-size:0.875rem; margin-top:0.3rem;">
                    Aperto: ${new Date(t.openedAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}
                </div>
                ${t.generalNotes ? `<div style="margin-top:0.5rem; font-size:0.875rem; color:#D97706;">📝 ${t.generalNotes}</div>` : ''}
                ${orderStatusHtml}
            `;
            div.addEventListener('click', () => this.openTableSession(t));
            container.appendChild(div);
        });
    }

    async createNewTable() {
        const { value: formValues } = await Swal.fire({
            title: 'Nuovo Tavolo',
            html: `
                <input id="swal-input1" class="swal2-input" placeholder="Numero Tavolo (es. 12)" type="text">
                <input id="swal-input2" class="swal2-input" placeholder="Coperti (es. 4)" type="number" min="1">
                <input id="swal-input3" class="swal2-input" placeholder="Note (opzionale)">
            `,
            focusConfirm: false,
            showCancelButton: true,
            confirmButtonText: 'Apri Tavolo',
            cancelButtonText: 'Annulla',
            preConfirm: () => {
                const tableNum = document.getElementById('swal-input1').value.trim();
                const coperti = document.getElementById('swal-input2').value.trim();
                const notes = document.getElementById('swal-input3').value.trim();
                if (!tableNum) {
                    Swal.showValidationMessage('Inserisci il numero tavolo');
                    return false;
                }
                return [tableNum, coperti, notes];
            }
        });

        if (formValues && formValues[0]) {
            try {
                const newTable = await API.post('/api/cameriere/tables', {
                    tableNumber: formValues[0],
                    copeRti: formValues[1] ? parseInt(formValues[1]) : null,
                    generalNotes: formValues[2] || null
                });
                await this.fetchTables();
                this.openTableSession(newTable);
            } catch (e) {
                Swal.fire('Errore', 'Impossibile creare il tavolo', 'error');
            }
        }
    }

    async openTableSession(table) {
        this.state.currentTableId = table.id;
        this.state.currentTableData = table;
        document.getElementById('current-table-display').innerText = table.tableNumber;

        // Mostra coperti nell'header se disponibili
        const copeRtiEl = document.getElementById('current-table-coperti');
        if (copeRtiEl) {
            copeRtiEl.innerText = table.copeRti ? `👥 ${table.copeRti}` : '';
        }

        try {
            // getOrCreateDraftOrder ora crea SEMPRE una nuova comanda DRAFT
            // se tutte le precedenti sono già state inviate.
            let draftOrder = await API.post(`/api/cameriere/tables/${table.id}/orders`, {});
            this.state.currentOrderId = draftOrder.id;
            this.state.cartOpenItems = draftOrder.items || [];
            this.state.currentOrderStatus = draftOrder.status;

            // Controlla quante comande già inviate esistono per questo tavolo
            // per mostrare il numero della comanda corrente all'operatore
            const history = await API.get(`/api/cameriere/tables/${table.id}/orders/history`);
            const sentCount = history.filter(o => o.status !== 'DRAFT').length;
            if (sentCount > 0) {
                Swal.fire({
                    toast: true, position: 'top', showConfirmButton: false, timer: 3000,
                    icon: 'info',
                    title: `Comanda #${sentCount + 1} — ${sentCount} già inviat${sentCount === 1 ? 'a' : 'e'} in cucina`
                });
            }

            this.updateBadge();
            this.updateOrderStatusUI();

            this.state.searchQuery = '';
            document.getElementById('menu-search').value = '';
            this.state.selectedCategory = null;

            this.renderMenu();
            this.showView('order');
        } catch (e) {
            Swal.fire('Errore', "Errore nell'inizializzazione della comanda", 'error');
        }
    }

    /**
     * Mostra lo storico completo di tutte le comande del tavolo corrente.
     * Include ordini già inviati/archiviati, non solo il draft corrente.
     */
    async viewTableHistory() {
        if (!this.state.currentTableId) return;

        try {
            const allOrders = await API.get(`/api/cameriere/tables/${this.state.currentTableId}/orders/history`);

            // Escludi le BOZZE — non sono comande reali, solo la comanda corrente in composizione
            const orders = allOrders.filter(o => o.status !== 'DRAFT');

            // Ordina per numero comanda (prima → ultima)
            orders.sort((a, b) => (a.commandaNumber || 0) - (b.commandaNumber || 0));

            if (orders.length === 0) {
                Swal.fire('Storico Tavolo', 'Nessuna comanda ancora inviata per questo tavolo.', 'info');
                return;
            }

            const statusLabel = {
                SENT: { text: '📨 Inviata', bg: '#FEF3C7', color: '#D97706' },
                IN_PREPARATION: { text: '👨‍🍳 In Preparazione', bg: '#DBEAFE', color: '#1D4ED8' },
                DONE: { text: '✅ Pronta', bg: '#D1FAE5', color: '#059669' },
                ARCHIVED: { text: 'Archiviata', bg: '#F3F4F6', color: '#6B7280' }
            };

            let html = '<div style="text-align:left; max-height:70vh; overflow-y:auto;">';

            orders.forEach(order => {
                const sl = statusLabel[order.status] || { text: order.status, bg: '#F3F4F6', color: '#374151' };
                const timeStr = new Date(order.sentAt || order.createdAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
                const num = order.commandaNumber || '?';

                html += `
                    <div style="border:1px solid #E5E7EB; border-radius:8px; padding:12px; margin-bottom:12px;">
                        <div style="display:flex; justify-content:space-between; align-items:center; margin-bottom:8px;">
                            <span style="font-weight:700; font-size:1rem;">Comanda #${num}</span>
                            <span style="background:${sl.bg}; color:${sl.color}; font-size:0.75rem; padding:2px 8px; border-radius:10px; font-weight:600;">${sl.text}</span>
                        </div>
                        <div style="font-size:0.8rem; color:#6B7280; margin-bottom:8px;">⏰ ${timeStr}${order.generalNotes ? ` · 📝 ${order.generalNotes}` : ''}</div>
                `;

                if (order.items.length === 0) {
                    html += `<div style="font-size:0.85rem; color:#9CA3AF; font-style:italic;">Nessun piatto</div>`;
                } else {
                    order.items.forEach(item => {
                        html += `
                            <div style="padding:5px 0; border-bottom:1px solid #F3F4F6; font-size:0.875rem;">
                                <strong>${item.quantity}x</strong> ${item.menuItemNameSnapshot}
                                ${item.note ? `<span style="color:#DC2626; background:#FEE2E2; padding:1px 5px; border-radius:4px; font-size:0.75rem; margin-left:4px;">📌 ${item.note}</span>` : ''}
                            </div>
                        `;
                    });
                }

                html += `</div>`;
            });

            html += '</div>';

            Swal.fire({
                title: `📋 Storico Tavolo ${this.state.currentTableData?.tableNumber || ''}`,
                html: html,
                width: '90%',
                confirmButtonText: 'Chiudi'
            });

        } catch (e) {
            Swal.fire('Errore', 'Impossibile caricare lo storico', 'error');
        }
    }

    async loadArchivedOrders() {
        try {
            const archived = await API.get('/api/cameriere/orders/archived');
            const aJson = JSON.stringify(archived);
            if (this.lastAJson !== aJson) {
                this.state.archivedOrders = archived;
                this.lastAJson = aJson;
                this.renderArchivedOrders();
            }
        } catch (e) { }
    }

    renderArchivedOrders() {
        const archived = this.state.archivedOrders || [];
        archived.sort((a, b) => new Date(b.updatedAt || b.createdAt) - new Date(a.updatedAt || a.createdAt));
        const container = document.getElementById('archived-list');
        container.innerHTML = '';

        if (archived.length === 0) {
            container.innerHTML = `<p class="empty-state">Nessuna comanda archiviata nelle ultime 12h.</p>`;
            return;
        }

        archived.forEach(order => {
            const div = document.createElement('div');
            div.className = 'table-card';
            div.innerHTML = `
                    <div class="table-card-top">
                        <span class="table-num">Tavolo ${order.tableSession ? order.tableSession.tableNumber : '?'}</span>
                        <span class="table-status" style="background:#E5E7EB; color:#374151;">Archiviata</span>
                    </div>
                    <div style="color:var(--text-secondary); font-size:0.875rem;">
                        Chiusa: ${new Date(order.updatedAt || order.createdAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}
                    </div>
                    <div style="margin-top:0.5rem; font-size:0.875rem; font-weight:600;">
                        Totale Piatti: ${order.items.length}
                    </div>
                `;
            div.addEventListener('click', () => this.viewHistoricalOrder(order));
            container.appendChild(div);
        });
    }

    viewHistoricalOrder(order) {
        let html = '<div style="text-align:left;">';
        order.items.forEach(i => {
            html += `
             <div style="border-bottom:1px solid #eee; padding-bottom:12px; margin-bottom:12px;">
               <b>${i.quantity}x</b> ${i.menuItemNameSnapshot}
               ${i.menuItemDescriptionSnapshot ? `<div style="font-size:0.75rem; color:#6B7280; margin-top:2px;">${i.menuItemDescriptionSnapshot}</div>` : ''}
               ${i.note ? `<br><small style="color:#DC2626; background:#FEE2E2; padding:2px 6px; border-radius:4px;">📌 ${i.note}</small>` : ''}
             </div>
           `;
        });
        html += '</div>';

        Swal.fire({
            title: `Storico Tavolo ${order.tableSession.tableNumber}`,
            html: html,
            confirmButtonText: 'Chiudi'
        });
    }

    updateOrderStatusUI() {
        const el = document.getElementById('current-order-status');
        if (!el) return;

        const statusMap = {
            DRAFT: { label: 'BOZZA', bg: '#E5E7EB', color: '#374151' },
            SENT: { label: 'INVIATA', bg: '#FEF3C7', color: '#D97706' },
            IN_PREPARATION: { label: 'IN PREPARAZIONE', bg: '#DBEAFE', color: '#1D4ED8' },
            DONE: { label: 'PRONTA ✅', bg: '#D1FAE5', color: '#059669' },
            ARCHIVED: { label: 'ARCHIVIATA', bg: '#F3F4F6', color: '#6B7280' }
        };

        const s = statusMap[this.state.currentOrderStatus] || { label: this.state.currentOrderStatus, bg: '#F3F4F6', color: '#374151' };
        el.innerText = s.label;
        el.style.background = s.bg;
        el.style.color = s.color;

        const btnSend = document.getElementById('btn-send-order');
        btnSend.style.display = this.state.currentOrderStatus !== 'DRAFT' ? 'none' : 'block';
    }

    // --- Menu Rendering --- //

    renderCategories() {
        const container = document.getElementById('menu-categories');
        container.innerHTML = `<button class="btn btn-primary" data-id="all" style="padding:0.4rem 1rem;">Tutti</button>`;
        this.state.categories.forEach(c => {
            const b = document.createElement('button');
            b.className = 'btn';
            b.style.background = 'var(--surface-color)';
            b.style.border = '1px solid #E5E7EB';
            b.style.padding = '0.4rem 1rem';
            b.innerText = c.name;
            b.dataset.name = c.name;
            b.addEventListener('click', () => {
                this.state.selectedCategory = c.name;
                Array.from(container.children).forEach(child => {
                    child.className = 'btn';
                    child.style.background = 'var(--surface-color)';
                    child.style.border = '1px solid #E5E7EB';
                    child.style.color = 'var(--text-color)';
                });
                b.className = 'btn btn-primary';
                b.style.background = '';
                b.style.border = '';
                b.style.color = '';
                this.renderMenu();
            });
            container.appendChild(b);
        });
        container.querySelector('[data-id="all"]').addEventListener('click', (e) => {
            this.state.selectedCategory = null;
            Array.from(container.children).forEach(child => {
                child.className = 'btn';
                child.style.background = 'var(--surface-color)';
                child.style.border = '1px solid #E5E7EB';
                child.style.color = 'var(--text-color)';
            });
            e.target.className = 'btn btn-primary';
            e.target.style.background = '';
            e.target.style.border = '';
            e.target.style.color = '';
            this.renderMenu();
        });
    }

    renderMenu() {
        const container = document.getElementById('menu-items-list');

        let filtered = this.state.menuItems;
        if (this.state.selectedCategory) {
            filtered = filtered.filter(i => i.categoryName && i.categoryName === this.state.selectedCategory);
        }
        if (this.state.searchQuery) {
            filtered = filtered.filter(i => i.titolo.toLowerCase().includes(this.state.searchQuery) || (i.descrizione && i.descrizione.toLowerCase().includes(this.state.searchQuery)));
        }

        // Usa DocumentFragment per operazione di DOM insert singola
        const frag = document.createDocumentFragment();

        filtered.forEach(item => {
            if (item.visibile === false || item.available === false) return;

            const div = document.createElement('div');
            div.className = 'menu-item-row';
            div.innerHTML = `
                <div class="menu-info">
                    <h4>${item.titolo}</h4>
                    ${item.descrizione ? `<div style="font-size:0.75rem; color:var(--text-secondary);">${item.descrizione}</div>` : ''}
                    <p class="price">€ ${item.prezzo.toFixed(2)}</p>
                </div>
                <div class="menu-actions">
                    <button class="btn-round add-btn">+</button>
                </div>
            `;
            // Listener separato per non usare document.querySelector
            div.querySelector('.add-btn').addEventListener('click', () => this.openAddItemModal(item));
            frag.appendChild(div);
        });

        // requestAnimationFrame previene layout thrashing sincrono
        requestAnimationFrame(() => {
            container.innerHTML = '';
            container.appendChild(frag);
        });
    }

    // --- Modals & Adds --- //

    openAddItemModal(menuItem) {
        document.getElementById('modal-item-name').innerText = menuItem.titolo;
        document.getElementById('modal-qty').innerText = '1';
        document.getElementById('modal-note').value = '';
        document.getElementById('modal-price').innerText = `(€${menuItem.prezzo.toFixed(2)})`;
        this.currentAddingItem = menuItem;
        document.getElementById('modal-add-item').classList.add('active');
    }

    async confirmAddItem() {
        if (!this.currentAddingItem) return;
        const item = this.currentAddingItem;
        const qty = parseInt(document.getElementById('modal-qty').innerText);
        const note = document.getElementById('modal-note').value.trim();

        document.getElementById('modal-add-item').classList.remove('active');

        // Optimistic UI Update: aggiorno subito per reattività istantanea
        const tempId = 'temp_' + Date.now();
        const tempItem = {
            id: tempId,
            menuItemNameSnapshot: item.titolo,
            menuItemDescriptionSnapshot: item.descrizione,
            quantity: qty,
            note: note !== '' ? note : null,
            status: 'DRAFT'
        };

        this.state.cartOpenItems.push(tempItem);
        this.updateBadge();
        // Toast non bloccante
        Swal.fire({ toast: true, position: 'top', icon: 'success', title: 'Aggiunto!', showConfirmButton: false, timer: 800 });

        // Background call
        try {
            await API.post(`/api/cameriere/orders/${this.state.currentOrderId}/items`, {
                menuItemId: item.id,
                quantity: qty,
                note: note !== '' ? note : null
            });

            // Sincronizza lo stato finale ufficiale server-side
            const draftOrder = await API.get(`/api/cameriere/tables/${this.state.currentTableId}/orders/current`);
            this.state.cartOpenItems = draftOrder.items || [];
            this.updateBadge();
        } catch (e) {
            // Rollback in caso di errore
            this.state.cartOpenItems = this.state.cartOpenItems.filter(i => i.id !== tempId);
            this.updateBadge();
            Swal.fire({ toast: true, position: 'top', icon: 'error', title: 'Errore aggiunta', text: 'Riprova', showConfirmButton: false, timer: 2000 });
        }
    }

    updateBadge() {
        const total = this.state.cartOpenItems.reduce((acc, i) => acc + i.quantity, 0);
        document.getElementById('order-badge').innerText = `${total} Elementi`;
    }

    async viewCart() {
        if (this.state.cartOpenItems.length === 0) {
            Swal.fire('Comanda Vuota', 'Non hai aggiunto nessun piatto', 'info');
            return;
        }

        let html = '<div style="text-align:left;">';
        this.state.cartOpenItems.forEach(i => {
            let statusHtml = '';
            if (this.state.currentOrderStatus !== 'DRAFT') {
                const col = i.status === 'DONE' ? '#059669' : (i.status === 'IN_PREPARATION' ? '#1D4ED8' : '#6B7280');
                statusHtml = `<span style="font-size: 0.75rem; font-weight: 600; color: ${col}; display:block;">Stato: ${i.status}</span>`;
            }

            html += `
             <div style="border-bottom:1px solid #eee; padding-bottom:12px; margin-bottom:12px; display:flex; justify-content:space-between; align-items:center;">
               <div>
                   <b>${i.quantity}x</b> ${i.menuItemNameSnapshot}
                   ${i.menuItemDescriptionSnapshot ? `<div style="font-size:0.75rem; color:#6B7280; margin-top:2px;">${i.menuItemDescriptionSnapshot}</div>` : ''}
                   ${i.note ? `<br><small style="color:#DC2626; background:#FEE2E2; padding:2px 6px; border-radius:4px;">📌 ${i.note}</small>` : ''}
                   ${statusHtml}
               </div>
               <button class="btn-round" style="width:36px; height:36px; background:#FEE2E2; color:#DC2626; font-size:1rem; border:none;" onclick="window.app.removeItem(${i.id})">✕</button>
             </div>
           `;
        });
        html += '</div>';

        await Swal.fire({
            title: 'Riepilogo',
            html: html,
            showConfirmButton: false,
            showCancelButton: true,
            cancelButtonText: 'Chiudi (Continua a ordinare)'
        });
    }

    async removeItem(itemId) {
        try {
            await API.delete(`/api/cameriere/orders/${this.state.currentOrderId}/items/${itemId}`);
            const draftOrder = await API.get(`/api/cameriere/tables/${this.state.currentTableId}/orders/current`);
            this.state.cartOpenItems = draftOrder.items;
            this.updateBadge();
            Swal.close();
            this.viewCart();
        } catch (e) {
            Swal.fire('Errore', 'Impossibile rimuovere il piatto', 'error');
        }
    }

    async sendOrder() {
        if (this.state.cartOpenItems.length === 0) {
            Swal.fire('Attenzione', 'Impossibile inviare una comanda vuota', 'warning');
            return;
        }

        const res = await Swal.fire({
            title: 'Invia in Cucina?',
            text: 'Sei sicuro di inviare questa comanda?',
            icon: 'question',
            showCancelButton: true,
            confirmButtonColor: '#10B981',
            confirmButtonText: 'Sì, Invia',
            cancelButtonText: 'Annulla'
        });

        if (res.isConfirmed) {
            try {
                Swal.fire({ title: 'Invio...', allowOutsideClick: false, didOpen: () => Swal.showLoading() });
                await API.post(`/api/cameriere/orders/${this.state.currentOrderId}/send`);

                await Swal.fire({ icon: 'success', title: 'Inviato!', timer: 1500, showConfirmButton: false });

                await this.fetchTables();
                this.showView('tables');
            } catch (e) {
                Swal.fire('Errore', "Impossibile inviare l'ordine", 'error');
            }
        }
    }
}

// Boot
window.onload = () => {
    window.app = new CameriereApp();
};
