import api from './api';

const reportService = {
  async getMonthlyReport(month, year) {
    const response = await api.get('/reports', { params: { month, year } });
    return response.data;
  },

  async exportCsv(startDate, endDate) {
    const response = await api.get('/export/csv', {
      params: { startDate, endDate },
      responseType: 'blob',
    });
    return response.data;
  },
};

export default reportService;
