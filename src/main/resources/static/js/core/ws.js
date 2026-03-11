// Requires SockJS and STOMP.js loaded globally via CDN

class WSClient {
    constructor(endpoint, topicListenerMappings) {
        this.endpoint = endpoint;
        this.stompClient = null;
        this.topicListenerMappings = topicListenerMappings; // e.g., {'/topic/kitchen/orders': (msg) => {}}
        this.isConnected = false;
    }

    connect() {
        return new Promise((resolve, reject) => {
            const socket = new SockJS(this.endpoint);
            this.stompClient = Stomp.over(socket);

            // Disable debug logs in console if needed
            this.stompClient.debug = null;

            this.stompClient.connect({}, (frame) => {
                this.isConnected = true;
                console.log('Connected: ' + frame);

                // Subscribe to topics
                for (const [topic, listener] of Object.entries(this.topicListenerMappings)) {
                    this.stompClient.subscribe(topic, (message) => {
                        listener(message.body);
                    });
                }
                resolve();
            }, (error) => {
                console.error("STOMP Error:", error);
                this.isConnected = false;
                // Auto reconnect could go here
                setTimeout(() => this.connect(), 5000);
                reject(error);
            });
        });
    }

    disconnect() {
        if (this.stompClient !== null) {
            this.stompClient.disconnect();
            this.isConnected = false;
            console.log("Disconnected");
        }
    }
}

export default WSClient;
