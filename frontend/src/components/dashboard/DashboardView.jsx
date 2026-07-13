import { useState, useEffect } from 'react';
import dashboardService from '../../services/dashboardService';
import StatCard from './StatCard';
import UpcomingRenewals from './UpcomingRenewals';

const styles = {
  container: {
    maxWidth: '1200px',
    margin: '0 auto',
    padding: '2rem 1rem',
  },
  header: {
    margin: '0 0 1.5rem',
    fontSize: '1.75rem',
    fontWeight: 700,
    color: '#1f2937',
  },
  grid: {
    display: 'grid',
    gridTemplateColumns: 'repeat(auto-fit, minmax(240px, 1fr))',
    gap: '1.25rem',
    marginBottom: '2rem',
  },
  loadingContainer: {
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    minHeight: '300px',
    color: '#6b7280',
    fontSize: '1rem',
  },
  errorContainer: {
    display: 'flex',
    flexDirection: 'column',
    alignItems: 'center',
    justifyContent: 'center',
    minHeight: '300px',
    gap: '1rem',
  },
  errorText: {
    color: '#dc2626',
    fontSize: '1rem',
    margin: 0,
  },
  retryButton: {
    padding: '0.5rem 1.25rem',
    fontSize: '0.875rem',
    fontWeight: 500,
    color: '#ffffff',
    backgroundColor: '#3b82f6',
    border: 'none',
    borderRadius: '8px',
    cursor: 'pointer',
  },
};

function formatCurrency(amount) {
  return new Intl.NumberFormat(undefined, {
    style: 'currency',
    currency: 'INR',
  }).format(amount ?? 0);
}

function formatPercent(value) {
  return `${Number(value ?? 0).toFixed(1)}%`;
}

function DashboardView() {
  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  const fetchDashboard = async () => {
    setLoading(true);
    setError(null);
    try {
      const result = await dashboardService.get();
      setData(result);
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to load dashboard data.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchDashboard();
  }, []);

  if (loading) {
    return (
      <div style={styles.loadingContainer} role="status" aria-live="polite">
        <p>Loading dashboard...</p>
      </div>
    );
  }

  if (error) {
    return (
      <div style={styles.errorContainer} role="alert">
        <p style={styles.errorText}>{error}</p>
        <button style={styles.retryButton} onClick={fetchDashboard}>
          Retry
        </button>
      </div>
    );
  }

  return (
    <div style={styles.container}>
      <h2 style={styles.header}>Dashboard</h2>

      <div style={styles.grid}>
        <StatCard
          title="Income"
          value={formatCurrency(data.currentMonthIncome)}
          icon="💰"
          color="#10b981"
        />
        <StatCard
          title="Expenses"
          value={formatCurrency(data.currentMonthExpenses)}
          icon="💸"
          color="#ef4444"
        />
        <StatCard
          title="Net Balance"
          value={formatCurrency(data.netBalance)}
          icon="📊"
          color="#3b82f6"
        />
        <StatCard
          title="Active Subscriptions"
          value={data.activeSubscriptionCount}
          icon="🔄"
          color="#8b5cf6"
        />
        <StatCard
          title="Subscription Cost"
          value={formatCurrency(data.totalSubscriptionCost)}
          icon="📅"
          color="#f59e0b"
        />
        <StatCard
          title="Budget Utilization"
          value={formatPercent(data.budgetUtilizationPercent)}
          icon="📈"
          color="#06b6d4"
        />
      </div>

      <UpcomingRenewals renewals={data.upcomingRenewals} />
    </div>
  );
}

export default DashboardView;
