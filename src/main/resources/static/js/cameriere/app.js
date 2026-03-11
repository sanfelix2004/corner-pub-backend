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
        setInterval(() => {
            if (Auth.isAuthenticated()) {
                this.pollUpdates();
            }
        }, 3000);
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
            await this.fetchTables(); // Refresh
            this.showView('tables');
        });

        // Menu Search & Filter
        document.getElementById('menu-search').addEventListener('input', (e) => {
            this.state.searchQuery = e.target.value.toLowerCase();
            this.renderMenu();
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
        this.wsClient.connect().catch(e => console.error("WS Cameriere Error", e));
    }

    async handleKitchenEvent(msg) {
        this.pollUpdates();
    }

    async pollUpdates() {
        if (document.getElementById('view-tables').style.display !== 'none') {
            try {
                // If in active tables view
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
                }
                // If in archived orders view
                else if (document.getElementById('archived-list').style.display !== 'none') {
                    this.loadArchivedOrders();
                }
            } catch (e) { }
        }

        // If inside an Order details view
        if (this.state.currentTableId && document.getElementById('view-order').style.display !== 'none') {
            try {
                const draftOrder = await API.get(`/api/cameriere/tables/${this.state.currentTableId}/orders/current`);
                const dJson = JSON.stringify(draftOrder);

                if (this.lastDraftJson !== dJson) {
                    if (this.state.currentOrderStatus !== draftOrder.status) {
                        if (draftOrder.status !== 'DRAFT') {
                            Swal.fire({ toast: true, position: 'top', showConfirmButton: false, timer: 3000, icon: 'info', title: `Stato Ordine: ${draftOrder.status}` });
                        }
                    }
                    this.state.cartOpenItems = draftOrder.items || [];
                    this.state.currentOrderStatus = draftOrder.status;
                    this.lastDraftJson = dJson;
                    this.updateBadge();
                    this.updateOrderStatusUI();

                    if (Swal.isVisible() && Swal.getTitle() && Swal.getTitle().innerText === 'Riepilogo') {
                        this.viewCart(); // Re-render open cart if it's currently showing
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

            // Render basic UI that doesn't change often
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
                let badgeColor = order.status === 'DONE' ? '#059669' : (order.status === 'IN_PREPARATION' ? '#1D4ED8' : '#D97706');
                let badgeBg = order.status === 'DONE' ? '#D1FAE5' : (order.status === 'IN_PREPARATION' ? '#DBEAFE' : '#FEF3C7');
                orderStatusHtml = `<div style="margin-top:0.5rem; font-size:0.75rem; background:${badgeBg}; color:${badgeColor}; padding:2px 8px; border-radius:12px; display:inline-block; font-weight:600;">Ordine: ${order.status}</div>`;
            }

            const div = document.createElement('div');
            div.className = 'table-card';
            div.innerHTML = `
                <div class="table-card-top">
                    <span class="table-num">Tavolo ${t.tableNumber}</span>
                    <span class="table-status status-${t.status.toLowerCase()}">${t.status}</span>
                </div>
                <div style="color:var(--text-secondary); font-size:0.875rem;">
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
                <input id="swal-input1" class="swal2-input" placeholder="Numero Tavolo (es. 12)">
                <input id="swal-input2" class="swal2-input" placeholder="Note (opzionale)">
            `,
            focusConfirm: false,
            showCancelButton: true,
            confirmButtonText: 'Apri',
            cancelButtonText: 'Annulla',
            preConfirm: () => {
                return [
                    document.getElementById('swal-input1').value,
                    document.getElementById('swal-input2').value
                ]
            }
        });

        if (formValues && formValues[0]) {
            try {
                const newTable = await API.post('/api/cameriere/tables', {
                    tableNumber: formValues[0],
                    generalNotes: formValues[1]
                });
                await this.fetchTables();
                // Optionally auto-open table
                this.openTableSession(newTable);
            } catch (e) {
                Swal.fire('Errore', "Impossibile creare il tavolo", 'error');
            }
        }
    }

    async openTableSession(table) {
        this.state.currentTableId = table.id;
        document.getElementById('current-table-display').innerText = table.tableNumber;

        try {
            // Fetch or create draft order for this table
            let draftOrder = await API.post(`/api/cameriere/tables/${table.id}/orders`, {});
            this.state.currentOrderId = draftOrder.id;
            this.state.cartOpenItems = draftOrder.items || [];
            this.state.currentOrderStatus = draftOrder.status;
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
        // Sort newest first
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
               ${i.note ? `<br><small style="color:#DC2626; background:#FEE2E2; padding:2px 6px; border-radius:4px;">Nota: ${i.note}</small>` : ''}
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
        el.innerText = this.state.currentOrderStatus === 'DRAFT' ? 'BOZZA' : this.state.currentOrderStatus;
        if (this.state.currentOrderStatus === 'DRAFT') {
            el.style.background = '#E5E7EB'; el.style.color = '#374151';
        } else if (this.state.currentOrderStatus === 'SENT') {
            el.style.background = '#FEF3C7'; el.style.color = '#D97706';
        } else if (this.state.currentOrderStatus === 'IN_PREPARATION') {
            el.style.background = '#DBEAFE'; el.style.color = '#1D4ED8';
        } else if (this.state.currentOrderStatus === 'DONE') {
            el.style.background = '#D1FAE5'; el.style.color = '#059669';
        }
        const btnSend = document.getElementById('btn-send-order');
        if (this.state.currentOrderStatus !== 'DRAFT') {
            btnSend.style.display = 'none';
        } else {
            btnSend.style.display = 'block';
        }
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
            b.addEventListener('click', (e) => {
                // visual active state feedback to be added
                this.state.selectedCategory = c.name;

                // reset all buttons visually
                Array.from(container.children).forEach(child => {
                    child.className = 'btn';
                    child.style.background = 'var(--surface-color)';
                    child.style.border = '1px solid #E5E7EB';
                    child.style.color = 'var(--text-color)';
                });
                // highlight active
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

            // reset all buttons visually
            Array.from(container.children).forEach(child => {
                child.className = 'btn';
                child.style.background = 'var(--surface-color)';
                child.style.border = '1px solid #E5E7EB';
                child.style.color = 'var(--text-color)';
            });
            const allBtn = e.target;
            allBtn.className = 'btn btn-primary';
            allBtn.style.background = '';
            allBtn.style.border = '';
            allBtn.style.color = '';

            this.renderMenu();
        });
    }

    renderMenu() {
        const container = document.getElementById('menu-items-list');
        container.innerHTML = '';

        let filtered = this.state.menuItems;
        if (this.state.selectedCategory) {
            filtered = filtered.filter(i => i.categoryName && i.categoryName === this.state.selectedCategory);
        }
        if (this.state.searchQuery) {
            filtered = filtered.filter(i => i.titolo.toLowerCase().includes(this.state.searchQuery) || (i.descrizione && i.descrizione.toLowerCase().includes(this.state.searchQuery)));
        }

        filtered.forEach(item => {
            // Non renderizzare se non attivo/visibile (adatta ai campi reali dell'entity JSON)
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

            div.querySelector('.add-btn').addEventListener('click', () => this.openAddItemModal(item));
            container.appendChild(div);
        });
    }

    // --- Modals & Adds --- //

    openAddItemModal(menuItem) {
        document.getElementById('modal-item-name').innerText = menuItem.titolo;
        document.getElementById('modal-qty').innerText = '1';
        document.getElementById('modal-note').value = '';
        document.getElementById('modal-price').innerText = `(€${menuItem.prezzo.toFixed(2)})`;

        // Store temp reference
        this.currentAddingItem = menuItem;
        document.getElementById('modal-add-item').classList.add('active');
    }

    async confirmAddItem() {
        const item = this.currentAddingItem;
        const qty = parseInt(document.getElementById('modal-qty').innerText);
        const note = document.getElementById('modal-note').value.trim();

        document.getElementById('modal-add-item').classList.remove('active');

        // API Call
        try {
            Swal.fire({ title: 'Aggiunta...', allowOutsideClick: false, didOpen: () => Swal.showLoading() });

            const addedLine = await API.post(`/api/cameriere/orders/${this.state.currentOrderId}/items`, {
                menuItemId: item.id,
                quantity: qty,
                note: note !== '' ? note : null
            });

            Swal.close();

            // Just update local cart array logic to save bandwidth
            // (In a real app, refetch whole order to be safe)
            const draftOrder = await API.get(`/api/cameriere/tables/${this.state.currentTableId}/orders/current`);
            this.state.cartOpenItems = draftOrder.items;
            this.updateBadge();

            if (this.state.currentOrderStatus !== 'DRAFT') {
                Swal.fire({ toast: true, position: 'top', icon: 'success', title: 'Inviato in cucina!', showConfirmButton: false, timer: 1500 });
            } else {
                Swal.fire({ toast: true, position: 'top', icon: 'success', title: 'Aggiunto!', showConfirmButton: false, timer: 800 });
            }
        } catch (e) {
            Swal.fire('Errore', 'Impossibile aggiungere il piatto', 'error');
        }
    }

    updateBadge() {
        const total = this.state.cartOpenItems.reduce((acc, i) => acc + i.quantity, 0);
        document.getElementById('order-badge').innerText = `${total} Elementi`;
    }

    async viewCart() {
        if (this.state.cartOpenItems.length === 0) {
            Swal.fire('Comanda Vuota', "Non hai aggiunto nessun piatto", 'info');
            return;
        }

        let html = '<div style="text-align:left;">';
        this.state.cartOpenItems.forEach(i => {
            let statusHtml = '';
            if (this.state.currentOrderStatus !== 'DRAFT') {
                let col = i.status === 'DONE' ? '#059669' : (i.status === 'IN_PREPARATION' ? '#1D4ED8' : '#6B7280');
                statusHtml = `<span style="font-size: 0.75rem; font-weight: 600; color: ${col}; display:block;">Stato: ${i.status}</span>`;
            }

            html += `
             <div style="border-bottom:1px solid #eee; padding-bottom:12px; margin-bottom:12px; display:flex; justify-content:space-between; align-items:center;">
               <div>
                   <b>${i.quantity}x</b> ${i.menuItemNameSnapshot}
                   ${i.menuItemDescriptionSnapshot ? `<div style="font-size:0.75rem; color:#6B7280; margin-top:2px;">${i.menuItemDescriptionSnapshot}</div>` : ''}
                   ${i.note ? `<br><small style="color:#DC2626; background:#FEE2E2; padding:2px 6px; border-radius:4px;">Nota: ${i.note}</small>` : ''}
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
            Swal.fire('Errore', "Impossibile rimuovere il piatto", 'error');
        }
    }

    async sendOrder() {
        if (this.state.cartOpenItems.length === 0) {
            Swal.fire('Attenzione', "Impossibile inviare una comanda vuota", 'warning');
            return;
        }

        const res = await Swal.fire({
            title: 'Invia in Cucina?',
            text: "Sei sicuro di inviare questa comanda?",
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

                await Swal.fire({
                    icon: 'success',
                    title: 'Inviato!',
                    timer: 1500,
                    showConfirmButton: false
                });

                // Ritorna ai tavoli
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
