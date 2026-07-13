import api from './api';

const dashboardService = {
  async get() {
    const response = await api.get('/dashboard');
    return response.data;
  },
};

export default dashboardService;
