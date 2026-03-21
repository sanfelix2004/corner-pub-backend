import Auth from '../core/auth.js';
import API from '../core/api.js';
import WSClient from '../core/ws.js';
import { DING_B64 } from './audioData.js';

class CucinaApp {
    constructor() {
        this.orders = [];
        this.wsClient = null;
        this.currentTab = 'active';
        // Traccia gli ID comande già notificate per evitare duplicati
        this.notifiedOrderIds = new Set();

        // Elemento audio precaricato da Base64 (zero delay di rete)
        this.audioElement = new Audio(DING_B64);
        this.audioElement.preload = 'auto';
        this.audioUnlocked = false;
        this.lastSoundPlayedAt = 0;

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
            this.ensureToastContainer();
            // Richiedi permesso per le notifiche native OS subito dopo login
            this.requestNotificationPermission();
            // Inizializza logic per sbloccare l'audio su iOS Safari
            this.requireAudioUnlock();
        }
        this.bindEvents();
    }

    startPolling() {
        // Fallback polling intelligente: lavora SOLO se il WebSocket muore.
        const tick = async () => {
            if (Auth.isAuthenticated()) {
                if (!this.wsClient || !this.wsClient.isConnected) {
                    await this.loadDashboard();
                }
            }
            this._pollTimer = setTimeout(tick, 5000);
        };
        this._pollTimer = setTimeout(tick, 5000);
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
                this.ensureToastContainer();
                this.requestNotificationPermission();
                this.requireAudioUnlock();
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

        this.wsClient.onStatusChange = (connected) => {
            const ind = document.getElementById('ws-indicator');
            const lbl = document.getElementById('ws-label');
            if (connected) {
                ind.classList.add('connected');
                lbl.innerText = 'Live';
                // Catch-up: Resync stato innescato dalla riconnessione WS
                this.loadDashboard();
            } else {
                ind.classList.remove('connected');
                lbl.innerText = 'Reconnecting...';
            }
        };

        this.wsClient.connect().catch(e => console.error('WS Cucina Error', e));
    }

    /**
     * Gestisce gli eventi WebSocket con payload strutturato.
     */
    async handleKitchenEvent(rawMsg) {
        let payload = null;
        try {
            payload = JSON.parse(rawMsg);
        } catch (e) {
            return;
        }

        const { eventType, orderId, tableNumber, copeRti, order } = payload || {};

        // Se l'ordinazione ha dati del tavolo ma l'oggetto order.tableSession è monco/assente, ricostruiamolo
        if (order && !order.tableSession && tableNumber) {
            order.tableSession = { tableNumber, copeRti };
        }

        if (eventType === 'TABLE_CLOSED') {
            if (this.currentTab === 'active') {
                this.orders = this.orders.filter(o => String(o.tableSession?.tableNumber) !== String(tableNumber));
            } else {
                this.loadDashboard(); // reload to get the newly archived items
            }
            this.renderBoard();
            return;
        }

        if (order) {
            // Local State Patching (Zero-Fetch Update)
            if (eventType === 'ARCHIVED') {
                if (this.currentTab === 'active') {
                    this.orders = this.orders.filter(o => o.id !== order.id);
                } else {
                    const idx = this.orders.findIndex(o => o.id === order.id);
                    if (idx >= 0) this.orders[idx] = order;
                    else this.orders.push(order);
                    this.orders.sort((a, b) => new Date(b.updatedAt).getTime() - new Date(a.updatedAt).getTime());
                }
            } else {
                if (this.currentTab === 'active') {
                    const idx = this.orders.findIndex(o => o.id === order.id);
                    if (idx >= 0) this.orders[idx] = order;
                    else this.orders.push(order);
                    this.orders.sort((a, b) => new Date(a.createdAt).getTime() - new Date(b.createdAt).getTime());
                } else {
                    this.orders = this.orders.filter(o => o.id !== order.id);
                }
            }
            this.renderBoard();
        } else {
            // Fallback per vecchi payload senza oggetto order integrato
            clearTimeout(this._wsDebounceTimer);
            this._wsDebounceTimer = setTimeout(async () => {
                await this.loadDashboard();
            }, 50);
        }

        // Notifica sonora + toast SOLO su nuove comande non ancora notificate
        if (eventType === 'NEW_ORDER' && orderId && !this.notifiedOrderIds.has(orderId)) {
            this.notifiedOrderIds.add(orderId);
            this.sendNativeNotification(tableNumber, copeRti); // Notifica OS nativa
            this.playNotificationSound();                       // Beep audio (fallback/rinforzo)
            this.showNewOrderToast(tableNumber, copeRti);       // Toast in-app
        }
    }

    /**
     * Richiede il permesso per le notifiche native del sistema operativo.
     * Viene chiamata una sola volta al login/init.
     * Su Mac/iPad compare il popup di sistema "Vuoi ricevere notifiche da localhost?"
     */
    requestNotificationPermission() {
        if (!('Notification' in window)) return;
        if (Notification.permission === 'default') {
            Notification.requestPermission();
        }
    }

    /**
     * Invia una notifica nativa OS-level (compare sopra il browser, anche da tab non in focus).
     * Fallback: se il permesso non è concesso, usa solo il toast in-app.
     */
    sendNativeNotification(tableNumber, copeRti) {
        if (!('Notification' in window) || Notification.permission !== 'granted') return;

        const copeRtiTxt = copeRti ? ` · ${copeRti} coperti` : '';
        const n = new Notification('🔔 Nuova Comanda', {
            body: `Tavolo ${tableNumber || '?'}${copeRtiTxt}`,
            icon: '/favicon.ico',
            tag: `kitchen-order-${Date.now()}`,   // tag unico = notifica non si sovrascrive
            requireInteraction: false,              // scompare da sola dopo qualche secondo
        });

        // Auto-chiudi dopo 6s (su alcuni browser rimane aperta)
        setTimeout(() => n.close(), 6000);
    }

    /**
     * Mostra banner per sbloccare l'audio su iOS Safari.
     * Necessario per aggirare la restrizione sull'Autoplay.
     */
    requireAudioUnlock() {
        if (this.audioUnlocked) return;

        const banner = document.getElementById('audio-unlock-banner');
        if (!banner) return;

        banner.classList.add('visible');

        const unlockHandler = () => {
            // Sblocca l'Elemento Audio con un play() silenzioso gestito dall'utente.
            // Safari validerà l'elemento audio e non bloccherà i successivi play via WebSocket.
            this.audioElement.volume = 0;
            this.audioElement.play().then(() => {
                this.audioElement.pause();
                this.audioElement.currentTime = 0;
                this.audioElement.volume = 1.0;
                this.audioUnlocked = true;
                banner.classList.remove('visible');

                document.removeEventListener('click', unlockHandler);
                document.removeEventListener('touchstart', unlockHandler);
            }).catch(e => {
                console.warn('Audio non sbloccato', e);
            });
        };

        document.addEventListener('click', unlockHandler, { once: true });
        document.addEventListener('touchstart', unlockHandler, { once: true });
    }

    /**
     * Suono di notifica basato sul file base64.
     * Dotato di sistema anti-spam (debounce/throttle) per i picchi.
     */
    playNotificationSound() {
        if (!this.audioUnlocked) return;

        const now = Date.now();
        // Throttle di 1.5 secondi per evitare "storming" di notifiche fastidiose
        // quando arrivano 5 piatti e 5 WebSocket in un decimo di secondo.
        if (now - this.lastSoundPlayedAt < 1500) {
            return;
        }

        this.lastSoundPlayedAt = now;

        try {
            this.audioElement.currentTime = 0;
            this.audioElement.play().catch(e => console.warn('Audio play failed:', e));
        } catch (e) {
            console.warn('Audio exception:', e);
        }
    }

    /**
     * Mostra un banner toast "WhatsApp-like" in alto a destra.
     * Si smonta automaticamente dopo 5 secondi.
     */
    showNewOrderToast(tableNumber, copeRti) {
        const container = document.getElementById('kitchen-toast-container');
        if (!container) return;

        const toast = document.createElement('div');
        toast.className = 'kitchen-toast';
        const copeRtiTxt = copeRti ? ` · <strong>${copeRti}</strong> cop.` : '';
        toast.innerHTML = `
            <div class="kitchen-toast-icon">🔔</div>
            <div class="kitchen-toast-body">
                <div class="kitchen-toast-title">Nuova Comanda</div>
                <div class="kitchen-toast-sub">Tavolo <strong>${tableNumber || '?'}</strong>${copeRtiTxt}</div>
            </div>
            <button class="kitchen-toast-close" onclick="this.closest('.kitchen-toast').remove()">✕</button>
        `;

        container.appendChild(toast);

        // Slide-in animation
        requestAnimationFrame(() => toast.classList.add('visible'));

        // Auto-remove dopo 5s
        setTimeout(() => {
            toast.classList.remove('visible');
            setTimeout(() => toast.remove(), 400);
        }, 5000);
    }

    ensureToastContainer() {
        if (!document.getElementById('kitchen-toast-container')) {
            const el = document.createElement('div');
            el.id = 'kitchen-toast-container';
            document.body.appendChild(el);
        }
    }

    async loadDashboard() {
        try {
            let newOrders = [];
            if (this.currentTab === 'archived') {
                newOrders = await API.get('/api/cucina/orders/archived');
                newOrders.sort((a, b) => new Date(b.updatedAt || b.createdAt) - new Date(a.updatedAt || a.createdAt));
            } else {
                newOrders = await API.get('/api/cucina/orders/active');
                newOrders = newOrders.filter(o => o.status !== 'DRAFT');
                newOrders.sort((a, b) => new Date(a.createdAt) - new Date(b.createdAt));
            }

            const ordersJson = JSON.stringify(newOrders);
            if (this.lastOrdersJson !== ordersJson) {
                this.orders = newOrders;
                this.lastOrdersJson = ordersJson;
                this.renderBoard();
            }
        } catch (e) {
            console.error('Failed to load dashboard data', e);
        }
    }

    renderBoard() {
        const board = document.getElementById('kanban-board');
        const emptyState = document.getElementById('empty-state');

        // Salva la posizione di scroll attuale del chirurgo/device
        const currentScrollY = window.scrollY || document.documentElement.scrollTop;

        board.innerHTML = '';

        if (this.orders.length === 0) {
            emptyState.style.display = 'block';
            return;
        }
        emptyState.style.display = 'none';

        // Raggruppa gli ordini per tavolo (tableSession.id)
        const byTable = new Map();
        for (const order of this.orders) {
            const tid = order.tableSession?.id ?? 'unknown';
            if (!byTable.has(tid)) byTable.set(tid, []);
            byTable.get(tid).push(order);
        }

        const frag = document.createDocumentFragment();

        // Determina la comanda più recente SENT globalmente → verrà evidenziata
        const sentOrders = this.orders.filter(o => o.status === 'SENT' || o.status === 'TO_PREPARE');
        let newestSentId = null;
        if (sentOrders.length > 0) {
            newestSentId = sentOrders.reduce((n, o) =>
                new Date(o.sentAt || o.createdAt) > new Date(n.sentAt || n.createdAt) ? o : n
            ).id;
        }

        for (const [, orders] of byTable) {
            // Ordina le comande del tavolo per numero progressivo
            orders.sort((a, b) => (a.commandaNumber || 0) - (b.commandaNumber || 0));

            const first = orders[0];
            const ts = first.tableSession;
            const copeRti = ts?.copeRti;

            // Stato aggregato del ticket: usa lo stato della comanda più critica
            // (la meno avanzata tra quelle non archiviate)
            const priority = ['SENT', 'TO_PREPARE', 'IN_PREPARATION', 'DONE', 'ARCHIVED'];
            const worstStatus = orders.reduce((worst, o) => {
                return priority.indexOf(o.status) < priority.indexOf(worst) ? o.status : worst;
            }, 'ARCHIVED');

            const ticket = document.createElement('div');
            ticket.className = 'ticket new-ticket';
            ticket.dataset.status = worstStatus;

            let html = `
                <div class="ticket-header">
                    <span class="ticket-table">Tav. ${ts?.tableNumber || '?'}</span>
                    ${copeRti ? `<span class="ticket-coperti">👥 ${copeRti} cop.</span>` : ''}
                    <button class="action-btn" style="width: auto; padding: 2px 8px; font-size: 0.7rem; background: #fee2e2; color: #dc2626; border: 1px solid #fecaca;" 
                        onclick="window.app.archiveTable(${ts?.id})">Archivia Tavolo ✕</button>
                </div>
            `;

            // Sotto-riquadro per ogni comanda
            orders.forEach(order => {
                const isNewest = order.id === newestSentId;
                const timeStr = new Date(order.sentAt || order.createdAt)
                    .toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
                const numLabel = order.commandaNumber ? `Comanda #${order.commandaNumber}` : 'Comanda';

                html += `<div class="comanda-sub${isNewest ? ' comanda-sub-new' : ''}">`;

                // Header sotto-riquadro
                html += `
                    <div class="comanda-sub-header">
                        <span class="comanda-sub-title">${numLabel}</span>
                        <div style="display:flex;align-items:center;gap:6px;">
                            <span class="ticket-time">${timeStr}</span>
                            ${isNewest ? `<span class="ticket-new-badge">🆕 NUOVA</span>` : ''}
                        </div>
                    </div>
                `;

                if (order.generalNotes) {
                    html += `<div class="ticket-notes">📝 ${order.generalNotes}</div>`;
                }

                // Lista piatti
                html += `<div class="ticket-items">`;
                order.items.forEach(item => {
                    html += `
                    <div class="item-row">
                        <div class="item-main">
                            <span class="item-qty">${item.quantity}x</span>
                            <div style="flex-grow:1;display:flex;flex-direction:column;">
                                <span>${item.menuItemNameSnapshot}</span>
                                ${item.menuItemDescriptionSnapshot
                            ? `<span style="font-size:0.75rem;color:#6B7280;margin-top:2px;">${item.menuItemDescriptionSnapshot}</span>`
                            : ''}
                            </div>
                        </div>
                        ${item.note ? `<div class="item-note">📌 ${item.note}</div>` : ''}
                    </div>`;
                });
                html += `</div>`;

                // Pulsante azione della singola comanda
                html += `<div class="ticket-actions" style="margin-top:0.75rem;">`;
                if (order.status === 'SENT' || order.status === 'TO_PREPARE') {
                    html += `<button class="action-btn btn-prepare" onclick="window.app.changeOrderStatus(${order.id},'IN_PREPARATION')">Inizia Preparazione</button>`;
                } else if (order.status === 'IN_PREPARATION') {
                    html += `<button class="action-btn btn-done" onclick="window.app.changeOrderStatus(${order.id},'DONE')">Segna come Finito</button>`;
                } else if (order.status === 'DONE') {
                    html += `<button class="action-btn btn-archive" onclick="window.app.archiveOrder(${order.id})">Archivia ✕</button>`;
                } else if (order.status === 'ARCHIVED') {
                    html += `<button class="action-btn" style="background:#E5E7EB;color:#6B7280;pointer-events:none;">Archiviata</button>`;
                }
                html += `</div>`;

                html += `</div>`; // end comanda-sub
            });

            ticket.innerHTML = html;
            frag.appendChild(ticket);
        }

        // Singolo update del DOM
        requestAnimationFrame(() => {
            board.innerHTML = '';
            board.appendChild(frag);

            // Ripristina istantaneamente la posizione di scroll
            window.scrollTo(0, currentScrollY);
        });
    }

    async changeOrderStatus(orderId, newStatus) {
        try {
            await API.patch(`/api/cucina/orders/${orderId}/status`, { status: newStatus });
            // this.loadDashboard(); // Rimossa per evitare il "doppio scatto" visivo (si affida al WebSocket)
        } catch (e) {
            Swal.fire('Errore', 'Impossibile aggiornare stato', 'error');
        }
    }

    async archiveOrder(orderId) {
        try {
            await API.post(`/api/cucina/orders/${orderId}/archive`, {});
            // this.loadDashboard(); // Rimossa per evitare il "doppio scatto" visivo
        } catch (e) {
            Swal.fire('Errore', 'Impossibile archiviare', 'error');
        }
    }

    async archiveTable(tableId) {
        const res = await Swal.fire({
            title: 'Archiviare tutto il tavolo?',
            text: 'Tutte le comande verranno segnate come archiviate.',
            icon: 'warning',
            showCancelButton: true,
            confirmButtonText: 'Sì, Archivia Tutto',
            cancelButtonText: 'Annulla'
        });
        if (res.isConfirmed) {
            try {
                await API.post(`/api/cucina/tables/${tableId}/archive`, {});
                // this.loadDashboard(); // Rimossa per evitare il "doppio scatto" visivo
            } catch (e) {
                Swal.fire('Errore', 'Impossibile archiviare il tavolo', 'error');
            }
        }
    }
}

window.onload = () => {
    window.app = new CucinaApp();
};
