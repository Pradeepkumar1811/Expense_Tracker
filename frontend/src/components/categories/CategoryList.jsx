import { useState, useEffect } from 'react';
import categoryService from '../../services/categoryService';
import CategoryForm from './CategoryForm';

const styles = {
  container: {
    maxWidth: '800px',
    margin: '0 auto',
    padding: '2rem 1rem',
  },
  header: {
    margin: '0 0 1.5rem',
    fontSize: '1.75rem',
    fontWeight: 700,
    color: '#1f2937',
  },
  list: {
    listStyle: 'none',
    padding: 0,
    margin: 0,
    display: 'flex',
    flexDirection: 'column',
    gap: '0.5rem',
  },
  item: {
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'space-between',
    padding: '0.875rem 1rem',
    background: '#ffffff',
    borderRadius: '8px',
    boxShadow: '0 1px 4px rgba(0, 0, 0, 0.06)',
  },
  name: {
    fontSize: '0.95rem',
    fontWeight: 500,
    color: '#1f2937',
  },
  badge: {
    fontSize: '0.75rem',
    fontWeight: 500,
    color: '#6b7280',
    backgroundColor: '#f3f4f6',
    padding: '0.2rem 0.6rem',
    borderRadius: '12px',
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

function CategoryList() {
  const [categories, setCategories] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  const fetchCategories = async () => {
    setLoading(true);
    setError(null);
    try {
      const data = await categoryService.getAll();
      setCategories(data);
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to load categories.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchCategories();
  }, []);

  const handleCategoryCreated = async (name) => {
    const newCategory = await categoryService.create(name);
    setCategories((prev) => [...prev, newCategory]);
  };

  if (loading) {
    return (
      <div style={styles.loadingContainer} role="status" aria-live="polite">
        <p>Loading categories...</p>
      </div>
    );
  }

  if (error) {
    return (
      <div style={styles.errorContainer} role="alert">
        <p style={styles.errorText}>{error}</p>
        <button style={styles.retryButton} onClick={fetchCategories}>
          Retry
        </button>
      </div>
    );
  }

  return (
    <div style={styles.container}>
      <h2 style={styles.header}>Categories</h2>

      <CategoryForm onCategoryCreated={handleCategoryCreated} />

      {categories.length === 0 ? (
        <p style={styles.emptyText}>No categories found.</p>
      ) : (
        <ul style={styles.list} aria-label="Category list">
          {categories.map((cat) => (
            <li key={cat.id} style={styles.item}>
              <span style={styles.name}>{cat.name}</span>
              {cat.isDefault && <span style={styles.badge}>Default</span>}
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}

export default CategoryList;
