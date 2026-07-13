import api from './api';

const categoryService = {
  async getAll() {
    const response = await api.get('/categories');
    return response.data;
  },

  async create(name) {
    const response = await api.post('/categories', { name });
    return response.data;
  },
};

export default categoryService;
