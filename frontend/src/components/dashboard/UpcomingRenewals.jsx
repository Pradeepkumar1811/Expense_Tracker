const styles = {
  container: {
    background: '#ffffff',
    borderRadius: '12px',
    padding: '1.5rem',
    boxShadow: '0 2px 8px rgba(0, 0, 0, 0.08)',
  },
  header: {
    margin: '0 0 1rem',
    fontSize: '1.125rem',
    fontWeight: 600,
    color: '#1f2937',
  },
  list: {
    listStyle: 'none',
    margin: 0,
    padding: 0,
    display: 'flex',
    flexDirection: 'column',
    gap: '0.75rem',
  },
  item: {
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'space-between',
    padding: '0.75rem 1rem',
    borderRadius: '8px',
    backgroundColor: '#f9fafb',
    border: '1px solid #f3f4f6',
  },
  itemLeft: {
    display: 'flex',
    flexDirection: 'column',
    gap: '0.25rem',
  },
  name: {
    margin: 0,
    fontSize: '0.9375rem',
    fontWeight: 600,
    color: '#1f2937',
  },
  date: {
    margin: 0,
    fontSize: '0.8125rem',
    color: '#6b7280',
  },
  amount: {
    fontSize: '1rem',
    fontWeight: 700,
    color: '#ef4444',
    whiteSpace: 'nowrap',
  },
  badge: {
    display: 'inline-block',
    fontSize: '0.6875rem',
    fontWeight: 500,
    padding: '0.125rem 0.5rem',
    borderRadius: '9999px',
    backgroundColor: '#dbeafe',
    color: '#1d4ed8',
    marginLeft: '0.5rem',
    textTransform: 'capitalize',
  },
  emptyState: {
    textAlign: 'center',
    padding: '2rem 1rem',
    color: '#9ca3af',
    fontSize: '0.875rem',
  },
};

function formatDate(dateStr) {
  const date = new Date(dateStr);
  return date.toLocaleDateString(undefined, {
    month: 'short',
    day: 'numeric',
    year: 'numeric',
  });
}

function formatCurrency(amount) {
  return new Intl.NumberFormat(undefined, {
    style: 'currency',
    currency: 'USD',
  }).format(amount);
}

function UpcomingRenewals({ renewals = [] }) {
  if (!renewals.length) {
    return (
      <div style={styles.container}>
        <h3 style={styles.header}>Upcoming Renewals</h3>
        <div style={styles.emptyState}>
          <p>No upcoming renewals in the next 7 days.</p>
        </div>
      </div>
    );
  }

  return (
    <div style={styles.container}>
      <h3 style={styles.header}>Upcoming Renewals</h3>
      <ul style={styles.list} aria-label="Upcoming subscription renewals">
        {renewals.map((renewal) => (
          <li key={renewal.id} style={styles.item}>
            <div style={styles.itemLeft}>
              <p style={styles.name}>
                {renewal.name}
                <span style={styles.badge}>{renewal.billingCycle?.toLowerCase()}</span>
              </p>
              <p style={styles.date}>Renews {formatDate(renewal.nextRenewalDate)}</p>
            </div>
            <span style={styles.amount}>{formatCurrency(renewal.amount)}</span>
          </li>
        ))}
      </ul>
    </div>
  );
}

export default UpcomingRenewals;
