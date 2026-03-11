class Auth {
    static TOKEN_KEY = 'auth_token';

    static async login(username, password) {
        try {
            const res = await fetch('/api/auth/login', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ username, password })
            });

            if (!res.ok) {
                const text = await res.text();
                throw new Error(text || 'Login Failed');
            }

            const data = await res.json();
            if (data.token) {
                localStorage.setItem(this.TOKEN_KEY, data.token);
                return true;
            }
            return false;
        } catch (e) {
            console.error(e);
            return false;
        }
    }

    static logout() {
        localStorage.removeItem(this.TOKEN_KEY);
        // Refresh page or redirect to login. Left flexible for caller.
    }

    static getToken() {
        return localStorage.getItem(this.TOKEN_KEY);
    }

    static isAuthenticated() {
        // Technically should verify expiry, but backend intercepts and responds with 401 anyway
        return !!this.getToken();
    }
}

export default Auth;
