const styles = {
  container: {
    display: 'flex',
    flexDirection: 'column',
    gap: '0.375rem',
  },
  labelRow: {
    display: 'flex',
    justifyContent: 'space-between',
    fontSize: '0.8rem',
    color: '#6b7280',
  },
  progressBar: {
    width: '100%',
    height: '8px',
    backgroundColor: '#e5e7eb',
    borderRadius: '4px',
    overflow: 'hidden',
  },
  overspentText: {
    fontSize: '0.75rem',
    fontWeight: 600,
    color: '#dc2626',
    margin: 0,
  },
};

function BudgetStatus({ budget }) {
  const { limitAmount, totalSpending, remainingAmount, overspent, overspentAmount } = budget;
  const percent = limitAmount > 0 ? Math.min((totalSpending / limitAmount) * 100, 100) : 0;

  const fillColor = overspent ? '#dc2626' : percent > 75 ? '#f59e0b' : '#10b981';

  const fillStyle = {
    height: '100%',
    width: `${percent}%`,
    backgroundColor: fillColor,
    borderRadius: '4px',
    transition: 'width 0.3s ease',
  };

  return (
    <div style={styles.container} aria-label={`Budget status: ${totalSpending} of ${limitAmount} spent`}>
      <div style={styles.labelRow}>
        <span>Spent: ${Number(totalSpending).toFixed(2)}</span>
        <span>Limit: ${Number(limitAmount).toFixed(2)}</span>
      </div>
      <div style={styles.progressBar} role="progressbar" aria-valuenow={percent} aria-valuemin={0} aria-valuemax={100}>
        <div style={fillStyle} />
      </div>
      {overspent && (
        <p style={styles.overspentText}>
          Overspent by ${Number(overspentAmount).toFixed(2)}
        </p>
      )}
      {!overspent && (
        <div style={{ ...styles.labelRow, color: '#10b981' }}>
          <span>Remaining: ${Number(remainingAmount).toFixed(2)}</span>
        </div>
      )}
    </div>
  );
}

export default BudgetStatus;
