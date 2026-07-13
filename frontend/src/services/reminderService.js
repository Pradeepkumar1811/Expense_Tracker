import api from './api';

const reminderService = {
  async getAll() {
    const response = await api.get('/reminders');
    return response.data;
  },

  async markAsRead(id) {
    const response = await api.patch(`/reminders/${id}/read`);
    return response.data;
  },
};

export default reminderService;
