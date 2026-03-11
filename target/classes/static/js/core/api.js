import Auth from './auth.js';

class API {

    static async request(endpoint, options = {}) {
        const token = Auth.getToken();

        const headers = {
            'Content-Type': 'application/json',
            ...options.headers
        };

        if (token) {
            headers['Authorization'] = `Bearer ${token}`;
        }

        const config = {
            ...options,
            headers
        };

        try {
            const response = await fetch(endpoint, config);

            if (response.status === 401) {
                // Unauthorized - token expired or invalid
                console.warn('Session expired, logging out');
                Auth.logout();
                window.location.reload();
                return null;
            }

            if (!response.ok) {
                const errText = await response.text();
                throw new Error(errText || `API Error: ${response.status}`);
            }

            // Not all endpoints return JSON (e.g., DELETE 204)
            const contentType = response.headers.get('content-type');
            if (contentType && contentType.indexOf('application/json') !== -1) {
                return await response.json();
            } else {
                return await response.text();
            }

        } catch (error) {
            console.error(`API Request failed for ${endpoint}:`, error);
            throw error;
        }
    }

    static get(endpoint) {
        return this.request(endpoint, { method: 'GET' });
    }

    static post(endpoint, data) {
        return this.request(endpoint, { method: 'POST', body: JSON.stringify(data) });
    }

    static patch(endpoint, data) {
        return this.request(endpoint, { method: 'PATCH', body: JSON.stringify(data) });
    }

    static delete(endpoint) {
        return this.request(endpoint, { method: 'DELETE' });
    }
}

export default API;
