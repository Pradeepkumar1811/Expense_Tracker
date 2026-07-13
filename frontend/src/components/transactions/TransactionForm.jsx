import { useState, useEffect } from 'react';
import transactionService from '../../services/transactionService';
import categoryService from '../../services/categoryService';

const styles = {
  overlay: {
    position: 'fixed',
    inset: 0,
    backgroundColor: 'rgba(0, 0, 0, 0.4)',
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    zIndex: 1000,
    padding: '1rem',
  },
  modal: {
    background: '#ffffff',
    borderRadius: '12px',
    boxShadow: '0 20px 60px rgba(0, 0, 0, 0.15)',
    width: '100%',
    maxWidth: '480px',
    maxHeight: '90vh',
    overflow: 'auto',
    padding: '2rem',
  },
  header: {
    display: 'flex',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: '1.5rem',
  },
  title: {
    margin: 0,
    fontSize: '1.25rem',
    fontWeight: 700,
    color: '#1f2937',
  },
  closeButton: {
    background: 'none',
    border: 'none',
    fontSize: '1.5rem',
    color: '#6b7280',
    cursor: 'pointer',
    padding: '0.25rem',
    lineHeight: 1,
  },
  form: {
    display: 'flex',
    flexDirection: 'column',
    gap: '1rem',
  },
  fieldGroup: {
    display: 'flex',
    flexDirection: 'column',
    gap: '0.35rem',
  },
  label: {
    fontSize: '0.875rem',
    fontWeight: 500,
    color: '#374151',
  },
  input: {
    padding: '0.6rem 0.75rem',
    fontSize: '0.875rem',
    border: '1px solid #d1d5db',
    borderRadius: '8px',
    outline: 'none',
    transition: 'border-color 0.2s',
    color: '#1f2937',
  },
  select: {
    padding: '0.6rem 0.75rem',
    fontSize: '0.875rem',
    border: '1px solid #d1d5db',
    borderRadius: '8px',
    outline: 'none',
    backgroundColor: '#ffffff',
    color: '#1f2937',
  },
  actions: {
    display: 'flex',
    justifyContent: 'flex-end',
    gap: '0.75rem',
    marginTop: '0.5rem',
  },
  cancelButton: {
    padding: '0.5rem 1.25rem',
    fontSize: '0.875rem',
    fontWeight: 500,
    color: '#374151',
    backgroundColor: '#f3f4f6',
    border: 'none',
    borderRadius: '8px',
    cursor: 'pointer',
  },
  saveButton: {
    padding: '0.5rem 1.25rem',
    fontSize: '0.875rem',
    fontWeight: 500,
    color: '#ffffff',
    backgroundColor: '#3b82f6',
    border: 'none',
    borderRadius: '8px',
    cursor: 'pointer',
  },
  saveButtonDisabled: {
    padding: '0.5rem 1.25rem',
    fontSize: '0.875rem',
    fontWeight: 500,
    color: '#ffffff',
    backgroundColor: '#93c5fd',
    border: 'none',
    borderRadius: '8px',
    cursor: 'not-allowed',
  },
  errorText: {
    color: '#dc2626',
    fontSize: '0.85rem',
    margin: 0,
  },
};

function TransactionForm({ transaction, onClose, onSave }) {
  const isEdit = Boolean(transaction);

  const [amount, setAmount] = useState(transaction?.amount?.toString() || '');
  const [type, setType] = useState(transaction?.type || 'EXPENSE');
  const [date, setDate] = useState(transaction?.date || new Date().toISOString().split('T')[0]);
  const [description, setDescription] = useState(transaction?.description || '');
  const [categoryId, setCategoryId] = useState('');
  const [categories, setCategories] = useState([]);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState(null);

  useEffect(() => {
    const loadCategories = async () => {
      try {
        const cats = await categoryService.getAll();
        setCategories(cats);
        // If editing, try to find matching category by name
        if (transaction?.categoryName && cats.length > 0) {
          const match = cats.find((c) => c.name === transaction.categoryName);
          if (match) setCategoryId(match.id.toString());
        }
      } catch {
        // Non-critical: user can still submit without a category
      }
    };
    loadCategories();
  }, [transaction]);

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError(null);

    if (!amount || !type || !date) {
      setError('Amount, type, and date are required.');
      return;
    }

    const payload = {
      amount: parseFloat(amount),
      type,
      date,
      description: description.trim() || null,
      categoryId: categoryId ? parseInt(categoryId, 10) : null,
    };

    setSaving(true);
    try {
      if (isEdit) {
        await transactionService.update(transaction.id, payload);
      } else {
        await transactionService.create(payload);
      }
      onSave();
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to save transaction.');
    } finally {
      setSaving(false);
    }
  };

  return (
    <div style={styles.overlay} onClick={onClose} role="dialog" aria-modal="true" aria-label={isEdit ? 'Edit Transaction' : 'Add Transaction'}>
      <div style={styles.modal} onClick={(e) => e.stopPropagation()}>
        <div style={styles.header}>
          <h3 style={styles.title}>{isEdit ? 'Edit Transaction' : 'Add Transaction'}</h3>
          <button style={styles.closeButton} onClick={onClose} aria-label="Close form">
            ×
          </button>
        </div>

        <form style={styles.form} onSubmit={handleSubmit}>
          <div style={styles.fieldGroup}>
            <label style={styles.label} htmlFor="txn-amount">Amount *</label>
            <input
              id="txn-amount"
              type="number"
              step="0.01"
              min="0.01"
              style={styles.input}
              value={amount}
              onChange={(e) => setAmount(e.target.value)}
              placeholder="0.00"
              required
            />
          </div>

          <div style={styles.fieldGroup}>
            <label style={styles.label} htmlFor="txn-type">Type *</label>
            <select
              id="txn-type"
              style={styles.select}
              value={type}
              onChange={(e) => setType(e.target.value)}
              required
            >
              <option value="INCOME">Income</option>
              <option value="EXPENSE">Expense</option>
            </select>
          </div>

          <div style={styles.fieldGroup}>
            <label style={styles.label} htmlFor="txn-date">Date *</label>
            <input
              id="txn-date"
              type="date"
              style={styles.input}
              value={date}
              onChange={(e) => setDate(e.target.value)}
              required
            />
          </div>

          <div style={styles.fieldGroup}>
            <label style={styles.label} htmlFor="txn-description">Description</label>
            <input
              id="txn-description"
              type="text"
              style={styles.input}
              value={description}
              onChange={(e) => setDescription(e.target.value)}
              placeholder="Optional description"
              maxLength={500}
            />
          </div>

          <div style={styles.fieldGroup}>
            <label style={styles.label} htmlFor="txn-category">Category</label>
            <select
              id="txn-category"
              style={styles.select}
              value={categoryId}
              onChange={(e) => setCategoryId(e.target.value)}
            >
              <option value="">— None —</option>
              {categories.map((cat) => (
                <option key={cat.id} value={cat.id}>
                  {cat.name}
                </option>
              ))}
            </select>
          </div>

          {error && (
            <p style={styles.errorText} role="alert">{error}</p>
          )}

          <div style={styles.actions}>
            <button type="button" style={styles.cancelButton} onClick={onClose}>
              Cancel
            </button>
            <button
              type="submit"
              style={saving ? styles.saveButtonDisabled : styles.saveButton}
              disabled={saving}
            >
              {saving ? 'Saving...' : isEdit ? 'Update' : 'Add'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}

export default TransactionForm;
