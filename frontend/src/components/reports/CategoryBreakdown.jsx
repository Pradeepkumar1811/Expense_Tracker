const styles = {
  container: {
    background: '#ffffff',
    borderRadius: '12px',
    padding: '1.5rem',
    boxShadow: '0 2px 8px rgba(0, 0, 0, 0.08)',
  },
  title: {
    margin: '0 0 1rem',
    fontSize: '1.1rem',
    fontWeight: 600,
    color: '#1f2937',
  },
  list: {
    display: 'flex',
    flexDirection: 'column',
    gap: '0.75rem',
  },
  row: {
    display: 'flex',
    justifyContent: 'space-between',
    alignItems: 'center',
  },
  categoryName: {
    fontSize: '0.9rem',
    fontWeight: 500,
    color: '#374151',
  },
  amount: {
    fontSize: '0.9rem',
    fontWeight: 600,
    color: '#1f2937',
  },
  barContainer: {
    width: '100%',
    height: '6px',
    backgroundColor: '#e5e7eb',
    borderRadius: '3px',
    overflow: 'hidden',
    marginTop: '0.25rem',
  },
  emptyText: {
    color: '#6b7280',
    fontSize: '0.875rem',
    textAlign: 'center',
    padding: '1rem 0',
  },
};

const colors = ['#3b82f6', '#10b981', '#f59e0b', '#ef4444', '#8b5cf6', '#06b6d4', '#ec4899'];

function CategoryBreakdown({ breakdown = [] }) {
  if (!breakdown || breakdown.length === 0) {
    return (
      <div style={styles.container}>
        <h3 style={styles.title}>Expense Breakdown by Category</h3>
        <p style={styles.emptyText}>No expense data for this period.</p>
      </div>
    );
  }

  const maxTotal = Math.max(...breakdown.map((b) => Number(b.total)));

  return (
    <div style={styles.container}>
      <h3 style={styles.title}>Expense Breakdown by Category</h3>
      <div style={styles.list} role="list" aria-label="Category expense breakdown">
        {breakdown.map((item, idx) => {
          const percent = maxTotal > 0 ? (Number(item.total) / maxTotal) * 100 : 0;
          const color = colors[idx % colors.length];

          return (
            <div key={item.categoryName} role="listitem">
              <div style={styles.row}>
                <span style={styles.categoryName}>{item.categoryName}</span>
                <span style={styles.amount}>${Number(item.total).toFixed(2)}</span>
              </div>
              <div style={styles.barContainer}>
                <div
                  style={{
                    height: '100%',
                    width: `${percent}%`,
                    backgroundColor: color,
                    borderRadius: '3px',
                    transition: 'width 0.3s ease',
                  }}
                />
              </div>
            </div>
          );
        })}
      </div>
    </div>
  );
}

export default CategoryBreakdown;
