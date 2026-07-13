import api from './api';

const authService = {
  async register(email, password) {
    const response = await api.post('/auth/register', { email, password });
    return response.data;
  },

  async login(email, password) {
    const response = await api.post('/auth/login', { email, password });
    const { accessToken, expiresIn } = response.data;
    localStorage.setItem('accessToken', accessToken);
    localStorage.setItem('expiresIn', String(expiresIn));
    return response.data;
  },

  logout() {
    localStorage.removeItem('accessToken');
    localStorage.removeItem('expiresIn');
  },

  getToken() {
    return localStorage.getItem('accessToken');
  },

  isAuthenticated() {
    return !!localStorage.getItem('accessToken');
  },
};

export default authService;
