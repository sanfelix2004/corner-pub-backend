import Auth from '../core/auth.js';
import API from '../core/api.js';
import WSClient from '../core/ws.js';

class CucinaApp {
    constructor() {
        this.orders = [];
        this.wsClient = null;
        this.currentTab = 'active';

        this.init();
    }

    async init() {
        if (!Auth.isAuthenticated()) {
            this.showView('login');
        } else {
            await this.loadDashboard();
            this.showView('dashboard');
            this.connectWebSocket();
            this.startPolling();
        }
        this.bindEvents();
    }

    startPolling() {
        setInterval(() => {
            if (Auth.isAuthenticated()) {
                this.loadDashboard();
            }
        }, 3000); // 3 seconds is very close to real-time for human perception
    }

    bindEvents() {
        document.getElementById('login-form').addEventListener('submit', async (e) => {
            e.preventDefault();
            const u = document.getElementById('username').value;
            const p = document.getElementById('password').value;
            if (await Auth.login(u, p)) {
                await this.loadDashboard();
                this.showView('dashboard');
                this.connectWebSocket();
            } else {
                Swal.fire({ icon: 'error', title: 'Errore', text: 'Credenziali non valide' });
            }
        });

        document.getElementById('btn-logout').addEventListener('click', () => {
            if (this.wsClient) this.wsClient.disconnect();
            Auth.logout();
            window.location.reload();
        });

        document.getElementById('btn-tab-active')?.addEventListener('click', () => {
            this.currentTab = 'active';
            document.getElementById('btn-tab-active').className = 'btn btn-primary';
            document.getElementById('btn-tab-active').style.background = '';
            document.getElementById('btn-tab-active').style.color = '';

            document.getElementById('btn-tab-archived').className = 'btn';
            document.getElementById('btn-tab-archived').style.background = 'var(--surface-color)';
            document.getElementById('btn-tab-archived').style.border = '1px solid #ccc';
            document.getElementById('btn-tab-archived').style.color = '#333';

            this.loadDashboard();
        });

        document.getElementById('btn-tab-archived')?.addEventListener('click', () => {
            this.currentTab = 'archived';
            document.getElementById('btn-tab-archived').className = 'btn btn-primary';
            document.getElementById('btn-tab-archived').style.background = '';
            document.getElementById('btn-tab-archived').style.color = '';

            document.getElementById('btn-tab-active').className = 'btn';
            document.getElementById('btn-tab-active').style.background = 'var(--surface-color)';
            document.getElementById('btn-tab-active').style.border = '1px solid #ccc';
            document.getElementById('btn-tab-active').style.color = '#333';

            this.loadDashboard();
        });
    }

    showView(viewName) {
        document.getElementById('view-login').style.display = 'none';
        document.getElementById('view-dashboard').style.display = 'none';
        document.getElementById(`view-${viewName}`).style.display = viewName === 'login' ? 'flex' : 'block';
    }

    connectWebSocket() {
        const endpoint = '/ws-orders';

        this.wsClient = new WSClient(endpoint, {
            '/topic/kitchen/orders': (msg) => this.handleKitchenEvent(msg)
        });

        this.wsClient.connect().then(() => {
            document.getElementById('ws-indicator').classList.add('connected');
            document.getElementById('ws-label').innerText = 'Live';
        }).catch(() => {
            document.getElementById('ws-indicator').classList.remove('connected');
            document.getElementById('ws-label').innerText = 'Reconnecting...';
        });
    }

    // Called automatically by WS Topic Listener when a new command is sent or updated via Waiter
    async handleKitchenEvent(messagePayload) {
        // Force an immediate refetch of active orders
        // This is extremely fast and ensures 100% data consistency
        console.log("Real-time update received!");
        await this.loadDashboard();

        // Optional: flash screen border or play subtle sound
    }

    async loadDashboard() {
        try {
            let newOrders = [];
            if (this.currentTab === 'archived') {
                newOrders = await API.get('/api/cucina/orders/archived');
                // Sort by updated descending (Newest completed first)
                newOrders.sort((a, b) => new Date(b.updatedAt || b.createdAt) - new Date(a.updatedAt || a.createdAt));
            } else {
                newOrders = await API.get('/api/cucina/orders/active');
                // Filter out DRAFT orders, kitchen only cares about SENT and above
                newOrders = newOrders.filter(o => o.status !== 'DRAFT');
                // Sort by createdAt ascending (Oldest first)
                newOrders.sort((a, b) => new Date(a.createdAt) - new Date(b.createdAt));
            }

            const ordersJson = JSON.stringify(newOrders);
            if (this.lastOrdersJson !== ordersJson) {
                this.orders = newOrders;
                this.lastOrdersJson = ordersJson;
                this.renderBoard();
            }
        } catch (e) {
            console.error("Failed to load dashboard data", e);
        }
    }

    renderBoard() {
        const board = document.getElementById('kanban-board');
        const emptyState = document.getElementById('empty-state');
        board.innerHTML = '';

        if (this.orders.length === 0) {
            emptyState.style.display = 'block';
            return;
        }

        emptyState.style.display = 'none';

        this.orders.forEach(order => {
            const ticket = document.createElement('div');
            ticket.className = 'ticket new-ticket'; // slide-in anim class
            ticket.dataset.status = order.status;
            ticket.dataset.id = order.id;

            // Header
            let timeStr = new Date(order.sentAt || order.createdAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });

            let html = `
                <div class="ticket-header">
                    <span class="ticket-table">Tav. ${order.tableSession?.tableNumber || '?'}</span>
                    <span class="ticket-time">${timeStr}</span>
                </div>
            `;

            if (order.generalNotes) {
                html += `<div class="ticket-notes">GEN: ${order.generalNotes}</div>`;
            }

            // Items
            html += `<div class="ticket-items">`;
            order.items.forEach(item => {
                html += `
                <div class="item-row">
                    <div class="item-main">
                        <span class="item-qty">${item.quantity}x</span>
                        <div style="flex-grow:1; display:flex; flex-direction:column;">
                            <span>${item.menuItemNameSnapshot}</span>
                            ${item.menuItemDescriptionSnapshot ? `<span style="font-size: 0.75rem; color: #6B7280; margin-top: 2px;">${item.menuItemDescriptionSnapshot}</span>` : ''}
                        </div>
                    </div>
                    ${item.note ? `<div class="item-note">${item.note}</div>` : ''}
                </div>
                `;
            });
            html += `</div>`;

            // Actions mapping
            html += `<div class="ticket-actions">`;
            if (order.status === 'SENT' || order.status === 'TO_PREPARE') {
                html += `<button class="action-btn btn-prepare" onclick="window.app.changeOrderStatus(${order.id}, 'IN_PREPARATION')">Inizia Preparazione</button>`;
            } else if (order.status === 'IN_PREPARATION') {
                html += `<button class="action-btn btn-done" onclick="window.app.changeOrderStatus(${order.id}, 'DONE')">Segna come Finito</button>`;
            } else if (order.status === 'DONE') {
                html += `<button class="action-btn btn-archive" onclick="window.app.archiveOrder(${order.id})">Archivia Comanda ✕</button>`;
            } else if (order.status === 'ARCHIVED') {
                html += `<button class="action-btn" style="background:#E5E7EB; color:#6B7280; pointer-events:none;">Archiviata</button>`;
            }
            html += `</div>`;

            ticket.innerHTML = html;
            board.appendChild(ticket);
        });
    }

    async changeOrderStatus(orderId, newStatus) {
        try {
            await API.patch(`/api/cucina/orders/${orderId}/status`, { status: newStatus });
            // The WS will rebroadcast this to all connected iPads anyway, 
            // but we can optimistic-reload our own board immediately
            this.loadDashboard();
        } catch (e) {
            Swal.fire('Errore', 'Impossibile aggiornare stato', 'error');
        }
    }

    async archiveOrder(orderId) {
        try {
            await API.post(`/api/cucina/orders/${orderId}/archive`, {});
            this.loadDashboard();
        } catch (e) {
            Swal.fire('Errore', 'Impossibile archiviare', 'error');
        }
    }
}

window.onload = () => {
    // Expose app to global scope so inline onclick handles work (simple approach for vanilla)
    window.app = new CucinaApp();
};
