import { useState } from 'react';

const styles = {
  form: {
    background: '#ffffff',
    borderRadius: '12px',
    padding: '1.5rem',
    boxShadow: '0 2px 8px rgba(0, 0, 0, 0.08)',
    marginBottom: '1.5rem',
  },
  title: {
    margin: '0 0 1rem',
    fontSize: '1.1rem',
    fontWeight: 600,
    color: '#1f2937',
  },
  grid: {
    display: 'grid',
    gridTemplateColumns: 'repeat(auto-fit, minmax(180px, 1fr))',
    gap: '0.75rem',
    marginBottom: '1rem',
  },
  fieldGroup: {
    display: 'flex',
    flexDirection: 'column',
    gap: '0.25rem',
  },
  label: {
    fontSize: '0.8rem',
    fontWeight: 500,
    color: '#6b7280',
  },
  input: {
    padding: '0.5rem 0.75rem',
    fontSize: '0.875rem',
    border: '1px solid #d1d5db',
    borderRadius: '8px',
    outline: 'none',
  },
  select: {
    padding: '0.5rem 0.75rem',
    fontSize: '0.875rem',
    border: '1px solid #d1d5db',
    borderRadius: '8px',
    outline: 'none',
    backgroundColor: '#ffffff',
  },
  actions: {
    display: 'flex',
    gap: '0.75rem',
    justifyContent: 'flex-end',
  },
  submitButton: {
    padding: '0.5rem 1.25rem',
    fontSize: '0.875rem',
    fontWeight: 500,
    color: '#ffffff',
    backgroundColor: '#3b82f6',
    border: 'none',
    borderRadius: '8px',
    cursor: 'pointer',
  },
  cancelButton: {
    padding: '0.5rem 1.25rem',
    fontSize: '0.875rem',
    fontWeight: 500,
    color: '#6b7280',
    backgroundColor: '#f3f4f6',
    border: 'none',
    borderRadius: '8px',
    cursor: 'pointer',
  },
  error: {
    color: '#dc2626',
    fontSize: '0.8rem',
    margin: '0.5rem 0 0',
  },
};

function SubscriptionForm({ onSubmit, onCancel, initialData = null }) {
  const [name, setName] = useState(initialData?.name || '');
  const [amount, setAmount] = useState(initialData?.amount || '');
  const [billingCycle, setBillingCycle] = useState(initialData?.billingCycle || 'MONTHLY');
  const [nextRenewalDate, setNextRenewalDate] = useState(initialData?.nextRenewalDate || '');
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState(null);

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!name.trim() || !amount || !nextRenewalDate) return;

    setSubmitting(true);
    setError(null);
    try {
      const data = {
        name: name.trim(),
        amount: Number(amount),
        billingCycle,
        nextRenewalDate,
      };
      await onSubmit(data);
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to save subscription.');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <form style={styles.form} onSubmit={handleSubmit} aria-label={initialData ? 'Edit subscription' : 'Create subscription'}>
      <h3 style={styles.title}>{initialData ? 'Edit Subscription' : 'New Subscription'}</h3>

      <div style={styles.grid}>
        <div style={styles.fieldGroup}>
          <label style={styles.label} htmlFor="sub-name">Name</label>
          <input
            id="sub-name"
            style={styles.input}
            type="text"
            value={name}
            onChange={(e) => setName(e.target.value)}
            placeholder="e.g. Netflix"
            required
          />
        </div>

        <div style={styles.fieldGroup}>
          <label style={styles.label} htmlFor="sub-amount">Amount ($)</label>
          <input
            id="sub-amount"
            style={styles.input}
            type="number"
            min="0"
            step="0.01"
            value={amount}
            onChange={(e) => setAmount(e.target.value)}
            placeholder="0.00"
            required
          />
        </div>

        <div style={styles.fieldGroup}>
          <label style={styles.label} htmlFor="sub-cycle">Billing Cycle</label>
          <select
            id="sub-cycle"
            style={styles.select}
            value={billingCycle}
            onChange={(e) => setBillingCycle(e.target.value)}
          >
            <option value="MONTHLY">Monthly</option>
            <option value="ANNUAL">Annual</option>
          </select>
        </div>

        <div style={styles.fieldGroup}>
          <label style={styles.label} htmlFor="sub-renewal">Next Renewal Date</label>
          <input
            id="sub-renewal"
            style={styles.input}
            type="date"
            value={nextRenewalDate}
            onChange={(e) => setNextRenewalDate(e.target.value)}
            required
          />
        </div>
      </div>

      <div style={styles.actions}>
        {onCancel && (
          <button type="button" style={styles.cancelButton} onClick={onCancel}>
            Cancel
          </button>
        )}
        <button type="submit" style={styles.submitButton} disabled={submitting}>
          {submitting ? 'Saving...' : initialData ? 'Update' : 'Create'}
        </button>
      </div>

      {error && <p style={styles.error} role="alert">{error}</p>}
    </form>
  );
}

export default SubscriptionForm;
