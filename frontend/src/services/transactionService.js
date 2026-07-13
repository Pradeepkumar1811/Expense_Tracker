import api from './api';

const transactionService = {
  async getAll(page = 0, size = 20) {
    const response = await api.get('/transactions', { params: { page, size } });
    return response.data;
  },

  async create(transaction) {
    const response = await api.post('/transactions', transaction);
    return response.data;
  },

  async update(id, transaction) {
    const response = await api.put(`/transactions/${id}`, transaction);
    return response.data;
  },

  async remove(id) {
    await api.delete(`/transactions/${id}`);
  },
};

export default transactionService;
