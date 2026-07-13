import { useState, useEffect } from 'react';
import categoryService from '../../services/categoryService';

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
    gridTemplateColumns: 'repeat(auto-fit, minmax(150px, 1fr))',
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

function BudgetForm({ onSubmit, onCancel, initialData = null }) {
  const [month, setMonth] = useState(initialData?.month || new Date().getMonth() + 1);
  const [year, setYear] = useState(initialData?.year || new Date().getFullYear());
  const [limitAmount, setLimitAmount] = useState(initialData?.limitAmount || '');
  const [categoryId, setCategoryId] = useState(initialData?.categoryId || '');
  const [categories, setCategories] = useState([]);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState(null);

  useEffect(() => {
    categoryService.getAll().then(setCategories).catch(() => {});
  }, []);

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!limitAmount || Number(limitAmount) <= 0) return;

    setSubmitting(true);
    setError(null);
    try {
      const data = {
        month: Number(month),
        year: Number(year),
        limitAmount: Number(limitAmount),
        categoryId: categoryId ? Number(categoryId) : null,
      };
      await onSubmit(data);
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to save budget.');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <form style={styles.form} onSubmit={handleSubmit} aria-label={initialData ? 'Edit budget' : 'Create budget'}>
      <h3 style={styles.title}>{initialData ? 'Edit Budget' : 'New Budget'}</h3>

      <div style={styles.grid}>
        <div style={styles.fieldGroup}>
          <label style={styles.label} htmlFor="budget-month">Month</label>
          <select
            id="budget-month"
            style={styles.select}
            value={month}
            onChange={(e) => setMonth(e.target.value)}
          >
            {Array.from({ length: 12 }, (_, i) => (
              <option key={i + 1} value={i + 1}>
                {new Date(2000, i).toLocaleString(undefined, { month: 'long' })}
              </option>
            ))}
          </select>
        </div>

        <div style={styles.fieldGroup}>
          <label style={styles.label} htmlFor="budget-year">Year</label>
          <input
            id="budget-year"
            style={styles.input}
            type="number"
            min="2020"
            max="2099"
            value={year}
            onChange={(e) => setYear(e.target.value)}
          />
        </div>

        <div style={styles.fieldGroup}>
          <label style={styles.label} htmlFor="budget-limit">Limit Amount ($)</label>
          <input
            id="budget-limit"
            style={styles.input}
            type="number"
            min="0"
            step="0.01"
            value={limitAmount}
            onChange={(e) => setLimitAmount(e.target.value)}
            placeholder="0.00"
            required
          />
        </div>

        <div style={styles.fieldGroup}>
          <label style={styles.label} htmlFor="budget-category">Category (optional)</label>
          <select
            id="budget-category"
            style={styles.select}
            value={categoryId}
            onChange={(e) => setCategoryId(e.target.value)}
          >
            <option value="">Overall (no category)</option>
            {categories.map((cat) => (
              <option key={cat.id} value={cat.id}>{cat.name}</option>
            ))}
          </select>
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

export default BudgetForm;
