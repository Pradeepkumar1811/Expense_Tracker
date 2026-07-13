import api from './api';

const subscriptionService = {
  async getAll() {
    const response = await api.get('/subscriptions');
    return response.data;
  },

  async create(subscription) {
    const response = await api.post('/subscriptions', subscription);
    return response.data;
  },

  async update(id, subscription) {
    const response = await api.put(`/subscriptions/${id}`, subscription);
    return response.data;
  },

  async remove(id) {
    await api.delete(`/subscriptions/${id}`);
  },
};

export default subscriptionService;
