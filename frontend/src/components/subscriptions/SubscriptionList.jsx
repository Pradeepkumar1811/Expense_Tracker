import { useState, useEffect } from 'react';
import subscriptionService from '../../services/subscriptionService';
import SubscriptionForm from './SubscriptionForm';

const styles = {
  container: {
    maxWidth: '900px',
    margin: '0 auto',
    padding: '2rem 1rem',
  },
  header: {
    display: 'flex',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: '1.5rem',
  },
  title: {
    margin: 0,
    fontSize: '1.75rem',
    fontWeight: 700,
    color: '#1f2937',
  },
  addButton: {
    padding: '0.5rem 1.25rem',
    fontSize: '0.875rem',
    fontWeight: 500,
    color: '#ffffff',
    backgroundColor: '#3b82f6',
    border: 'none',
    borderRadius: '8px',
    cursor: 'pointer',
  },
  list: {
    display: 'flex',
    flexDirection: 'column',
    gap: '0.75rem',
  },
  card: {
    background: '#ffffff',
    borderRadius: '12px',
    padding: '1.25rem',
    boxShadow: '0 2px 8px rgba(0, 0, 0, 0.08)',
    display: 'flex',
    justifyContent: 'space-between',
    alignItems: 'center',
  },
  info: {
    display: 'flex',
    flexDirection: 'column',
    gap: '0.25rem',
  },
  name: {
    fontSize: '1rem',
    fontWeight: 600,
    color: '#1f2937',
    margin: 0,
  },
  details: {
    fontSize: '0.8rem',
    color: '#6b7280',
    margin: 0,
  },
  amount: {
    fontSize: '1.1rem',
    fontWeight: 700,
    color: '#1f2937',
  },
  actions: {
    display: 'flex',
    gap: '0.5rem',
    alignItems: 'center',
  },
  editButton: {
    padding: '0.375rem 0.75rem',
    fontSize: '0.8rem',
    fontWeight: 500,
    color: '#3b82f6',
    backgroundColor: '#eff6ff',
    border: 'none',
    borderRadius: '6px',
    cursor: 'pointer',
  },
  deleteButton: {
    padding: '0.375rem 0.75rem',
    fontSize: '0.8rem',
    fontWeight: 500,
    color: '#dc2626',
    backgroundColor: '#fef2f2',
    border: 'none',
    borderRadius: '6px',
    cursor: 'pointer',
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
  emptyText: {
    color: '#6b7280',
    fontSize: '0.95rem',
    textAlign: 'center',
    padding: '2rem 0',
  },
};

function SubscriptionList() {
  const [subscriptions, setSubscriptions] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [showForm, setShowForm] = useState(false);
  const [editingId, setEditingId] = useState(null);

  const fetchSubscriptions = async () => {
    setLoading(true);
    setError(null);
    try {
      const data = await subscriptionService.getAll();
      setSubscriptions(data);
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to load subscriptions.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchSubscriptions();
  }, []);

  const handleCreate = async (data) => {
    await subscriptionService.create(data);
    setShowForm(false);
    fetchSubscriptions();
  };

  const handleUpdate = async (data) => {
    await subscriptionService.update(editingId, data);
    setEditingId(null);
    fetchSubscriptions();
  };

  const handleDelete = async (id) => {
    if (!window.confirm('Are you sure you want to delete this subscription?')) return;
    try {
      await subscriptionService.remove(id);
      setSubscriptions((prev) => prev.filter((s) => s.id !== id));
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to delete subscription.');
    }
  };

  if (loading) {
    return (
      <div style={styles.loadingContainer} role="status" aria-live="polite">
        <p>Loading subscriptions...</p>
      </div>
    );
  }

  if (error) {
    return (
      <div style={styles.errorContainer} role="alert">
        <p style={styles.errorText}>{error}</p>
        <button style={styles.retryButton} onClick={fetchSubscriptions}>Retry</button>
      </div>
    );
  }

  const editingSub = editingId ? subscriptions.find((s) => s.id === editingId) : null;

  return (
    <div style={styles.container}>
      <div style={styles.header}>
        <h2 style={styles.title}>Subscriptions</h2>
        <button style={styles.addButton} onClick={() => { setShowForm(!showForm); setEditingId(null); }}>
          {showForm ? 'Cancel' : '+ New Subscription'}
        </button>
      </div>

      {showForm && !editingId && (
        <SubscriptionForm onSubmit={handleCreate} onCancel={() => setShowForm(false)} />
      )}

      {editingSub && (
        <SubscriptionForm
          initialData={editingSub}
          onSubmit={handleUpdate}
          onCancel={() => setEditingId(null)}
        />
      )}

      {subscriptions.length === 0 ? (
        <p style={styles.emptyText}>No subscriptions yet. Add one to start tracking.</p>
      ) : (
        <div style={styles.list} aria-label="Subscription list">
          {subscriptions.map((sub) => (
            <div key={sub.id} style={styles.card}>
              <div style={styles.info}>
                <p style={styles.name}>{sub.name}</p>
                <p style={styles.details}>
                  {sub.billingCycle === 'MONTHLY' ? 'Monthly' : 'Annual'} · Renews {sub.nextRenewalDate}
                </p>
              </div>
              <div style={styles.actions}>
                <span style={styles.amount}>${Number(sub.amount).toFixed(2)}</span>
                <button
                  style={styles.editButton}
                  onClick={() => { setEditingId(sub.id); setShowForm(false); }}
                  aria-label={`Edit ${sub.name}`}
                >
                  Edit
                </button>
                <button
                  style={styles.deleteButton}
                  onClick={() => handleDelete(sub.id)}
                  aria-label={`Delete ${sub.name}`}
                >
                  Delete
                </button>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}

export default SubscriptionList;
