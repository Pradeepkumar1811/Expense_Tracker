import api from './api';

const budgetService = {
  async getAll(month, year) {
    const response = await api.get('/budgets', { params: { month, year } });
    return response.data;
  },

  async create(budget) {
    const response = await api.post('/budgets', budget);
    return response.data;
  },

  async update(id, budget) {
    const response = await api.put(`/budgets/${id}`, budget);
    return response.data;
  },

  async delete(id) {
    const response = await api.delete(`/budgets/${id}`);
    return response.data;
  }
};

export default budgetService;
