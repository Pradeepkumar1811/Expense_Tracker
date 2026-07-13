const styles = {
  item: {
    display: 'flex',
    justifyContent: 'space-between',
    alignItems: 'center',
    padding: '1rem 1.25rem',
    background: '#ffffff',
    borderRadius: '10px',
    boxShadow: '0 1px 4px rgba(0, 0, 0, 0.06)',
    borderLeft: '4px solid #f59e0b',
  },
  info: {
    display: 'flex',
    flexDirection: 'column',
    gap: '0.2rem',
  },
  subscriptionName: {
    fontSize: '0.95rem',
    fontWeight: 600,
    color: '#1f2937',
    margin: 0,
  },
  details: {
    fontSize: '0.8rem',
    color: '#6b7280',
    margin: 0,
  },
  right: {
    display: 'flex',
    alignItems: 'center',
    gap: '0.75rem',
  },
  amount: {
    fontSize: '1rem',
    fontWeight: 700,
    color: '#1f2937',
  },
  markReadButton: {
    padding: '0.375rem 0.75rem',
    fontSize: '0.75rem',
    fontWeight: 500,
    color: '#10b981',
    backgroundColor: '#ecfdf5',
    border: 'none',
    borderRadius: '6px',
    cursor: 'pointer',
  },
};

function ReminderItem({ reminder, onMarkAsRead }) {
  const { id, subscriptionName, renewalDate, amount } = reminder;

  return (
    <div style={styles.item} role="listitem" aria-label={`Reminder for ${subscriptionName}`}>
      <div style={styles.info}>
        <p style={styles.subscriptionName}>{subscriptionName}</p>
        <p style={styles.details}>Renews on {renewalDate}</p>
      </div>
      <div style={styles.right}>
        <span style={styles.amount}>${Number(amount).toFixed(2)}</span>
        <button
          style={styles.markReadButton}
          onClick={() => onMarkAsRead(id)}
          aria-label={`Mark ${subscriptionName} reminder as read`}
        >
          Mark Read
        </button>
      </div>
    </div>
  );
}

export default ReminderItem;
