import axios from 'axios';

// Vite proxies /api to backend in development
// In production, this might need to point to the actual backend URL
const api = axios.create({
    baseURL: '/api',
    headers: {
        'Content-Type': 'application/json',
    },
});

export default api;
