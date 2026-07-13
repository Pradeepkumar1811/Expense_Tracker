import { useState, useEffect, useCallback } from 'react';
import transactionService from '../../services/transactionService';
import TransactionForm from './TransactionForm';
import ExportButton from './ExportButton';

const styles = {
  container: {
    maxWidth: '1200px',
    margin: '0 auto',
    padding: '2rem 1rem',
  },
  header: {
    display: 'flex',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: '1.5rem',
    flexWrap: 'wrap',
    gap: '1rem',
  },
  title: {
    margin: 0,
    fontSize: '1.75rem',
    fontWeight: 700,
    color: '#1f2937',
  },
  headerActions: {
    display: 'flex',
    gap: '0.75rem',
    alignItems: 'center',
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
  tableWrapper: {
    background: '#ffffff',
    borderRadius: '12px',
    boxShadow: '0 2px 8px rgba(0, 0, 0, 0.08)',
    overflow: 'hidden',
  },
  table: {
    width: '100%',
    borderCollapse: 'collapse',
  },
  th: {
    padding: '0.75rem 1rem',
    textAlign: 'left',
    fontSize: '0.75rem',
    fontWeight: 600,
    color: '#6b7280',
    textTransform: 'uppercase',
    letterSpacing: '0.05em',
    borderBottom: '1px solid #e5e7eb',
    backgroundColor: '#f9fafb',
  },
  td: {
    padding: '0.75rem 1rem',
    fontSize: '0.875rem',
    color: '#374151',
    borderBottom: '1px solid #f3f4f6',
  },
  incomeAmount: {
    color: '#10b981',
    fontWeight: 600,
  },
  expenseAmount: {
    color: '#ef4444',
    fontWeight: 600,
  },
  typeBadge: {
    display: 'inline-block',
    padding: '0.2rem 0.5rem',
    borderRadius: '4px',
    fontSize: '0.75rem',
    fontWeight: 500,
  },
  incomeBadge: {
    backgroundColor: '#d1fae5',
    color: '#065f46',
  },
  expenseBadge: {
    backgroundColor: '#fee2e2',
    color: '#991b1b',
  },
  actionButton: {
    padding: '0.3rem 0.6rem',
    fontSize: '0.75rem',
    fontWeight: 500,
    border: 'none',
    borderRadius: '6px',
    cursor: 'pointer',
    marginRight: '0.4rem',
  },
  editButton: {
    backgroundColor: '#eff6ff',
    color: '#2563eb',
  },
  deleteButton: {
    backgroundColor: '#fef2f2',
    color: '#dc2626',
  },
  pagination: {
    display: 'flex',
    justifyContent: 'center',
    alignItems: 'center',
    gap: '1rem',
    padding: '1rem',
    borderTop: '1px solid #e5e7eb',
  },
  pageButton: {
    padding: '0.4rem 1rem',
    fontSize: '0.875rem',
    fontWeight: 500,
    color: '#374151',
    backgroundColor: '#f3f4f6',
    border: 'none',
    borderRadius: '6px',
    cursor: 'pointer',
  },
  pageButtonDisabled: {
    padding: '0.4rem 1rem',
    fontSize: '0.875rem',
    fontWeight: 500,
    color: '#9ca3af',
    backgroundColor: '#f9fafb',
    border: 'none',
    borderRadius: '6px',
    cursor: 'not-allowed',
  },
  pageInfo: {
    fontSize: '0.875rem',
    color: '#6b7280',
  },
  loadingContainer: {
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    minHeight: '300px',
    color: '#6b7280',
    fontSize: '1rem',
  },
  errorContainer: {
    display: 'flex',
    flexDirection: 'column',
    alignItems: 'center',
    justifyContent: 'center',
    minHeight: '300px',
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
  emptyState: {
    textAlign: 'center',
    padding: '3rem 1rem',
    color: '#6b7280',
    fontSize: '0.95rem',
  },
};

function formatCurrency(amount) {
  return new Intl.NumberFormat(undefined, {
    style: 'currency',
    currency: 'USD',
  }).format(amount ?? 0);
}

function formatDate(dateStr) {
  if (!dateStr) return '';
  return new Date(dateStr).toLocaleDateString(undefined, {
    year: 'numeric',
    month: 'short',
    day: 'numeric',
  });
}

function TransactionList() {
  const [transactions, setTransactions] = useState([]);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [showForm, setShowForm] = useState(false);
  const [editingTransaction, setEditingTransaction] = useState(null);

  const pageSize = 20;

  const fetchTransactions = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const result = await transactionService.getAll(page, pageSize);
      setTransactions(result.content || []);
      setTotalPages(result.totalPages || 0);
      setTotalElements(result.totalElements || 0);
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to load transactions.');
    } finally {
      setLoading(false);
    }
  }, [page]);

  useEffect(() => {
    fetchTransactions();
  }, [fetchTransactions]);

  const handleAdd = () => {
    setEditingTransaction(null);
    setShowForm(true);
  };

  const handleEdit = (transaction) => {
    setEditingTransaction(transaction);
    setShowForm(true);
  };

  const handleDelete = async (id) => {
    if (!window.confirm('Are you sure you want to delete this transaction?')) return;
    try {
      await transactionService.remove(id);
      fetchTransactions();
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to delete transaction.');
    }
  };

  const handleFormClose = () => {
    setShowForm(false);
    setEditingTransaction(null);
  };

  const handleFormSave = () => {
    setShowForm(false);
    setEditingTransaction(null);
    fetchTransactions();
  };

  if (loading) {
    return (
      <div style={styles.loadingContainer} role="status" aria-live="polite">
        <p>Loading transactions...</p>
      </div>
    );
  }

  if (error && transactions.length === 0) {
    return (
      <div style={styles.errorContainer} role="alert">
        <p style={styles.errorText}>{error}</p>
        <button style={styles.retryButton} onClick={fetchTransactions}>
          Retry
        </button>
      </div>
    );
  }

  return (
    <div style={styles.container}>
      <div style={styles.header}>
        <h2 style={styles.title}>Transactions</h2>
        <div style={styles.headerActions}>
          <ExportButton />
          <button style={styles.addButton} onClick={handleAdd}>
            + Add Transaction
          </button>
        </div>
      </div>

      {error && (
        <div role="alert" style={{ marginBottom: '1rem' }}>
          <p style={styles.errorText}>{error}</p>
        </div>
      )}

      {showForm && (
        <TransactionForm
          transaction={editingTransaction}
          onClose={handleFormClose}
          onSave={handleFormSave}
        />
      )}

      {transactions.length === 0 ? (
        <div style={styles.tableWrapper}>
          <p style={styles.emptyState}>No transactions found. Add your first transaction to get started.</p>
        </div>
      ) : (
        <div style={styles.tableWrapper}>
          <table style={styles.table}>
            <thead>
              <tr>
                <th style={styles.th}>Date</th>
                <th style={styles.th}>Type</th>
                <th style={styles.th}>Amount</th>
                <th style={styles.th}>Category</th>
                <th style={styles.th}>Description</th>
                <th style={styles.th}>Actions</th>
              </tr>
            </thead>
            <tbody>
              {transactions.map((txn) => (
                <tr key={txn.id}>
                  <td style={styles.td}>{formatDate(txn.date)}</td>
                  <td style={styles.td}>
                    <span
                      style={{
                        ...styles.typeBadge,
                        ...(txn.type === 'INCOME' ? styles.incomeBadge : styles.expenseBadge),
                      }}
                    >
                      {txn.type}
                    </span>
                  </td>
                  <td style={{ ...styles.td, ...(txn.type === 'INCOME' ? styles.incomeAmount : styles.expenseAmount) }}>
                    {formatCurrency(txn.amount)}
                  </td>
                  <td style={styles.td}>{txn.categoryName || '—'}</td>
                  <td style={styles.td}>{txn.description || '—'}</td>
                  <td style={styles.td}>
                    <button
                      style={{ ...styles.actionButton, ...styles.editButton }}
                      onClick={() => handleEdit(txn)}
                      aria-label={`Edit transaction from ${formatDate(txn.date)}`}
                    >
                      Edit
                    </button>
                    <button
                      style={{ ...styles.actionButton, ...styles.deleteButton }}
                      onClick={() => handleDelete(txn.id)}
                      aria-label={`Delete transaction from ${formatDate(txn.date)}`}
                    >
                      Delete
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>

          <div style={styles.pagination}>
            <button
              style={page === 0 ? styles.pageButtonDisabled : styles.pageButton}
              onClick={() => setPage((p) => Math.max(0, p - 1))}
              disabled={page === 0}
              aria-label="Previous page"
            >
              ← Prev
            </button>
            <span style={styles.pageInfo}>
              Page {page + 1} of {totalPages} ({totalElements} total)
            </span>
            <button
              style={page >= totalPages - 1 ? styles.pageButtonDisabled : styles.pageButton}
              onClick={() => setPage((p) => p + 1)}
              disabled={page >= totalPages - 1}
              aria-label="Next page"
            >
              Next →
            </button>
          </div>
        </div>
      )}
    </div>
  );
}

export default TransactionList;
