import { useState } from 'react';

const styles = {
  form: {
    display: 'flex',
    gap: '0.75rem',
    alignItems: 'center',
    marginBottom: '1.5rem',
  },
  input: {
    flex: 1,
    padding: '0.625rem 1rem',
    fontSize: '0.875rem',
    border: '1px solid #d1d5db',
    borderRadius: '8px',
    outline: 'none',
    transition: 'border-color 0.2s ease',
  },
  button: {
    padding: '0.625rem 1.25rem',
    fontSize: '0.875rem',
    fontWeight: 500,
    color: '#ffffff',
    backgroundColor: '#3b82f6',
    border: 'none',
    borderRadius: '8px',
    cursor: 'pointer',
    whiteSpace: 'nowrap',
  },
  buttonDisabled: {
    padding: '0.625rem 1.25rem',
    fontSize: '0.875rem',
    fontWeight: 500,
    color: '#ffffff',
    backgroundColor: '#93c5fd',
    border: 'none',
    borderRadius: '8px',
    cursor: 'not-allowed',
    whiteSpace: 'nowrap',
  },
  error: {
    color: '#dc2626',
    fontSize: '0.8rem',
    margin: '0.25rem 0 0',
  },
};

function CategoryForm({ onCategoryCreated }) {
  const [name, setName] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState(null);

  const handleSubmit = async (e) => {
    e.preventDefault();
    const trimmed = name.trim();
    if (!trimmed) return;

    setSubmitting(true);
    setError(null);
    try {
      await onCategoryCreated(trimmed);
      setName('');
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to create category.');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div>
      <form style={styles.form} onSubmit={handleSubmit} aria-label="Add category">
        <input
          style={styles.input}
          type="text"
          value={name}
          onChange={(e) => setName(e.target.value)}
          placeholder="New category name"
          aria-label="Category name"
          disabled={submitting}
        />
        <button
          type="submit"
          style={submitting || !name.trim() ? styles.buttonDisabled : styles.button}
          disabled={submitting || !name.trim()}
        >
          {submitting ? 'Adding...' : 'Add'}
        </button>
      </form>
      {error && <p style={styles.error} role="alert">{error}</p>}
    </div>
  );
}

export default CategoryForm;
