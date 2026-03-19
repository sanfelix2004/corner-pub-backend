// WebSocket client con reconnect esponenziale automatico
// e callback onStatusChange per aggiornare l'indicatore UI.

class WSClient {
    constructor(endpoint, topicListenerMappings) {
        this.endpoint = endpoint;
        this.topicListenerMappings = topicListenerMappings;
        this.stompClient = null;
        this.isConnected = false;
        this._reconnectDelay = 1500;   // ms iniziale
        this._maxDelay = 30000;
        this._destroyed = false;
        this.onStatusChange = null;    // callback(connected: bool)
    }

    connect() {
        return new Promise((resolve, reject) => {
            this._doConnect(resolve, reject);
        });
    }

    _doConnect(resolve, reject) {
        if (this._destroyed) return;

        const socket = new SockJS(this.endpoint);
        this.stompClient = Stomp.over(socket);
        this.stompClient.debug = null; // zero spam in console

        this.stompClient.connect({}, (frame) => {
            this.isConnected = true;
            this._reconnectDelay = 1500;  // reset dopo connessione ok
            this._notifyStatus(true);

            for (const [topic, listener] of Object.entries(this.topicListenerMappings)) {
                this.stompClient.subscribe(topic, (msg) => listener(msg.body));
            }
            resolve(frame);
        }, (error) => {
            this.isConnected = false;
            this._notifyStatus(false);
            console.warn(`WS disconnesso — retry in ${this._reconnectDelay}ms`, error);

            // Prima connessione → reject (così il chiamante può loggare)
            // Poi reconnect automatico esponenziale
            if (reject) { reject(error); resolve = null; reject = null; }

            if (!this._destroyed) {
                setTimeout(() => {
                    this._reconnectDelay = Math.min(this._reconnectDelay * 2, this._maxDelay);
                    this._doConnect(null, null);
                }, this._reconnectDelay);
            }
        });
    }

    _notifyStatus(connected) {
        if (typeof this.onStatusChange === 'function') {
            this.onStatusChange(connected);
        }
    }

    disconnect() {
        this._destroyed = true;
        if (this.stompClient) {
            try { this.stompClient.disconnect(); } catch (_) { }
            this.isConnected = false;
        }
    }
}

export default WSClient;
