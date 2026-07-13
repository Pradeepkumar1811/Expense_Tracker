import { useState, useEffect } from 'react';
import reportService from '../../services/reportService';
import CategoryBreakdown from './CategoryBreakdown';

const styles = {
  container: {
    maxWidth: '900px',
    margin: '0 auto',
    padding: '2rem 1rem',
  },
  header: {
    margin: '0 0 1.5rem',
    fontSize: '1.75rem',
    fontWeight: 700,
    color: '#1f2937',
  },
  controls: {
    display: 'flex',
    gap: '0.75rem',
    marginBottom: '1.5rem',
    alignItems: 'center',
  },
  select: {
    padding: '0.5rem 0.75rem',
    fontSize: '0.875rem',
    border: '1px solid #d1d5db',
    borderRadius: '8px',
    outline: 'none',
    backgroundColor: '#ffffff',
  },
  summaryGrid: {
    display: 'grid',
    gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))',
    gap: '1rem',
    marginBottom: '2rem',
  },
  summaryCard: {
    background: '#ffffff',
    borderRadius: '12px',
    padding: '1.25rem',
    boxShadow: '0 2px 8px rgba(0, 0, 0, 0.08)',
    textAlign: 'center',
  },
  summaryLabel: {
    fontSize: '0.8rem',
    fontWeight: 500,
    color: '#6b7280',
    textTransform: 'uppercase',
    letterSpacing: '0.025em',
    margin: '0 0 0.375rem',
  },
  summaryValue: {
    fontSize: '1.5rem',
    fontWeight: 700,
    color: '#1f2937',
    margin: 0,
  },
  loadingContainer: {
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    minHeight: '200px',
    color: '#6b7280',
    fontSize: '1rem',
  },
  errorContainer: {
    display: 'flex',
    flexDirection: 'column',
    alignItems: 'center',
    justifyContent: 'center',
    minHeight: '200px',
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

function ReportView() {
  const [report, setReport] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [month, setMonth] = useState(new Date().getMonth() + 1);
  const [year, setYear] = useState(new Date().getFullYear());

  const fetchReport = async () => {
    setLoading(true);
    setError(null);
    try {
      const data = await reportService.getMonthlyReport(month, year);
      setReport(data);
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to load report.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchReport();
  }, [month, year]);

  if (loading) {
    return (
      <div style={styles.loadingContainer} role="status" aria-live="polite">
        <p>Loading report...</p>
      </div>
    );
  }

  if (error) {
    return (
      <div style={styles.errorContainer} role="alert">
        <p style={styles.errorText}>{error}</p>
        <button style={styles.retryButton} onClick={fetchReport}>Retry</button>
      </div>
    );
  }

  const netColor = report && Number(report.netSavings) >= 0 ? '#10b981' : '#dc2626';

  return (
    <div style={styles.container}>
      <h2 style={styles.header}>Monthly Report</h2>

      <div style={styles.controls}>
        <select
          style={styles.select}
          value={month}
          onChange={(e) => setMonth(Number(e.target.value))}
          aria-label="Select month"
        >
          {Array.from({ length: 12 }, (_, i) => (
            <option key={i + 1} value={i + 1}>
              {new Date(2000, i).toLocaleString(undefined, { month: 'long' })}
            </option>
          ))}
        </select>
        <select
          style={styles.select}
          value={year}
          onChange={(e) => setYear(Number(e.target.value))}
          aria-label="Select year"
        >
          {Array.from({ length: 6 }, (_, i) => {
            const y = new Date().getFullYear() - 2 + i;
            return <option key={y} value={y}>{y}</option>;
          })}
        </select>
      </div>

      {report && (
        <>
          <div style={styles.summaryGrid}>
            <div style={styles.summaryCard}>
              <p style={styles.summaryLabel}>Total Income</p>
              <p style={{ ...styles.summaryValue, color: '#10b981' }}>
                {formatCurrency(report.totalIncome)}
              </p>
            </div>
            <div style={styles.summaryCard}>
              <p style={styles.summaryLabel}>Total Expenses</p>
              <p style={{ ...styles.summaryValue, color: '#ef4444' }}>
                {formatCurrency(report.totalExpenses)}
              </p>
            </div>
            <div style={styles.summaryCard}>
              <p style={styles.summaryLabel}>Net Savings</p>
              <p style={{ ...styles.summaryValue, color: netColor }}>
                {formatCurrency(report.netSavings)}
              </p>
            </div>
          </div>

          <CategoryBreakdown breakdown={report.categoryBreakdown} />
        </>
      )}
    </div>
  );
}

export default ReportView;
